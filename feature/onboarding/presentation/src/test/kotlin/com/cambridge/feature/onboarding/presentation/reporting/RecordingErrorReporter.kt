package com.cambridge.feature.onboarding.presentation.reporting

import com.cambridge.core.common.reporting.ErrorReporter

/** 테스트용 [ErrorReporter] — 실제로 기록된(걸러지지 않은) 실패와 속성을 모은다. */
internal class RecordingErrorReporter : ErrorReporter {
    data class Recorded(
        val throwable: Throwable,
        val attributes: Map<String, String>,
    )

    val failures = mutableListOf<Recorded>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += Recorded(throwable, attributes)
    }

    fun stages(): List<String?> = failures.map { it.attributes[ONBOARDING_REPORT_KEY_STAGE] }
}
