package com.cambridge.feature.onboarding.presentation.login

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.feature.onboarding.presentation.BuildConfig
import com.cambridge.feature.onboarding.presentation.R
import com.cambridge.feature.onboarding.presentation.biometric.BiometricEnrollGate
import com.cambridge.feature.onboarding.presentation.login.util.GoogleLoginHelper
import com.cambridge.feature.onboarding.presentation.login.util.KakaoLoginHelper
import kotlinx.coroutines.launch

/**
 * 소셜 로그인 화면의 상태 배선.
 *
 * 카카오·Google SDK 는 Activity 를 요구하므로 토큰 획득은 여기서 하고, [LoginViewModel] 에는 토큰만 넘긴다.
 * [LocalActivity] 가 없는 호스트(프리뷰 등)에서는 SDK 를 부르지 않고 실패로 처리한다.
 *
 * 기존 사용자가 피드로 나가는 길목에는 [BiometricEnrollGate] 가 있다 — 지문 등록을 한 번 제안하고 끝나면 이동을
 * 이어 준다(#98). 신규 가입은 온보딩으로 가므로 여기서 묻지 않는다. 그쪽은 완료 화면이 맡는다.
 *
 * Kakao SDK 초기화(`KakaoSdk.init`)와 `GOOGLE_WEB_CLIENT_ID` 주입은 앱 셸 몫이다.
 *
 * 세션 만료 안내도 마찬가지다 — 이 화면은 [isSessionExpiryNoticeVisible] 라는 「보이라」는 입력만 받고 세션을
 * 판정하지 않는다(#128). 문구는 온보딩 저장이 만료로 실패했을 때와 같은 것을 쓴다: 사용자가 읽는 사실이 같은데
 * 두 벌로 두면 한쪽만 고쳐진다.
 *
 * @param isSessionExpiryNoticeVisible 셸이 켠 만료 안내. 로그인 실패 카드와 같은 자리를 쓴다.
 * @param onSessionExpiryNoticeDismissed 안내를 닫았거나 다시 로그인을 시도했다 — 셸이 안내를 끈다.
 */
@Composable
public fun LoginEntry(
    onLoginSuccess: () -> Unit,
    onNewUserOnboarding: () -> Unit,
    isSessionExpiryNoticeVisible: Boolean,
    onSessionExpiryNoticeDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val currentOnNewUserOnboarding by rememberUpdatedState(onNewUserOnboarding)

    // 피드행만 관문을 거친다 — 신규 가입은 온보딩으로 곧장 간다.
    LaunchedEffect(state.pendingNavigation) {
        if (state.pendingNavigation != LoginDestination.Onboarding) return@LaunchedEffect
        currentOnNewUserOnboarding()
        viewModel.onNavigationConsumed()
    }

    fun requestSocialToken(provider: SocialProvider) {
        // 다시 로그인하는 순간 만료 안내는 할 일을 마쳤다 — 시도 결과가 그 자리를 이어받는다.
        onSessionExpiryNoticeDismissed()
        if (activity == null) {
            viewModel.onSocialTokenRequestFailed(provider, IllegalStateException("social login requires an Activity host"))
            return
        }
        viewModel.onSocialTokenRequestStarted()
        scope.launch {
            requestProviderToken(provider, activity)
                .onSuccess { token ->
                    when (provider) {
                        SocialProvider.Kakao -> viewModel.loginWithKakao(token)
                        SocialProvider.Google -> viewModel.loginWithGoogle(token)
                    }
                }.onFailure { throwable -> viewModel.onSocialTokenRequestFailed(provider, throwable) }
        }
    }

    // 만료 안내와 로그인 실패는 같은 카드 자리를 나눠 쓴다. 방금 누른 버튼의 결과가 급하므로 실패가 먼저지만,
    // 시도를 시작할 때 안내를 이미 껐으므로 둘이 겹치는 프레임은 없다.
    val sessionExpiryMessage = stringResource(R.string.onboarding_failure_session_expired)
    val errorMessage = state.failure?.let { it.toMessage() } ?: sessionExpiryMessage.takeIf { isSessionExpiryNoticeVisible }

    /** 닫기는 지금 그 자리에 있는 카드의 주인에게 돌려준다. */
    fun dismissErrorCard() {
        if (state.failure != null) viewModel.onFailureConsumed() else onSessionExpiryNoticeDismissed()
    }

    LoginScreen(
        // 이동이 대기 중인 동안에도 로딩을 유지한다 — 관문이 프로필을 받아 오는 사이 버튼이 살아 있으면 이미
        // 로그인한 사용자가 SDK 를 한 번 더 열 수 있다.
        state = LoginUiState(isLoading = state.isLoading || state.pendingNavigation != null, errorMessage = errorMessage),
        onEvent = { event ->
            when (event) {
                LoginEvent.KakaoLoginClicked -> requestSocialToken(SocialProvider.Kakao)
                LoginEvent.GoogleLoginClicked -> requestSocialToken(SocialProvider.Google)
                LoginEvent.ErrorDismissed -> dismissErrorCard()
            }
        },
        modifier = modifier,
    )

    BiometricEnrollGate(
        isRequested = state.pendingNavigation == LoginDestination.Feed,
        onProceed = {
            currentOnLoginSuccess()
            viewModel.onNavigationConsumed()
        },
    )
}

private suspend fun requestProviderToken(
    provider: SocialProvider,
    activity: Activity,
): Result<String> =
    when (provider) {
        SocialProvider.Kakao -> KakaoLoginHelper.requestKakaoAccessToken(activity)
        SocialProvider.Google -> GoogleLoginHelper.requestGoogleIdToken(activity, BuildConfig.GOOGLE_WEB_CLIENT_ID)
    }
