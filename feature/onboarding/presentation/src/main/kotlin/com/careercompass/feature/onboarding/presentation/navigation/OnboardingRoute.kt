package com.careercompass.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import kotlinx.serialization.Serializable

/**
 * 온보딩 로컬 Navigation 3 스택의 키. 시작 화면(로그인 / 지문 / 재개 단계)은 앱 셸이 고른다.
 *
 * [NavKey] 는 Nav3 백스택에 실릴 수 있다는 표식이고, `@Serializable` 은 프로세스 재생성 뒤 스택을 복원하는 데 쓰인다.
 * 둘 다 있어야 `rememberNavBackStack` 이 이 키를 저장한다.
 */
@Serializable
public sealed interface OnboardingRoute : NavKey {
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
