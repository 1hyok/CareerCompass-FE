package com.careercompass.feature.onboarding.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.core.model.auth.SocialProvider
import com.careercompass.core.ui.mvi.ObserveSignal
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.R
import com.careercompass.feature.onboarding.presentation.biometric.BiometricEnrollGate
import com.careercompass.feature.onboarding.presentation.login.component.GoogleLoginButton
import com.careercompass.feature.onboarding.presentation.login.component.KakaoLoginButton
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingBrandMark
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingCenteredLayout
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingErrorCard

/**
 * 소셜 로그인 화면 — stateful 층.
 *
 * 카카오·Google SDK 는 Activity 를 요구하므로 토큰 획득은 [rememberSocialLoginLauncher] 가 만들고,
 * [LoginViewModel] 에는 「토큰을 가져오는 일」만 [LoginIntent.RequestSocialLogin] 으로 넘긴다 — 진행 중인 시도는
 * 컴포지션이 아니라 ViewModel 이 들고 있어야 화면이 재생성돼도 잠금이 풀린다(#147). Activity 가 없는 호스트(프리뷰
 * 등)에서는 SDK 를 부르지 않고 실패로 처리한다.
 *
 * 기존 사용자가 피드로 나가는 길목에는 [BiometricEnrollGate] 가 있다 — 지문 등록을 한 번 제안하고 끝나면 이동을
 * 이어 준다(#98). 신규 가입은 온보딩으로 가므로 여기서 묻지 않는다. 그쪽은 완료 화면이 맡는다.
 *
 * Kakao SDK 초기화(`KakaoSdk.init`)와 `GOOGLE_WEB_CLIENT_ID` 주입은 앱 셸 몫이다.
 *
 * 세션 만료 안내도 마찬가지다 — 이 화면은 [isSessionExpiryNoticeVisible] 라는 「보이라」는 입력만 받고 세션을
 * 판정하지 않는다(#128). 셸의 상태라 ViewModel 에 넣지 않고 [LoginContent] 까지 그대로 내려보낸다.
 *
 * @param isSessionExpiryNoticeVisible 셸이 켠 만료 안내. 로그인 실패 카드와 같은 자리를 쓴다.
 * @param onSessionExpiryNoticeDismissed 안내를 닫았거나 다시 로그인을 시도했다 — 셸이 안내를 끈다.
 */
@Composable
public fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNewUserOnboarding: () -> Unit,
    isSessionExpiryNoticeVisible: Boolean,
    onSessionExpiryNoticeDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchSocialLogin =
        rememberSocialLoginLauncher(
            onAttempt = { provider, requestToken ->
                viewModel.onIntent(LoginIntent.RequestSocialLogin(provider, requestToken))
            },
            onHostDetached = { viewModel.onIntent(LoginIntent.DetachLoginHost) },
        )

    // 피드행만 관문을 거친다 — 신규 가입은 온보딩으로 곧장 간다.
    ObserveSignal(
        signal = state.pendingNavigation.takeIf { it == LoginDestination.Onboarding },
        consumed = LoginIntent.ConsumeNavigation,
        onIntent = viewModel::onIntent,
        onSignal = { onNewUserOnboarding() },
    )

    LoginContent(
        state = state,
        isSessionExpiryNoticeVisible = isSessionExpiryNoticeVisible,
        onIntent = viewModel::onIntent,
        onSocialLoginClick = { provider ->
            // 다시 로그인하는 순간 만료 안내는 할 일을 마쳤다 — 시도 결과가 그 자리를 이어받는다.
            onSessionExpiryNoticeDismissed()
            launchSocialLogin(provider)
        },
        onSessionExpiryNoticeDismissed = onSessionExpiryNoticeDismissed,
        modifier = modifier,
    )

    BiometricEnrollGate(
        isRequested = state.pendingNavigation == LoginDestination.Feed,
        onProceed = {
            onLoginSuccess()
            viewModel.onIntent(LoginIntent.ConsumeNavigation)
        },
    )
}

/**
 * 소셜 로그인 화면 — stateless 층. 프리뷰·골든·Robolectric 과 앱의 접근성 스모크가 그리는 진입점이라 public 이다.
 *
 * 만료 안내와 로그인 실패는 같은 카드 자리를 나눠 쓴다. 방금 누른 버튼의 결과가 급하므로 실패가 먼저지만, 시도를
 * 시작할 때 안내를 이미 껐으므로 둘이 겹치는 프레임은 없다.
 *
 * @param onSocialLoginClick SDK 토큰 요청은 Activity 에 매여 있어 stateful 층이 만든다 — Intent 가 아니라 콜백으로 남는
 *   유일한 상호작용이다(`docs/convention/mvi.md`).
 */
@Composable
public fun LoginContent(
    state: LoginUiState,
    isSessionExpiryNoticeVisible: Boolean,
    onIntent: (LoginIntent) -> Unit,
    onSocialLoginClick: (SocialProvider) -> Unit,
    onSessionExpiryNoticeDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessionExpiryMessage = stringResource(R.string.onboarding_failure_session_expired)
    val errorMessage = state.failure?.toMessage() ?: sessionExpiryMessage.takeIf { isSessionExpiryNoticeVisible }

    OnboardingCenteredLayout(
        topContent = null,
        modifier = modifier,
        centerContent = { LoginBrandHeader() },
        bottomContent = {
            if (errorMessage != null) {
                OnboardingErrorCard(
                    message = errorMessage,
                    // 닫기는 지금 그 자리에 있는 카드의 주인에게 돌려준다.
                    onDismissClick = {
                        if (state.failure != null) onIntent(LoginIntent.ConsumeFailure) else onSessionExpiryNoticeDismissed()
                    },
                )
            }
            if (state.isBusy) {
                LoginProgress()
            }
            SocialLoginButtons(
                enabled = state.isActionEnabled,
                onKakaoClick = { onSocialLoginClick(SocialProvider.Kakao) },
                onGoogleClick = { onSocialLoginClick(SocialProvider.Google) },
            )
            Text(
                text = stringResource(R.string.onboarding_login_terms_notice),
                color = CareerCompassTheme.colors.mutedContent,
                textAlign = TextAlign.Center,
                style = CareerCompassTheme.typography.caption,
            )
        },
    )
}

@Composable
private fun LoginBrandHeader() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        OnboardingBrandMark(size = 56.dp, contentDescription = null)
        Text(
            text = stringResource(R.string.onboarding_app_name),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.headline1,
        )
        Text(
            text = stringResource(R.string.onboarding_login_tagline),
            color = colors.mutedContent,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoginProgress() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .size(20.dp)
                    .clearAndSetSemantics {},
            color = colors.primaryEmphasis,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.onboarding_login_loading),
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SocialLoginButtons(
    enabled: Boolean,
    onKakaoClick: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        KakaoLoginButton(onClick = onKakaoClick, enabled = enabled)
        GoogleLoginButton(onClick = onGoogleClick, enabled = enabled)
    }
}
