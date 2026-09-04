package com.careercompass.feature.onboarding.domain.repository

import com.careercompass.feature.onboarding.domain.model.OnboardingProgress
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import kotlinx.coroutines.flow.Flow

/**
 * 온보딩 진행 상태의 로컬 저장 계약 — 기능 스펙 F1-1 「재진입 시 중단된 단계부터 재개」.
 *
 * 서버에는 단계 단위 진행 상태가 없고(`onboardingDone` 플래그뿐) 이 저장소는 세션에 귀속된다 — 로그아웃하면
 * 함께 비워진다.
 */
public interface OnboardingProgressRepository {
    public val progress: Flow<OnboardingProgress>

    /** 다음에 이어서 진행할 [step] 을 기록한다. */
    public suspend fun save(step: OnboardingStep): Result<Unit>

    /** 4단계를 모두 마쳤다고 기록한다. 이후 [progress] 는 [OnboardingProgress.Completed] 를 흘린다. */
    public suspend fun markCompleted(): Result<Unit>

    /** 진행 기록을 지운다. 이후 [progress] 는 [OnboardingProgress.NotStarted] 를 흘린다. */
    public suspend fun clear(): Result<Unit>
}
