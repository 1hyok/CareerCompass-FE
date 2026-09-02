package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.onboarding.domain.model.OnboardingProgress
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.repository.OnboardingProgressRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 온보딩 진입 판정 결과.
 *
 * @property progress 재개할 지점. [OnboardingProgress.NotStarted] 는 오지 않는다 — 시작 전이면 첫 단계 [OnboardingProgress.InProgress] 다.
 * @property profile 프리필에 쓸 프로필. 갱신에 실패했고 캐시도 없으면 null.
 * @property profileRefreshFailure `GET /users/me` 실패 원인. 진입 자체는 막지 않으므로 호출처가 계측만 한다.
 */
public data class OnboardingEntry(
    val progress: OnboardingProgress,
    val profile: UserProfile?,
    val profileRefreshFailure: Throwable?,
) {
    init {
        require(progress !is OnboardingProgress.NotStarted) { "progress must be resolved to a step or Completed" }
    }
}

/**
 * 온보딩에 들어갈 때 어느 단계부터 보여줄지 정한다 — 기능 스펙 F1-1 「재진입 시 중단된 단계부터 재개」.
 *
 * 판정 순서:
 * 1. 서버 프로필의 `onboardingDone` 이 true 면 [OnboardingProgress.Completed]. 서버가 정본이다.
 * 2. 아니면 로컬 진행 기록을 따른다 — [OnboardingProgress.Completed] 는 그대로(4단계를 마쳤지만 서버 플래그가
 *    아직 뒤따르지 않은 경우), [OnboardingProgress.InProgress] 는 그 단계, 기록이 없으면 [OnboardingStep.BasicInfo].
 *
 * 프로필 갱신 실패는 진입을 막지 않는다 — 캐시된 프로필로 프리필하고, 그마저 없으면 빈 폼으로 시작한다.
 */
public class ResolveOnboardingEntryUseCase
    @Inject
    constructor(
        private val userProfileRepository: UserProfileRepository,
        private val progressRepository: OnboardingProgressRepository,
    ) {
        public suspend operator fun invoke(): OnboardingEntry {
            val refreshed = userProfileRepository.refreshProfile()
            val profile = refreshed.getOrNull() ?: userProfileRepository.profile.first()
            val refreshFailure = refreshed.exceptionOrNull()

            if (profile?.onboardingDone == true) {
                return OnboardingEntry(OnboardingProgress.Completed, profile, refreshFailure)
            }
            val progress =
                when (val saved = progressRepository.progress.first()) {
                    OnboardingProgress.Completed -> OnboardingProgress.Completed
                    is OnboardingProgress.InProgress -> saved
                    OnboardingProgress.NotStarted -> OnboardingProgress.InProgress(OnboardingStep.BasicInfo)
                }
            return OnboardingEntry(progress, profile, refreshFailure)
        }
    }
