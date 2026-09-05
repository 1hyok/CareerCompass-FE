package com.careercompass.feature.onboarding.presentation.biometric

import androidx.lifecycle.viewModelScope
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.ui.mvi.MviViewModel
import com.careercompass.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.careercompass.feature.onboarding.presentation.reporting.recordOnboardingFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 지문 빠른 로그인을 **켜는** 경로 — 기능 스펙 F1-1(#98). 진입점은 [onIntent] 하나, 전이는 [reduce] 한 곳이다(#245).
 *
 * 로그인 성공·온보딩 완료로 피드에 들어가기 직전 한 번만 묻는다. 그 자리인 이유는 [AuthRepository.registerBiometric]
 * 이 현재 세션 사용자 id 를 알아야 하기 때문이다 — 등록을 계정에 귀속하는 규칙(#81)이라, 프로필을 받기 전에는
 * 서버를 부르지도 못하고 실패한다. 그래서 [ensureProfile] 로 프로필을 먼저 확보하고 판단한다.
 *
 * 제안하지 않고 그냥 통과하는 경우는 넷이다.
 * - 기기가 강한 생체 인증을 쓸 수 없다(하드웨어 없음·미등록·일시 잠금 — 판정은 플랫폼을 아는 [BiometricEnrollGate] 몫).
 * - 프로필을 끝내 받지 못했다(오프라인 로그인) — 다음 로그인에 다시 기회가 온다.
 * - 이 계정이 이 기기에 이미 등록해 뒀다.
 * - 이 계정이 전에 「나중에」로 넘겼다.
 *
 * 어느 실패도 로그인·온보딩 흐름을 막지 않는다. 등록이 성공했을 때만 켜진 것으로 보고, 나머지는 안내만 남긴 채
 * [BiometricEnrollUiState.canProceed] 로 원래 이동을 이어 준다.
 */
@HiltViewModel
public class BiometricEnrollViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<BiometricEnrollIntent, BiometricEnrollUiState, BiometricEnrollReducerEvent>(BiometricEnrollUiState()) {
        private var offerJob: Job? = null
        private var registerJob: Job? = null
        private var declineJob: Job? = null

        override fun onIntent(intent: BiometricEnrollIntent) {
            when (intent) {
                is BiometricEnrollIntent.RequestOffer -> {
                    requestOffer(intent.deviceCanEnroll)
                }

                BiometricEnrollIntent.AuthenticationStarted -> {
                    dispatch(BiometricEnrollReducerEvent.RegistrationStarted)
                }

                BiometricEnrollIntent.AuthenticationSucceeded -> {
                    register()
                }

                BiometricEnrollIntent.AuthenticationCancelled -> {
                    dispatch(BiometricEnrollReducerEvent.RegistrationEnded)
                }

                is BiometricEnrollIntent.AuthenticationFailed -> {
                    errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, intent.cause)
                    dispatch(BiometricEnrollReducerEvent.RegistrationFailed(BiometricEnrollFailureReason.Authentication))
                }

                BiometricEnrollIntent.Decline -> {
                    decline()
                }

                BiometricEnrollIntent.ConsumeProceed -> {
                    dispatch(BiometricEnrollReducerEvent.ProceedConsumed)
                }

                BiometricEnrollIntent.ConsumeFailure -> {
                    dispatch(BiometricEnrollReducerEvent.FailureConsumed)
                }
            }
        }

        override fun reduce(
            state: BiometricEnrollUiState,
            event: BiometricEnrollReducerEvent,
        ): BiometricEnrollUiState =
            when (event) {
                BiometricEnrollReducerEvent.Offered -> {
                    state.copy(isOffered = true)
                }

                BiometricEnrollReducerEvent.Proceeded -> {
                    state.copy(isOffered = false, isRegistering = false, failure = null, canProceed = true)
                }

                BiometricEnrollReducerEvent.RegistrationStarted -> {
                    state.copy(isRegistering = true, failure = null)
                }

                BiometricEnrollReducerEvent.RegistrationEnded -> {
                    state.copy(isRegistering = false)
                }

                is BiometricEnrollReducerEvent.RegistrationFailed -> {
                    state.copy(isRegistering = false, failure = event.reason)
                }

                BiometricEnrollReducerEvent.ProceedConsumed -> {
                    state.copy(canProceed = false)
                }

                BiometricEnrollReducerEvent.FailureConsumed -> {
                    state.copy(failure = null)
                }
            }

        private fun requestOffer(deviceCanEnroll: Boolean) {
            if (offerJob?.isActive == true || currentState.isOffered) return
            offerJob =
                viewModelScope.launch {
                    if (deviceCanEnroll && shouldOffer()) {
                        dispatch(BiometricEnrollReducerEvent.Offered)
                    } else {
                        dispatch(BiometricEnrollReducerEvent.Proceeded)
                    }
                }
        }

        /**
         * 지문이 맞았다 — 이제서야 서버에 기기를 등록한다. 등록까지 성공해야 켜진 것이다.
         *
         * 등록이 진행 중이면 합류한다: 프롬프트가 성공을 두 번 전달해도 서버 호출은 한 번이다.
         */
        private fun register() {
            if (registerJob?.isActive == true) return
            dispatch(BiometricEnrollReducerEvent.RegistrationStarted)
            registerJob =
                viewModelScope.launch {
                    authRepository
                        .registerBiometric()
                        .onSuccess { dispatch(BiometricEnrollReducerEvent.Proceeded) }
                        .onFailure { cause ->
                            errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, cause)
                            dispatch(BiometricEnrollReducerEvent.RegistrationFailed(BiometricEnrollFailureReason.Registration))
                        }
                }
        }

        /**
         * 「나중에」 — 취소가 아니라 **다시 묻지 말라는 답**이라 기기에 남긴다.
         *
         * 등록 실패 뒤에 닫은 경우에도 남긴다. 서버가 계속 실패하는 동안 로그인마다 같은 시트를 다시 띄우는 쪽이
         * 더 나쁘고, 다시 켜는 자리는 마이 탭의 설정 토글이 맡는다.
         * 기록에 실패해도(프로필 유실) 흐름은 막지 않는다 — 다음 로그인에 한 번 더 물을 뿐이다.
         */
        private fun decline() {
            if (declineJob?.isActive == true) return
            declineJob =
                viewModelScope.launch {
                    authRepository
                        .declineBiometricEnroll()
                        .onFailure { errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, it) }
                    dispatch(BiometricEnrollReducerEvent.Proceeded)
                }
        }

        private suspend fun shouldOffer(): Boolean {
            if (!ensureProfile()) return false
            // 두 판정 모두 현재 사용자 id 와 대조하므로 프로필 확보 뒤에 읽어야 한다 — 그 전에는 둘 다 false 로 보인다.
            if (authRepository.isBiometricEnabled.first()) return false
            return !authRepository.isBiometricEnrollDeclined.first()
        }

        /** 소셜 로그인 직후에는 프로필이 아직 없다 — 이때만 `GET /users/me` 를 한 번 기다린다. */
        private suspend fun ensureProfile(): Boolean {
            if (userProfileRepository.profile.first() != null) return true
            return userProfileRepository
                .refreshProfile()
                .onFailure { errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, it) }
                .isSuccess
        }
    }
