package com.cambridge.feature.onboarding.domain.model

/** 온보딩 4단계 — 기능 스펙 F1-2. [ordinalNumber] 는 화면 진행 표시(STEP n / 4)에 쓰는 1부터 시작하는 번호다. */
public enum class OnboardingStep(
    public val ordinalNumber: Int,
) {
    BasicInfo(1),
    JobPreference(2),
    Experience(3),
    PastApplication(4),
    ;

    /** 다음 단계. 마지막 단계면 null — 그 다음은 [OnboardingProgress.Completed] 다. */
    public val next: OnboardingStep?
        get() = entries.getOrNull(ordinal + 1)

    public companion object {
        public const val TOTAL_STEPS: Int = 4

        /** 저장된 이름을 단계로 되돌린다. 알 수 없는 이름(스키마 변경 등)은 null 로 흘려 호출처가 처음부터 시작하게 한다. */
        public fun fromName(name: String): OnboardingStep? = entries.firstOrNull { it.name == name }
    }
}

/**
 * 저장된 온보딩 진행 상태 — 기능 스펙 F1-1 「온보딩 도중 앱을 종료하더라도, 재진입 시 중단된 단계부터 재개」.
 *
 * [InProgress.step] 은 사용자가 **다음에 이어서 해야 할** 단계다(직전 단계의 저장이 끝난 시점에 갱신된다).
 */
public sealed interface OnboardingProgress {
    /** 아직 어떤 단계도 저장하지 않았다. */
    public data object NotStarted : OnboardingProgress

    /** [step] 부터 이어서 진행한다. */
    public data class InProgress(
        val step: OnboardingStep,
    ) : OnboardingProgress

    /** 4단계를 모두 마쳤다. */
    public data object Completed : OnboardingProgress
}
