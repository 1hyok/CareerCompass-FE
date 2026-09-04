package com.careercompass.feature.onboarding.presentation.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.careercompass.feature.onboarding.presentation.reporting.recordOnboardingFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 지문 등록 제안의 실패 사유. 문구는 [BiometricEnrollGate] 가 리소스로 만든다. */
public enum class BiometricEnrollFailureReason {
    /** 지문 확인이 실패·잠금·불가로 끝났다. */
    Authentication,

    /** 지문은 확인했지만 서버 등록이 실패했다. */
    Registration,
}

/**
 * [BiometricEnrollViewModel] 상태.
 *
 * @property isOffered 제안 시트가 떠 있는지.
 * @property isRegistering 프롬프트가 떠 있는 동안과 서버 등록을 기다리는 동안 true — 둘 다 「버튼을 다시 누르면 안
 *   되는」 같은 상태라 화면은 구분하지 않는다.
 * @property canProceed 제안이 끝나 원래 이동을 이어서 해도 되는 시점. 단발 신호다.
 */
public data class BiometricEnrollViewState(
    val isOffered: Boolean = false,
    val isRegistering: Boolean = false,
    val failure: BiometricEnrollFailureReason? = null,
    val canProceed: Boolean = false,
)

/**
 * 지문 빠른 로그인을 **켜는** 경로 — 기능 스펙 F1-1(#98).
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
 * [BiometricEnrollViewState.canProceed] 로 원래 이동을 이어 준다.
 */
@HiltViewModel
public class BiometricEnrollViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BiometricEnrollViewState())
        public val uiState: StateFlow<BiometricEnrollViewState> = _uiState.asStateFlow()

        private var offerJob: Job? = null
        private var registerJob: Job? = null
        private var declineJob: Job? = null

        /**
         * 제안할지 정한다. 제안하지 않기로 하면 곧바로 통과 신호를 낸다.
         *
         * @param deviceCanEnroll 이 기기·호스트에서 강한 생체 인증을 지금 쓸 수 있는가.
         */
        public fun onOfferRequested(deviceCanEnroll: Boolean) {
            if (offerJob?.isActive == true || _uiState.value.isOffered) return
            offerJob =
                viewModelScope.launch {
                    if (deviceCanEnroll && shouldOffer()) {
                        _uiState.update { it.copy(isOffered = true) }
                    } else {
                        proceed()
                    }
                }
        }

        /** 프롬프트가 떴다 — 결과가 올 때까지 시트의 버튼을 잠근다. */
        public fun onAuthenticationStarted() {
            _uiState.update { it.copy(isRegistering = true, failure = null) }
        }

        /**
         * 지문이 맞았다 — 이제서야 서버에 기기를 등록한다. 등록까지 성공해야 켜진 것이다.
         *
         * 등록이 진행 중이면 합류한다: 프롬프트가 성공을 두 번 전달해도 서버 호출은 한 번이다.
         */
        public fun onAuthenticationSucceeded() {
            if (registerJob?.isActive == true) return
            _uiState.update { it.copy(isRegistering = true, failure = null) }
            registerJob =
                viewModelScope.launch {
                    authRepository
                        .registerBiometric()
                        .onSuccess { proceed() }
                        .onFailure { cause ->
                            errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, cause)
                            _uiState.update {
                                it.copy(isRegistering = false, failure = BiometricEnrollFailureReason.Registration)
                            }
                        }
                }
        }

        /** 사용자가 프롬프트를 닫았다 — 아직 답을 고르는 중이므로 시트를 그대로 두고 표시도 기록도 하지 않는다. */
        public fun onAuthenticationCancelled() {
            _uiState.update { it.copy(isRegistering = false) }
        }

        /**
         * 지문 확인이 오류로 끝났다. 프롬프트가 준 [BiometricFailureReason] 은 받지 않는다 — 잠금이든 미지원이든
         * 이 시트가 할 말은 「확인하지 못했다」 하나뿐이라, 사유는 [cause] 로 리포팅에만 남는다.
         */
        public fun onAuthenticationFailed(cause: Throwable) {
            errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, cause)
            _uiState.update { it.copy(isRegistering = false, failure = BiometricEnrollFailureReason.Authentication) }
        }

        /**
         * 「나중에」 — 취소가 아니라 **다시 묻지 말라는 답**이라 기기에 남긴다. 시트를 스와이프로 닫는 것도 같다.
         *
         * 등록 실패 뒤에 닫은 경우에도 남긴다. 서버가 계속 실패하는 동안 로그인마다 같은 시트를 다시 띄우는 쪽이
         * 더 나쁘고, 다시 켜는 자리는 마이 탭의 설정 토글이 맡는다.
         * 기록에 실패해도(프로필 유실) 흐름은 막지 않는다 — 다음 로그인에 한 번 더 물을 뿐이다.
         */
        public fun onDeclined() {
            if (declineJob?.isActive == true) return
            declineJob =
                viewModelScope.launch {
                    authRepository
                        .declineBiometricEnroll()
                        .onFailure { errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricEnroll, it) }
                    proceed()
                }
        }

        public fun onProceedConsumed() {
            _uiState.update { it.copy(canProceed = false) }
        }

        public fun onFailureConsumed() {
            _uiState.update { it.copy(failure = null) }
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

        /** 제안이 끝났다 — 시트를 닫고 원래 이동을 이어서 하라고 알린다. */
        private fun proceed() {
            _uiState.update {
                it.copy(isOffered = false, isRegistering = false, failure = null, canProceed = true)
            }
        }
    }
