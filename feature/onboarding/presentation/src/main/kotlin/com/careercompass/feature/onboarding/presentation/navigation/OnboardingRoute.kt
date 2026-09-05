package com.careercompass.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import kotlinx.serialization.Serializable

/**
 * 루트 `NavHost`(Navigation 2)에 남는 온보딩 host destination — 앱 셸이 `navigate(OnboardingGraphRoute)` 로 들어오고,
 * 그 안에서 [OnboardingNavHost] 가 로컬 Navigation 3 스택을 돌린다. 이 엔트리가 host 스코프 ViewModel 의 소유자다.
 */
@Serializable
public data object OnboardingGraphRoute

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
