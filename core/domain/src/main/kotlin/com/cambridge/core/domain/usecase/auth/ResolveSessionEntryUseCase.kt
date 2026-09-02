package com.cambridge.core.domain.usecase.auth

import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import javax.inject.Inject

/** 저장된 세션으로 들어갈 화면. */
public enum class SessionEntryDestination {
    /** 세션이 끝났다(401) — 로컬 세션은 정리됐고 로그인부터 다시. */
    Login,

    /** 온보딩 미완료 — 재개 단계는 온보딩 그래프가 정한다. */
    Onboarding,

    /** 온보딩 완료 — 피드. */
    Feed,
}

/**
 * 세션 진입 판정 결과.
 *
 * @property destination 들어갈 화면.
 * @property fallbackCause 서버 확인에 실패해 마지막으로 알려진 온보딩 상태로 판단했을 때 그 실패. 서버가 확정했거나
 *   세션이 끝났으면 null. 진입 자체는 막지 않으므로 호출처가 계측만 한다.
 */
public data class SessionEntry(
    val destination: SessionEntryDestination,
    val fallbackCause: Throwable? = null,
)

/**
 * 저장된 세션이 아직 살아 있는지, 온보딩을 마쳤는지를 `GET /users/me` 로 확정한다 — 기능 스펙 F1-1.
 *
 * 앱 시작(스플래시)과 지문 빠른 로그인 성공 뒤가 같은 판정을 쓴다. 지문 성공은 새 세션 발급이 아니라 저장된
 * 세션을 그대로 쓰겠다는 확인이라, 서버가 그 세션을 아직 받아 주는지 여기서 확인해야 피드에 들어갔다가 401 로
 * 튕겨 나오는 두 번 이동이 없다(#81).
 *
 * - 성공 → `onboardingDone` 으로 [SessionEntryDestination.Feed] / [SessionEntryDestination.Onboarding].
 * - [CoreDataFailure.Unauthorized] → 로컬 세션을 정리하고 [SessionEntryDestination.Login]. network 계층이 이미
 *   정리했어도 다시 부른다 — 정리는 멱등이고, 「로그인으로 보낸다」 와 「토큰이 남아 있다」 가 어긋나면 다음 시작이
 *   또 지문 화면으로 간다.
 * - 그 밖의 실패 → 마지막으로 알려진 완료 여부([UserProfileRepository.lastKnownOnboardingDone])로 판단하고
 *   [SessionEntry.fallbackCause] 에 실패를 싣는다. 모르면 온보딩이다 — 온보딩 진입 판정이 서버를 다시 확인하므로
 *   완료 사용자는 네트워크가 돌아오면 피드로 간다. 반대로 모를 때 피드로 보내면 신규 사용자가 온보딩 없이
 *   들어가고 아무도 되돌리지 않는다.
 */
public class ResolveSessionEntryUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
    ) {
        public suspend operator fun invoke(): SessionEntry {
            val refreshed = userProfileRepository.refreshProfile()
            refreshed.onSuccess { profile -> return SessionEntry(profile.onboardingDone.toDestination()) }
            val failure = checkNotNull(refreshed.exceptionOrNull())
            if (failure is CoreDataFailure.Unauthorized) {
                authRepository.clearSession()
                return SessionEntry(SessionEntryDestination.Login)
            }
            val lastKnownDone = userProfileRepository.lastKnownOnboardingDone() == true
            return SessionEntry(lastKnownDone.toDestination(), fallbackCause = failure)
        }

        private fun Boolean.toDestination(): SessionEntryDestination =
            if (this) SessionEntryDestination.Feed else SessionEntryDestination.Onboarding
    }
