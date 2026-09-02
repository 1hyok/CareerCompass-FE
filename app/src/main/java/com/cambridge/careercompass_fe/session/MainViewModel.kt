package com.cambridge.careercompass_fe.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.usecase.auth.ResolveSessionEntryUseCase
import com.cambridge.core.domain.usecase.auth.SessionEntryDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 목적지 결정 — 기능 스펙 F1-1.
 *
 * - 세션 없음 → [AppStartDestination.Login]
 * - 세션 있음 + 이 계정이 이 기기에서 지문 로그인을 켬 → [AppStartDestination.BiometricLogin] (지문 확인 뒤 세션 검증과
 *   온보딩/메인 분기는 지문 화면이 같은 [ResolveSessionEntryUseCase] 로 한다)
 * - 세션 있음 → [ResolveSessionEntryUseCase] 가 `GET /users/me` 로 온보딩/메인을 가른다. 세션 만료(401)는 로그인으로
 *   보낸다. 서버 확인에 실패해 마지막으로 알려진 완료 여부로 판단했으면 그 실패를 기록만 한다.
 *
 * 결과는 [AppShellLaunch] 로 흘린다 — 세션 종료 뒤 같은 목적지가 나와도 [AppShellLaunch.revision] 이 올라 NavHost 가
 * 새로 만들어진다.
 */
@HiltViewModel
public class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val resolveSessionEntry: ResolveSessionEntryUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _launch = MutableStateFlow<AppShellLaunch?>(null)

        /** 초기 진입 시 null(로딩)이며, 세션·프로필 확인 뒤 확정된다. */
        public val launch: StateFlow<AppShellLaunch?> = _launch.asStateFlow()

        // 프로세스마다 다른 시작값 — 이전 프로세스의 NavController 저장 상태와 키가 겹치지 않는다.
        private var revision: Long = System.nanoTime()
        private var resolveJob: Job? = null

        init {
            refresh()
        }

        /**
         * 로그아웃·세션 만료 뒤 시작 목적지를 다시 계산하고 NavHost 를 새로 만들게 한다.
         *
         * 계산이 진행 중이면 합류한다 — 여러 화면이 같은 세션 종료를 동시에 알려도 초기화는 한 번이고, 계산이
         * 겹치지 않으니 늦게 끝난 옛 결과가 새 결과를 덮어쓰지도 않는다.
         */
        public fun refresh() {
            if (resolveJob?.isActive == true) return
            resolveJob =
                viewModelScope.launch {
                    val destination = resolve()
                    revision += 1
                    _launch.value = AppShellLaunch(revision = revision, destination = destination)
                }
        }

        private suspend fun resolve(): AppStartDestination {
            if (!authRepository.isLoggedIn.first()) return AppStartDestination.Login
            if (authRepository.isBiometricEnabled.first()) return AppStartDestination.BiometricLogin
            val entry = resolveSessionEntry()
            entry.fallbackCause?.let { errorReporter.recordFailure(it, attributes = mapOf(KEY_STAGE to STAGE_START_PROFILE)) }
            return when (entry.destination) {
                SessionEntryDestination.Login -> AppStartDestination.Login
                SessionEntryDestination.Onboarding -> AppStartDestination.Onboarding
                SessionEntryDestination.Feed -> AppStartDestination.Main
            }
        }

        private companion object {
            const val KEY_STAGE = "app_stage"
            const val STAGE_START_PROFILE = "start_profile"
        }
    }
