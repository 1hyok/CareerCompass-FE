package com.cambridge.core.network.token

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.network.model.ApiException
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선제 갱신(`AuthInterceptor`)과 401 대응(`TokenAuthenticator`)이 공유하는 토큰 재발급 single-flight.
 *
 * 락 안에서 호출자가 본 토큰과 현재 저장 토큰을 다시 비교해 다른 경로가 이미 갱신했다면 중복 재발급을
 * 건너뛴다. 확정 거절([Outcome.AuthenticationRejected])은 락 안에서 세션 정리까지 끝내고 그 결과를 액세스
 * 토큰에 묶어 공유한다 — 같은 토큰으로 들어온 대기자는 재발급을 치지 않고 같은 결과를 받는다. 일시 실패는
 * 공유하지 않는다(재시도가 성립하는 실패라 캐시하면 복구를 막는다).
 */
@Singleton
public class TokenReissuer
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val expiryTracker: AccessTokenExpiryTracker,
        private val errorReporter: ErrorReporter,
    ) {
        public sealed interface Outcome {
            /** 다른 경로가 먼저 토큰을 갱신해 재발급을 생략함. */
            public data class TokenAlreadyChanged(
                val accessToken: String,
            ) : Outcome

            /** 현재 호출이 새 토큰 발급을 완료함. */
            public data class Rotated(
                val accessToken: String,
            ) : Outcome

            public sealed interface Failure : Outcome {
                public val exception: Throwable
            }

            /** refresh 가 거절됨 — 세션은 이미 정리됐다. */
            public data class AuthenticationRejected(
                override val exception: Throwable,
            ) : Failure

            public data class TransportFailure(
                override val exception: IOException,
            ) : Failure

            public data class ServerFailure(
                override val exception: Throwable,
            ) : Failure

            public data class UnexpectedFailure(
                override val exception: Throwable,
            ) : Failure
        }

        private var rejectedAccessToken: String? = null
        private var rejection: Outcome.AuthenticationRejected? = null

        /** @param expectedAccessToken 호출자가 교체하려는 기존 액세스 토큰. */
        public fun reissue(expectedAccessToken: String): Outcome {
            synchronized(this) {
                // 저장 토큰 비교보다 먼저다 — 세션 정리 뒤엔 저장 토큰이 비어 있어 아래 가드를 그냥 통과한다.
                rejection?.let { if (expectedAccessToken == rejectedAccessToken) return it }

                val currentToken = runBlocking { authRepository.get().getAccessToken() }.getOrNull()
                if (!currentToken.isNullOrEmpty() && currentToken != expectedAccessToken) {
                    return Outcome.TokenAlreadyChanged(currentToken)
                }

                val rotationResult = runBlocking { authRepository.get().rotateToken() }
                val rotationException = rotationResult.exceptionOrNull()
                if (rotationException != null) {
                    // 실패한 토큰의 deadline 을 지워 선제 재시도 반복을 막는다.
                    expiryTracker.clear()
                    val failure = classifyFailure(rotationException)
                    rememberIfRejected(expectedAccessToken, failure)
                    reportObservableFailure(failure)
                    return failure
                }

                val bundle = rotationResult.getOrThrow()
                bundle.expiresInSeconds?.let(expiryTracker::record) ?: expiryTracker.clear()
                rejectedAccessToken = null
                rejection = null
                return Outcome.Rotated(bundle.accessToken)
            }
        }

        private fun classifyFailure(exception: Throwable): Outcome.Failure =
            when (exception) {
                is ApiException -> classifyApiFailure(exception)
                is HttpException -> classifyHttpFailure(exception, exception.code())
                is IOException -> Outcome.TransportFailure(exception)
                else -> Outcome.UnexpectedFailure(exception)
            }

        private fun classifyApiFailure(exception: ApiException): Outcome.Failure =
            when {
                exception.code in REJECTION_CODES -> Outcome.AuthenticationRejected(exception)
                exception.status != null -> classifyHttpFailure(exception, exception.status)
                else -> Outcome.UnexpectedFailure(exception)
            }

        /**
         * 재발급 엔드포인트 전용 분류다 — 일반 API 응답에 쓰면 안 된다.
         *
         * 이 엔드포인트에 한해 400·401·403 은 "이 refresh 로는 더 진행할 수 없다" 이므로 본문과 무관하게 거절로
         * 확정한다. 무효 refresh 를 세션에 남기면 이후 요청이 401 → 재발급 실패를 반복해 사용자가 로그인
         * 화면으로도 못 가고 데이터도 못 받는다.
         */
        private fun classifyHttpFailure(
            exception: Throwable,
            status: Int,
        ): Outcome.Failure =
            when (status) {
                400, 401, 403 -> Outcome.AuthenticationRejected(exception)
                in 500..599 -> Outcome.ServerFailure(exception)
                else -> Outcome.UnexpectedFailure(exception)
            }

        private fun rememberIfRejected(
            expectedAccessToken: String,
            failure: Outcome.Failure,
        ) {
            if (failure !is Outcome.AuthenticationRejected) return
            runBlocking { authRepository.get().clearSession() }
            rejectedAccessToken = expectedAccessToken
            rejection = failure
        }

        private fun reportObservableFailure(failure: Outcome.Failure) {
            val failureKind =
                when (failure) {
                    is Outcome.AuthenticationRejected -> return
                    is Outcome.TransportFailure -> "transport"
                    is Outcome.ServerFailure -> "server"
                    is Outcome.UnexpectedFailure -> "unexpected"
                }
            errorReporter.recordFailure(
                throwable = failure.exception,
                attributes = mapOf("auth_stage" to "token_reissue", "failure_kind" to failureKind),
            )
        }

        private companion object {
            /** API_SPEC v0.1 §9 — 토큰 없음/만료·무효는 재로그인만이 해법이다. */
            val REJECTION_CODES = setOf("AUTH_REQUIRED", "AUTH_INVALID")
        }
    }
