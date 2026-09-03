package com.cambridge.feature.onboarding.presentation.reporting

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.model.auth.SocialProvider

/** 리포팅 속성 키 — 온보딩 흐름의 어느 단계에서 실패했는지. */
public const val ONBOARDING_REPORT_KEY_STAGE: String = "onboarding_stage"

/** 리포팅 속성 키 — 소셜 로그인 실패의 제공자(`kakao` / `google`). */
public const val ONBOARDING_REPORT_KEY_PROVIDER: String = "auth_provider"

/** 온보딩 흐름에서 흡수되는 실패의 발생 지점. [key] 가 리포팅 콘솔에 남는 값이다. */
public enum class OnboardingFailureStage(
    public val key: String,
) {
    SocialTokenRequest("social_token_request"),
    SocialLogin("social_login"),
    BiometricAuth("biometric_auth"),
    BiometricSessionVerify("biometric_session_verify"),

    /** 지문 등록 제안 — 프로필 확보·지문 확인·서버 등록·거절 기록의 실패를 한 단계로 묶는다. */
    BiometricEnroll("biometric_enroll"),
    ResolveEntry("resolve_entry"),
    SaveBasicInfo("save_basic_info"),
    SaveJobPreferences("save_job_preferences"),
    LoadExperiences("load_experiences"),
    AddExperience("add_experience"),
    UpdateExperience("update_experience"),
    DeleteExperience("delete_experience"),
    ProceedToPastApplication("proceed_to_past_application"),
    LoadPastApplications("load_past_applications"),
    UploadPastApplication("upload_past_application"),
    DeletePastApplication("delete_past_application"),
    UpdatePastApplicationItemCategory("update_past_application_item_category"),
    Complete("complete"),
}

/**
 * 온보딩 실패를 단계·제공자 속성과 함께 non-fatal 로 남긴다.
 *
 * [CoreAuthFailure.UserCancelledAuth] 는 사용자의 의도된 행동이라 기록하지 않는다 — 기록하면 실제 장애 신호가
 * 취소 잡음에 묻힌다. 코루틴 취소는 [ErrorReporter.recordFailure] 가 거른다.
 */
public fun ErrorReporter.recordOnboardingFailure(
    stage: OnboardingFailureStage,
    throwable: Throwable,
    provider: SocialProvider? = null,
) {
    if (throwable is CoreAuthFailure.UserCancelledAuth) return
    recordFailure(
        throwable = throwable,
        attributes =
            buildMap {
                put(ONBOARDING_REPORT_KEY_STAGE, stage.key)
                provider?.let { put(ONBOARDING_REPORT_KEY_PROVIDER, it.name.lowercase()) }
            },
    )
}
