package com.careercompass.feature.onboarding.presentation.basicinfo

import androidx.compose.runtime.Immutable
import com.careercompass.feature.onboarding.presentation.shared.model.OnboardingFieldError

/**
 * 학교 선택 시트 상태 — [results] 는 [query] 로 걸러진 목록이다.
 *
 * [directInput] 이 null 이 아니면 시트가 직접 입력 모드다. 두 모드를 한 시트에 둔 이유는
 * [isDirectInputOffered] 참고.
 */
@Immutable
public data class SchoolPickerState(
    public val query: String = "",
    public val results: List<String>,
    public val directInput: SchoolDirectInputState? = null,
) {
    init {
        require(results.all(String::isNotBlank)) { "school names must not be blank" }
        require(results.distinct().size == results.size) { "school names must be unique" }
    }

    /**
     * 「목록에 없어요」 안내를 노출할지.
     *
     * **검색어를 넣은 뒤에만 보여 준다.** 목록 선택이 기본이어야 표기 흔들림이 줄고(#138), 시트를 연
     * 첫 화면에 직접 입력이 나란히 놓이면 목록을 훑어보지도 않고 타이핑하게 된다. 반대로 검색 결과가
     * 0건인데 안내가 없으면 막다른 길이므로, 목록이 제 몫을 다한 뒤에는 결과 건수와 무관하게 연다 —
     * 「서울대」 로 「서울대학교」 만 나오는 대학원생처럼 **결과가 있어도 자기 학교가 아닌** 경우가 있다.
     */
    public val isDirectInputOffered: Boolean
        get() = directInput == null && query.isNotBlank()
}

/**
 * 학교 직접 입력 모드 상태 — 목록에서 못 찾은 사용자의 탈출구다.
 *
 * [value] 는 사용자가 친 그대로다. 다듬기는 저장 직전 `SchoolNameRules` 가 한 번만 한다 —
 * 입력 도중에 공백을 지우면 커서가 튄다.
 */
@Immutable
public data class SchoolDirectInputState(
    public val value: String = "",
    public val error: OnboardingFieldError? = null,
) {
    /** 공백만 남은 값으로는 확정할 수 없다 — 확정 뒤에도 「다음」 이 막히는 헛걸음을 만들지 않는다. */
    public val isConfirmEnabled: Boolean
        get() = value.isNotBlank() && error == null
}

/** User intentions emitted by [SchoolPickerSheet]. */
public sealed interface SchoolPickerEvent {
    public data class QueryChanged(
        public val value: String,
    ) : SchoolPickerEvent

    public data class SchoolSelected(
        public val school: String,
    ) : SchoolPickerEvent

    /** 「목록에 없어요」 — 직접 입력 모드로 넘어간다. */
    public data object DirectInputRequested : SchoolPickerEvent

    public data class DirectInputChanged(
        public val value: String,
    ) : SchoolPickerEvent

    /** 직접 입력값을 학교로 확정한다. */
    public data object DirectInputConfirmed : SchoolPickerEvent

    /** 직접 입력을 접고 목록으로 돌아간다. 시트는 닫히지 않는다. */
    public data object DirectInputCancelled : SchoolPickerEvent

    public data object Dismissed : SchoolPickerEvent
}
