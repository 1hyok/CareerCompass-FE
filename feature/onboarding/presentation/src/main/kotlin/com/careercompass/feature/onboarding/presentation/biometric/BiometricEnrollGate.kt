package com.careercompass.feature.onboarding.presentation.biometric

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.feature.onboarding.presentation.R
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingSheetHost

/**
 * 피드로 나가기 직전 지문 등록을 한 번 제안하는 관문 — 로그인 성공(기존 사용자)과 온보딩 완료(신규 사용자)가 쓴다.
 *
 * 관문은 이동을 **지연**시킬 뿐 막지 않는다. 제안 조건이 아니면 시트 없이 곧바로 [onProceed] 가 오고, 시트를 띄운
 * 경우에도 등록·거절·실패 어느 쪽으로 끝나든 [onProceed] 로 끝난다. 조건 판정은 [BiometricEnrollViewModel] 이 하고,
 * 여기서는 플랫폼만 본다 — `BiometricPrompt` 가 요구하는 [FragmentActivity] 호스트인지와 강한 생체를 지금 쓸 수
 * 있는지다. 프리뷰·테스트 호스트처럼 Activity 가 없으면 제안하지 않고 통과한다.
 *
 * @param isRequested 이동이 대기 중인가. false 로 돌아가면 관문은 아무것도 하지 않는다.
 * @param onProceed 제안이 끝났다(띄우지 않은 경우 포함) — 호출부가 원래 이동을 이어서 한다.
 */
@Composable
internal fun BiometricEnrollGate(
    isRequested: Boolean,
    onProceed: () -> Unit,
    viewModel: BiometricEnrollViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    val currentOnProceed by rememberUpdatedState(onProceed)

    LaunchedEffect(isRequested, activity) {
        if (isRequested) viewModel.onIntent(BiometricEnrollIntent.RequestOffer(deviceCanEnroll = activity.canEnrollBiometric()))
    }

    // [isRequested] 를 다시 보는 이유 — 판정 도중 호출부가 다른 곳으로 가기로 했을 수 있다(완료 화면의 「게시판 먼저
    // 등록하기」). 그때 늦게 도착한 통과 신호로 피드까지 밀어 넣지 않는다.
    LaunchedEffect(isRequested, state.canProceed) {
        if (!isRequested || !state.canProceed) return@LaunchedEffect
        currentOnProceed()
        viewModel.onIntent(BiometricEnrollIntent.ConsumeProceed)
    }

    val launchPrompt =
        rememberBiometricPromptLauncher(
            title = stringResource(R.string.onboarding_biometric_enroll_prompt_title),
            negativeButtonText = stringResource(R.string.onboarding_biometric_prompt_negative),
            allowedAuthenticators = BIOMETRIC_ENROLL_AUTHENTICATORS,
            listener = remember(viewModel) { BiometricEnrollPromptListener(viewModel::onIntent) },
        )

    if (!state.isOffered) return

    OnboardingSheetHost(onDismissRequest = { viewModel.onIntent(BiometricEnrollIntent.Decline) }) {
        BiometricEnrollSheet(
            state = state,
            onIntent = viewModel::onIntent,
            onEnrollClick = launchPrompt,
        )
    }
}

private class BiometricEnrollPromptListener(
    private val onIntent: (BiometricEnrollIntent) -> Unit,
) : BiometricPromptListener {
    override fun onStarted(): Unit = onIntent(BiometricEnrollIntent.AuthenticationStarted)

    override fun onSucceeded(): Unit = onIntent(BiometricEnrollIntent.AuthenticationSucceeded)

    override fun onCancelled(): Unit = onIntent(BiometricEnrollIntent.AuthenticationCancelled)

    override fun onFailed(
        reason: BiometricFailureReason,
        cause: Throwable,
    ): Unit = onIntent(BiometricEnrollIntent.AuthenticationFailed(cause))
}
