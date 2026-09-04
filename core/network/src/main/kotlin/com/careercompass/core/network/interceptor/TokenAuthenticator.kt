package com.careercompass.core.network.interceptor

import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.network.token.TokenReissuer
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

/**
 * 401 응답을 받았을 때 토큰을 회전하고 같은 요청을 새 토큰으로 한 번 더 보낸다.
 *
 * 회전은 선제 갱신 경로와 공유하는 [TokenReissuer] 락 경유 — 앞선 다른 경로가 이미 회전했으면 새 토큰만 받아
 * 재시도한다. 인증 거절의 세션 정리는 락 안에서 끝난다. 요청의 세션이 이미 끝났거나 교체됐으면 재시도하지
 * 않고 401 을 그대로 흘린다 — 다른 계정의 토큰으로 재전송하면 안 된다.
 */
public class TokenAuthenticator
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val tokenReissuer: TokenReissuer,
        private val errorReporter: ErrorReporter,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (response.responseCount >= MAX_ATTEMPTS) {
                errorReporter.recordAuthContractViolation(AUTH_STAGE_RETRY_LIMIT)
                runBlocking { authRepository.get().clearSession() }
                return null
            }

            val originalRequest = response.request
            val oldAccessToken = originalRequest.bearerToken()
            if (oldAccessToken == null) {
                errorReporter.recordAuthContractViolation(AUTH_STAGE_MISSING_AUTH_HEADER)
                return null
            }

            val outcome =
                tokenReissuer.reissue(
                    expectedAccessToken = oldAccessToken,
                    trigger = TokenReissuer.Trigger.Unauthorized,
                )
            return when (outcome) {
                is TokenReissuer.Outcome.TokenAlreadyChanged -> {
                    originalRequest.withBearer(outcome.accessToken)
                }

                is TokenReissuer.Outcome.SessionChanged -> {
                    null
                }

                is TokenReissuer.Outcome.Rotated -> {
                    if (outcome.accessToken == oldAccessToken) {
                        errorReporter.recordAuthContractViolation(AUTH_STAGE_SAME_TOKEN)
                        runBlocking { authRepository.get().clearSession() }
                        null
                    } else {
                        originalRequest.withBearer(outcome.accessToken)
                    }
                }

                is TokenReissuer.Outcome.AuthenticationRejected -> {
                    null
                }

                is TokenReissuer.Outcome.Failure -> {
                    throw TokenReissueFailureException(outcome.exception)
                }
            }
        }
    }

private fun ErrorReporter.recordAuthContractViolation(authStage: String) {
    recordFailure(
        throwable = IllegalStateException("Token authenticator contract violation"),
        attributes = mapOf(KEY_AUTH_STAGE to authStage),
    )
}

/** 재발급의 기술 원문을 UI 에 노출하지 않고 현재 요청만 실패시키는 예외. */
internal class TokenReissueFailureException(
    cause: Throwable,
) : IOException(null, cause)

/** 액세스 토큰만 갈아 끼운 재시도용 요청 사본. */
internal fun Request.withBearer(accessToken: String): Request =
    newBuilder().header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken").build()

internal fun Request.bearerToken(): String? =
    header(AUTHORIZATION_HEADER)?.let { value ->
        if (value.startsWith(BEARER_PREFIX, ignoreCase = true)) value.substring(BEARER_PREFIX.length) else value
    }

/** 이 응답까지의 시도 횟수 — OkHttp 는 재시도마다 직전 시도의 응답을 [Response.priorResponse] 로 매단다. */
private val Response.responseCount: Int
    get() = generateSequence(this) { it.priorResponse }.count()

private const val MAX_ATTEMPTS = 3
private const val AUTHORIZATION_HEADER = "Authorization"
private const val BEARER_PREFIX = "Bearer "
private const val KEY_AUTH_STAGE = "auth_stage"
private const val AUTH_STAGE_RETRY_LIMIT = "retry_limit"
private const val AUTH_STAGE_MISSING_AUTH_HEADER = "missing_auth_header"
private const val AUTH_STAGE_SAME_TOKEN = "same_token"
