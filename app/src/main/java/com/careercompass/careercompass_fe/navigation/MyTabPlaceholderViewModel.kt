package com.careercompass.careercompass_fe.navigation

import androidx.lifecycle.viewModelScope
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.common.reporting.recordStagedFailure
import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.domain.settings.AppSettingsRepository
import com.careercompass.core.domain.usecase.auth.LogoutUseCase
import com.careercompass.core.model.settings.ThemeMode
import com.careercompass.core.model.user.UserProfile
import com.careercompass.core.ui.mvi.MviIntent
import com.careercompass.core.ui.mvi.MviViewModel
import com.careercompass.core.ui.mvi.ReducerEvent
import com.careercompass.core.ui.mvi.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 마이 탭 자리표시자가 그리는 값. 프로필을 모르면 이름·소속이 null 이고 화면이 대체 문구를 쓴다.
 *
 * @property canEnrollBiometric 이 기기에서 지문을 **등록**할 수 있는가. 플랫폼을 아는 화면이 알려 주기 전까지는
 *   null(판정 전)이다 — false 로 시작하면 첫 프레임에 「쓸 수 없다」 안내가 한 번 스쳤다 사라진다.
 * @property isBiometricBusy 프롬프트가 떠 있는 동안과 서버 등록·해제를 기다리는 동안 true. 둘 다 「스위치를 다시
 *   움직이면 안 되는」 같은 상태라 화면은 구분하지 않는다.
 * @property isEnrollPromptRequested 켜기를 눌러 등록 프롬프트를 띄워야 한다. 화면이 띄우고 소비하는 단발 신호다.
 */
internal data class MyTabPlaceholderUiState(
    val name: String? = null,
    val affiliation: String? = null,
    val isBiometricEnabled: Boolean = false,
    val canEnrollBiometric: Boolean? = null,
    val isBiometricBusy: Boolean = false,
    val isEnrollPromptRequested: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val isThemeDialogVisible: Boolean = false,
    val isLogoutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
    val sessionEnded: Boolean = false,
) : UiState {
    /**
     * 지문 스위치를 지금 움직일 수 있는가.
     *
     * **끄는 방향은 기기 지원과 무관하게 늘 열어 둔다.** 켜 둔 채 지문을 지운 기기는 시작 목적지가 지문 화면이라
     * ([com.careercompass.careercompass_fe.session.MainViewModel]) 되돌릴 자리가 여기뿐인데, 기기 판정으로 함께 잠그면
     * 그 사용자가 갇힌다. 켜는 방향만 [canEnrollBiometric] 이 확정되고 참일 때 연다.
     */
    val isBiometricSwitchEnabled: Boolean
        get() = !isBiometricBusy && (isBiometricEnabled || canEnrollBiometric == true)

    /** 「이 기기에서는 켤 수 없다」 안내를 보일 때 — 판정이 끝났고, 못 쓰는데 켜져 있지도 않은 경우뿐이다. */
    val isBiometricUnavailableNoticeVisible: Boolean
        get() = canEnrollBiometric == false && !isBiometricEnabled
}

internal sealed interface MyTabPlaceholderEvent {
    data class BiometricToggled(
        val enabled: Boolean,
    ) : MyTabPlaceholderEvent

    data object ThemeClicked : MyTabPlaceholderEvent

    data class ThemeSelected(
        val mode: ThemeMode,
    ) : MyTabPlaceholderEvent

    data object ThemeDismissed : MyTabPlaceholderEvent

    data object LogoutClicked : MyTabPlaceholderEvent

    data object LogoutConfirmed : MyTabPlaceholderEvent

    data object LogoutDismissed : MyTabPlaceholderEvent
}

/** 화면이 [MyTabPlaceholderViewModel] 에 보내는 것. 생체 프롬프트의 결과도 여기로 들어온다. */
internal sealed interface MyTabPlaceholderIntent : MviIntent {
    data class Screen(
        val event: MyTabPlaceholderEvent,
    ) : MyTabPlaceholderIntent

    /** 기기가 강한 생체 인증을 지금 쓸 수 있는지 — 플랫폼을 아는 화면이 알려 준다(#98 의 등록 관문과 같은 배선). */
    data class BiometricAvailabilityChanged(
        val canEnroll: Boolean,
    ) : MyTabPlaceholderIntent

    /** 등록 프롬프트를 띄운 뒤 보낸다 — 재구성 때마다 같은 프롬프트가 다시 뜨지 않게. */
    data object ConsumeEnrollPromptRequest : MyTabPlaceholderIntent

    /** 지문이 맞았다 — 이제서야 서버에 기기를 등록한다. */
    data object BiometricEnrollSucceeded : MyTabPlaceholderIntent

    /** 사용자가 프롬프트를 닫았다 — 켜지 않기로 한 것이라 스위치를 원래 자리에 두고 잠금만 푼다. */
    data object BiometricEnrollCancelled : MyTabPlaceholderIntent

    /** 지문 확인이 오류로 끝났다. 스위치는 원래 자리에 남고 사유는 리포팅에만 남는다. */
    data class BiometricEnrollFailed(
        val cause: Throwable,
    ) : MyTabPlaceholderIntent

    /** 세션 종료를 알린 뒤 보낸다 — 셸이 시작 목적지를 다시 계산하는 사이 같은 알림이 되풀이되지 않게. */
    data object ConsumeSessionEnded : MyTabPlaceholderIntent
}

/** 상태가 겪은 것. [MyTabPlaceholderViewModel] 만 만든다. */
internal sealed interface MyTabPlaceholderReducerEvent : ReducerEvent {
    data class ProfileChanged(
        val name: String?,
        val affiliation: String?,
    ) : MyTabPlaceholderReducerEvent

    data class BiometricEnabledChanged(
        val isEnabled: Boolean,
    ) : MyTabPlaceholderReducerEvent

    data class ThemeModeChanged(
        val mode: ThemeMode,
    ) : MyTabPlaceholderReducerEvent

    data class BiometricAvailabilityChanged(
        val canEnroll: Boolean,
    ) : MyTabPlaceholderReducerEvent

    data class ThemeDialogVisibilityChanged(
        val isVisible: Boolean,
    ) : MyTabPlaceholderReducerEvent

    data class LogoutDialogVisibilityChanged(
        val isVisible: Boolean,
    ) : MyTabPlaceholderReducerEvent

    /** 켜기를 눌렀다 — 스위치를 잠그고 프롬프트를 요청한다. */
    data object EnrollPromptRequested : MyTabPlaceholderReducerEvent

    data object EnrollPromptRequestConsumed : MyTabPlaceholderReducerEvent

    data class BiometricBusyChanged(
        val isBusy: Boolean,
    ) : MyTabPlaceholderReducerEvent

    /** 로그아웃 요청이 나갔다 — 다이얼로그를 닫고 진행 표시를 켠다. */
    data object LogoutStarted : MyTabPlaceholderReducerEvent

    /** 로그아웃이 끝났다(성공·실패 무관) — 세션 종료 신호를 올린다. */
    data object LoggedOut : MyTabPlaceholderReducerEvent

    data object SessionEndedConsumed : MyTabPlaceholderReducerEvent
}

/**
 * 마이 탭 자리표시자의 세션 상태 — profile 모듈이 마이 탭을 인수하면 화면과 함께 통째로 사라진다. 진입점은 [onIntent]
 * 하나, 전이는 [reduce] 한 곳이다(#252).
 *
 * 이름·소속은 프로필 캐시([UserProfileRepository.profile])에서만 읽는다 — 탭을 열 때마다 `GET /users/me` 를
 * 부르지 않는다. 서버와 맞추는 일은 시작 경로([com.careercompass.careercompass_fe.session.MainViewModel])와 프로필을
 * 실제로 소유할 profile 모듈 몫이다.
 *
 * 로그아웃은 성공·실패와 무관하게 [MyTabPlaceholderUiState.sessionEnded] 로 끝난다 — [LogoutUseCase] 는 서버
 * 호출이 실패해도 로컬 세션을 정리하므로, 실패했다고 사용자를 로그인된 화면에 붙잡아 두면 나갈 방법이 없다.
 * 실패는 문구 대신 리포팅으로 남긴다.
 *
 * 지문 로그인 스위치(#113)도 같다. 스위치가 그리는 값은 [AuthRepository.isBiometricEnabled] 하나뿐이라 등록·해제가
 * 실패하면 스위치는 저절로 원래 자리에 남고, 사유는 리포팅에만 쌓인다 — 사용자가 다시 누르면 되는 자리에 문구를
 * 띄우려면 자리표시자에 없는 스낵바 배선이 필요하다. 「나중에」로 넘긴 기록([AuthRepository.isBiometricEnrollDeclined])
 * 은 보지 않는다: 제안을 다시 하지 않겠다는 답일 뿐, 직접 찾아온 이 경로까지 막을 이유가 없다.
 */
@HiltViewModel
internal class MyTabPlaceholderViewModel
    @Inject
    constructor(
        userProfileRepository: UserProfileRepository,
        private val authRepository: AuthRepository,
        private val logout: LogoutUseCase,
        private val appSettingsRepository: AppSettingsRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<MyTabPlaceholderIntent, MyTabPlaceholderUiState, MyTabPlaceholderReducerEvent>(MyTabPlaceholderUiState()) {
        private var logoutJob: Job? = null
        private var biometricJob: Job? = null

        init {
            viewModelScope.launch {
                userProfileRepository.profile.collect { profile ->
                    dispatch(MyTabPlaceholderReducerEvent.ProfileChanged(name = profile?.name, affiliation = profile?.affiliation()))
                }
            }
            viewModelScope.launch {
                authRepository.isBiometricEnabled.collect { enabled ->
                    dispatch(MyTabPlaceholderReducerEvent.BiometricEnabledChanged(enabled))
                }
            }
            viewModelScope.launch {
                appSettingsRepository.themeMode.collect { mode -> dispatch(MyTabPlaceholderReducerEvent.ThemeModeChanged(mode)) }
            }
        }

        override fun onIntent(intent: MyTabPlaceholderIntent) {
            when (intent) {
                is MyTabPlaceholderIntent.Screen -> {
                    onEvent(intent.event)
                }

                is MyTabPlaceholderIntent.BiometricAvailabilityChanged -> {
                    dispatch(MyTabPlaceholderReducerEvent.BiometricAvailabilityChanged(intent.canEnroll))
                }

                MyTabPlaceholderIntent.ConsumeEnrollPromptRequest -> {
                    dispatch(MyTabPlaceholderReducerEvent.EnrollPromptRequestConsumed)
                }

                MyTabPlaceholderIntent.BiometricEnrollSucceeded -> {
                    registerBiometric()
                }

                MyTabPlaceholderIntent.BiometricEnrollCancelled -> {
                    dispatch(MyTabPlaceholderReducerEvent.BiometricBusyChanged(false))
                }

                is MyTabPlaceholderIntent.BiometricEnrollFailed -> {
                    recordBiometricFailure(intent.cause)
                    dispatch(MyTabPlaceholderReducerEvent.BiometricBusyChanged(false))
                }

                MyTabPlaceholderIntent.ConsumeSessionEnded -> {
                    dispatch(MyTabPlaceholderReducerEvent.SessionEndedConsumed)
                }
            }
        }

        override fun reduce(
            state: MyTabPlaceholderUiState,
            event: MyTabPlaceholderReducerEvent,
        ): MyTabPlaceholderUiState =
            when (event) {
                is MyTabPlaceholderReducerEvent.ProfileChanged -> state.copy(name = event.name, affiliation = event.affiliation)
                is MyTabPlaceholderReducerEvent.BiometricEnabledChanged -> state.copy(isBiometricEnabled = event.isEnabled)
                is MyTabPlaceholderReducerEvent.ThemeModeChanged -> state.copy(themeMode = event.mode)
                is MyTabPlaceholderReducerEvent.BiometricAvailabilityChanged -> state.copy(canEnrollBiometric = event.canEnroll)
                is MyTabPlaceholderReducerEvent.ThemeDialogVisibilityChanged -> state.copy(isThemeDialogVisible = event.isVisible)
                is MyTabPlaceholderReducerEvent.LogoutDialogVisibilityChanged -> state.copy(isLogoutDialogVisible = event.isVisible)
                MyTabPlaceholderReducerEvent.EnrollPromptRequested -> state.copy(isBiometricBusy = true, isEnrollPromptRequested = true)
                MyTabPlaceholderReducerEvent.EnrollPromptRequestConsumed -> state.copy(isEnrollPromptRequested = false)
                is MyTabPlaceholderReducerEvent.BiometricBusyChanged -> state.copy(isBiometricBusy = event.isBusy)
                MyTabPlaceholderReducerEvent.LogoutStarted -> state.copy(isLogoutDialogVisible = false, isLoggingOut = true)
                MyTabPlaceholderReducerEvent.LoggedOut -> state.copy(isLoggingOut = false, sessionEnded = true)
                MyTabPlaceholderReducerEvent.SessionEndedConsumed -> state.copy(sessionEnded = false)
            }

        private fun onEvent(event: MyTabPlaceholderEvent) {
            when (event) {
                is MyTabPlaceholderEvent.BiometricToggled -> toggleBiometric(event.enabled)
                MyTabPlaceholderEvent.ThemeClicked -> dispatch(MyTabPlaceholderReducerEvent.ThemeDialogVisibilityChanged(true))
                MyTabPlaceholderEvent.ThemeDismissed -> dispatch(MyTabPlaceholderReducerEvent.ThemeDialogVisibilityChanged(false))
                is MyTabPlaceholderEvent.ThemeSelected -> selectThemeMode(event.mode)
                MyTabPlaceholderEvent.LogoutClicked -> dispatch(MyTabPlaceholderReducerEvent.LogoutDialogVisibilityChanged(true))
                MyTabPlaceholderEvent.LogoutDismissed -> dispatch(MyTabPlaceholderReducerEvent.LogoutDialogVisibilityChanged(false))
                MyTabPlaceholderEvent.LogoutConfirmed -> endSession()
            }
        }

        /**
         * 지문이 맞았다 — 이제서야 서버에 기기를 등록한다. 등록까지 성공해야 켜진 것이다(#98 과 같은 규칙).
         *
         * 등록이 진행 중이면 무시한다: 프롬프트가 성공을 두 번 전달해도 서버 호출은 한 번이다.
         */
        private fun registerBiometric() {
            if (biometricJob?.isActive == true) return
            dispatch(MyTabPlaceholderReducerEvent.BiometricBusyChanged(true))
            biometricJob =
                viewModelScope.launch {
                    authRepository.registerBiometric().onFailure(::recordBiometricFailure)
                    dispatch(MyTabPlaceholderReducerEvent.BiometricBusyChanged(false))
                }
        }

        /**
         * 켜는 쪽은 프롬프트를 **요청**만 한다 — 생체 프롬프트는 Activity 를 아는 화면이 띄우고 결과만 돌아온다.
         * 끄는 쪽은 확인할 것이 없어 곧바로 등록 기록을 지운다.
         */
        private fun toggleBiometric(enabled: Boolean) {
            if (currentState.isBiometricBusy) return
            if (enabled) {
                dispatch(MyTabPlaceholderReducerEvent.EnrollPromptRequested)
            } else {
                disableBiometric()
            }
        }

        private fun disableBiometric() {
            dispatch(MyTabPlaceholderReducerEvent.BiometricBusyChanged(true))
            biometricJob =
                viewModelScope.launch {
                    authRepository.setBiometricEnabled(false).onFailure(::recordBiometricFailure)
                    dispatch(MyTabPlaceholderReducerEvent.BiometricBusyChanged(false))
                }
        }

        /**
         * 고른 테마를 저장한다. 다이얼로그는 **저장을 기다리지 않고** 닫는다 — 화면은 저장소 흐름을 되비추므로
         * 저장이 실패하면 값이 저절로 원래대로 남는다(지문 스위치와 같은 규칙).
         */
        private fun selectThemeMode(mode: ThemeMode) {
            dispatch(MyTabPlaceholderReducerEvent.ThemeDialogVisibilityChanged(false))
            if (mode == currentState.themeMode) return
            viewModelScope.launch {
                runCatching { appSettingsRepository.setThemeMode(mode) }
                    .onFailure { cause ->
                        errorReporter.recordStagedFailure(stageKey = KEY_STAGE, stage = STAGE_THEME, throwable = cause)
                    }
            }
        }

        /** 진행 중인 로그아웃이 있으면 무시한다 — 다이얼로그를 닫아도 버튼 연타가 요청을 겹치게 하지 않는다. */
        private fun endSession() {
            if (logoutJob?.isActive == true) return
            dispatch(MyTabPlaceholderReducerEvent.LogoutStarted)
            logoutJob =
                viewModelScope.launch {
                    logout().onFailure { cause ->
                        errorReporter.recordStagedFailure(stageKey = KEY_STAGE, stage = STAGE_LOGOUT, throwable = cause)
                    }
                    dispatch(MyTabPlaceholderReducerEvent.LoggedOut)
                }
        }

        private fun recordBiometricFailure(cause: Throwable) {
            errorReporter.recordStagedFailure(stageKey = KEY_STAGE, stage = STAGE_BIOMETRIC, throwable = cause)
        }

        /** 학교·학과 중 아는 것만 잇는다. 둘 다 모르면 null 이고 화면이 대체 문구를 쓴다. */
        private fun UserProfile.affiliation(): String? =
            listOfNotNull(school, department).joinToString(AFFILIATION_SEPARATOR).ifBlank { null }

        private companion object {
            const val AFFILIATION_SEPARATOR = " · "
            const val KEY_STAGE = "app_stage"
            const val STAGE_LOGOUT = "logout"
            const val STAGE_BIOMETRIC = "biometric_toggle"
            const val STAGE_THEME = "theme_mode"
        }
    }
