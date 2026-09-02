package com.cambridge.feature.onboarding.presentation.basicinfo

import androidx.compose.runtime.Immutable

/** 학교 선택 시트 상태 — [results] 는 [query] 로 걸러진 목록이다. */
@Immutable
public data class SchoolPickerState(
    public val query: String = "",
    public val results: List<String>,
) {
    init {
        require(results.all(String::isNotBlank)) { "school names must not be blank" }
        require(results.distinct().size == results.size) { "school names must be unique" }
    }
}

/** User intentions emitted by [SchoolPickerSheet]. */
public sealed interface SchoolPickerEvent {
    public data class QueryChanged(
        public val value: String,
    ) : SchoolPickerEvent

    public data class SchoolSelected(
        public val school: String,
    ) : SchoolPickerEvent

    public data object Dismissed : SchoolPickerEvent
}
