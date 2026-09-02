package com.cambridge.careercompass_fe.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.domain.usecase.auth.LogoutUseCase
import com.cambridge.core.model.user.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 마이 탭 자리표시자가 그리는 값. 프로필을 모르면 이름·소속이 null 이고 화면이 대체 문구를 쓴다. */
internal data class MyTabPlaceholderUiState(
    val name: String? = null,
    val affiliation: String? = null,
    val isLogoutDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
    val sessionEnded: Boolean = false,
)

internal sealed interface MyTabPlaceholderEvent {
    data object LogoutClicked : MyTabPlaceholderEvent

    data object LogoutConfirmed : MyTabPlaceholderEvent

    data object LogoutDismissed : MyTabPlaceholderEvent
}

/**
 * 마이 탭 자리표시자의 세션 상태 — profile 모듈이 마이 탭을 인수하면 화면과 함께 통째로 사라진다.
 *
 * 이름·소속은 프로필 캐시([UserProfileRepository.profile])에서만 읽는다 — 탭을 열 때마다 `GET /users/me` 를
 * 부르지 않는다. 서버와 맞추는 일은 시작 경로([com.cambridge.careercompass_fe.session.MainViewModel])와 프로필을
 * 실제로 소유할 profile 모듈 몫이다.
 *
 * 로그아웃은 성공·실패와 무관하게 [MyTabPlaceholderUiState.sessionEnded] 로 끝난다 — [LogoutUseCase] 는 서버
 * 호출이 실패해도 로컬 세션을 정리하므로, 실패했다고 사용자를 로그인된 화면에 붙잡아 두면 나갈 방법이 없다.
 * 실패는 문구 대신 리포팅으로 남긴다.
 */
@HiltViewModel
internal class MyTabPlaceholderViewModel
    @Inject
    constructor(
        userProfileRepository: UserProfileRepository,
        private val logout: LogoutUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _state = MutableStateFlow(MyTabPlaceholderUiState())
        val state: StateFlow<MyTabPlaceholderUiState> = _state.asStateFlow()

        private var logoutJob: Job? = null

        init {
            viewModelScope.launch {
                userProfileRepository.profile.collect { profile ->
                    _state.update { it.copy(name = profile?.name, affiliation = profile?.affiliation()) }
                }
            }
        }

        fun onEvent(event: MyTabPlaceholderEvent) {
            when (event) {
                MyTabPlaceholderEvent.LogoutClicked -> _state.update { it.copy(isLogoutDialogVisible = true) }
                MyTabPlaceholderEvent.LogoutDismissed -> _state.update { it.copy(isLogoutDialogVisible = false) }
                MyTabPlaceholderEvent.LogoutConfirmed -> endSession()
            }
        }

        /** 세션 종료를 알린 뒤 호출한다 — 셸이 시작 목적지를 다시 계산하는 사이 같은 알림이 되풀이되지 않게. */
        fun onSessionEndedConsumed() {
            _state.update { it.copy(sessionEnded = false) }
        }

        /** 진행 중인 로그아웃이 있으면 무시한다 — 다이얼로그를 닫아도 버튼 연타가 요청을 겹치게 하지 않는다. */
        private fun endSession() {
            if (logoutJob?.isActive == true) return
            _state.update { it.copy(isLogoutDialogVisible = false, isLoggingOut = true) }
            logoutJob =
                viewModelScope.launch {
                    logout().onFailure { cause ->
                        errorReporter.recordFailure(cause, attributes = mapOf(KEY_STAGE to STAGE_LOGOUT))
                    }
                    _state.update { it.copy(isLoggingOut = false, sessionEnded = true) }
                }
        }

        /** 학교·학과 중 아는 것만 잇는다. 둘 다 모르면 null 이고 화면이 대체 문구를 쓴다. */
        private fun UserProfile.affiliation(): String? =
            listOfNotNull(school, department).joinToString(AFFILIATION_SEPARATOR).ifBlank { null }

        private companion object {
            const val AFFILIATION_SEPARATOR = " · "
            const val KEY_STAGE = "app_stage"
            const val STAGE_LOGOUT = "logout"
        }
    }
