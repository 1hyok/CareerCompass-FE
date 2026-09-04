package com.careercompass.feature.onboarding.presentation.navigation

import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import kotlinx.serialization.Serializable

/** 온보딩 중첩 그래프의 루트 — 앱 셸이 `navigate(OnboardingGraphRoute)` 로 들어오고, 이 그래프의 back stack entry 가 ViewModel 스코프다. */
@Serializable
public data object OnboardingGraphRoute

/** 온보딩 그래프 안의 화면들. 시작 화면(로그인 / 지문 / 재개 단계)은 앱 셸이 고른다. */
@Serializable
public sealed interface OnboardingRoute {
    @Serializable
    public data object Login : OnboardingRoute

    @Serializable
    public data object BiometricLogin : OnboardingRoute

    @Serializable
    public data object Step1 : OnboardingRoute

    @Serializable
    public data object Step2 : OnboardingRoute

    @Serializable
    public data object Step3 : OnboardingRoute

    @Serializable
    public data object Step4 : OnboardingRoute

    @Serializable
    public data object Complete : OnboardingRoute
}

public fun OnboardingStep.toRoute(): OnboardingRoute =
    when (this) {
        OnboardingStep.BasicInfo -> OnboardingRoute.Step1
        OnboardingStep.JobPreference -> OnboardingRoute.Step2
        OnboardingStep.Experience -> OnboardingRoute.Step3
        OnboardingStep.PastApplication -> OnboardingRoute.Step4
    }
