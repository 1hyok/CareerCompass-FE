package com.careercompass.feature.onboarding.presentation.login.util

import android.app.Activity
import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.domain.error.CoreAuthFailure
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 카카오 SDK 로그인 — 카카오톡 앱 로그인을 먼저 시도하고, 앱이 없거나 실패하면 카카오계정(웹) 로그인으로 폴백한다.
 *
 * 카카오톡 로그인에서 사용자가 직접 취소한 경우는 폴백하지 않는다 — 취소 직후 계정 로그인 창이 또 뜨면 취소가
 * 무시된 것으로 보인다. 취소는 [CoreAuthFailure.UserCancelledAuth] 로 번역해 호출처가 조용히 흘려보낸다.
 *
 * SDK 가 초기화되지 않았으면(`KAKAO_NATIVE_APP_KEY` 미기재 — `KakaoInitializer` 가 초기화를 건너뛴다) SDK 진입점이
 * 동기 예외를 던진다. 그 예외를 던지지 않고 [Result] 실패로 돌려주어 로그인 화면이 안내하게 한다 — 예외로 두면
 * 버튼 하나에 앱이 죽는다.
 */
internal object KakaoLoginHelper {
    suspend fun requestKakaoAccessToken(activity: Activity): Result<String> =
        runCatchingCancellable { requestOrThrow(activity) }.getOrElse { Result.failure(it) }

    private suspend fun requestOrThrow(activity: Activity): Result<String> {
        val client = UserApiClient.instance
        if (client.isKakaoTalkLoginAvailable(activity)) {
            val viaTalk =
                suspendCancellableCoroutine<Result<String>> { continuation ->
                    client.loginWithKakaoTalk(activity) { token, error ->
                        if (continuation.isActive) continuation.resume(toResult(token, error))
                    }
                }
            val error = viaTalk.exceptionOrNull() ?: return viaTalk
            if (error is CoreAuthFailure.UserCancelledAuth) return viaTalk
        }
        return suspendCancellableCoroutine { continuation ->
            client.loginWithKakaoAccount(activity) { token, error ->
                if (continuation.isActive) continuation.resume(toResult(token, error))
            }
        }
    }

    private fun toResult(
        token: OAuthToken?,
        error: Throwable?,
    ): Result<String> =
        when {
            error != null -> Result.failure(if (error.isUserCancelled()) CoreAuthFailure.UserCancelledAuth() else error)
            token != null -> Result.success(token.accessToken)
            else -> Result.failure(IllegalStateException("Kakao SDK returned neither a token nor an error"))
        }

    private fun Throwable.isUserCancelled(): Boolean = this is ClientError && reason == ClientErrorCause.Cancelled
}
