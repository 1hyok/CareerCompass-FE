package com.careercompass.feature.onboarding.presentation.login

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.careercompass.core.model.auth.SocialProvider
import com.careercompass.feature.onboarding.presentation.BuildConfig
import com.careercompass.feature.onboarding.presentation.login.util.GoogleLoginHelper
import com.careercompass.feature.onboarding.presentation.login.util.KakaoLoginHelper

/** 소셜 SDK 에서 토큰을 받아 오는 일. Activity 를 요구하는 유일한 조각이라 화면 쪽에만 둔다. */
internal typealias SocialTokenSource = suspend (Activity, SocialProvider) -> Result<String>

/**
 * 화면 수명에 묶인 소셜 로그인 발사대.
 *
 * 카카오·Credential Manager 는 Activity 를 요구한다. 그 Activity 를 [LocalActivity] 에서 읽어 「토큰을 가져오는
 * 일」로 감싼 뒤 [onAttempt] 에 넘긴다 — ViewModel 은 그 람다만 받으므로 플랫폼 타입이 ViewModel 로 들어가지
 * 않고, 시도가 끝나면 참조도 함께 사라진다.
 *
 * 컴포지션이 죽으면(설정 변경에 따른 액티비티 재생성·화면 이탈) [onHostDetached] 를 **반드시** 부른다. SDK 를
 * 띄운 호스트가 사라졌다는 사실을 진행 상태의 주인에게 알리는 유일한 통로다 — 이 신호가 없으면 진행 표시가 그대로
 * 남아 로그인 버튼이 영영 잠긴다(#147). `rememberCoroutineScope()` 로 SDK 를 띄우던 예전 배선이 정확히 그랬다.
 *
 * [LocalActivity] 가 없는 호스트(프리뷰 등)에서는 SDK 를 부르지 않고 설정 오류로 끝낸다.
 *
 * @param onAttempt 시도를 맡을 곳. 두 번째 인자가 「토큰을 가져오는 일」이다.
 * @param onHostDetached 호스트가 사라졌다 — 진행 중이던 토큰 요청을 끊고 잠금을 푼다.
 * @param tokenSource SDK 호출. 실제 SDK 를 띄울 수 없는 테스트가 갈아 끼울 수 있게 열어 둔다.
 */
@Composable
internal fun rememberSocialLoginLauncher(
    onAttempt: (SocialProvider, suspend () -> Result<String>) -> Unit,
    onHostDetached: () -> Unit,
    tokenSource: SocialTokenSource = ::requestProviderToken,
): (SocialProvider) -> Unit {
    val activity = LocalActivity.current
    val currentOnAttempt by rememberUpdatedState(onAttempt)
    val currentOnHostDetached by rememberUpdatedState(onHostDetached)
    val currentTokenSource by rememberUpdatedState(tokenSource)

    DisposableEffect(Unit) {
        onDispose { currentOnHostDetached() }
    }

    return remember(activity) {
        { provider: SocialProvider ->
            currentOnAttempt(provider) {
                if (activity == null) {
                    Result.failure(IllegalStateException("social login requires an Activity host"))
                } else {
                    currentTokenSource(activity, provider)
                }
            }
        }
    }
}

private suspend fun requestProviderToken(
    activity: Activity,
    provider: SocialProvider,
): Result<String> =
    when (provider) {
        SocialProvider.Kakao -> KakaoLoginHelper.requestKakaoAccessToken(activity)
        SocialProvider.Google -> GoogleLoginHelper.requestGoogleIdToken(activity, BuildConfig.GOOGLE_WEB_CLIENT_ID)
    }
