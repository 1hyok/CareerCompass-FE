package com.cambridge.careercompass_fe.reporting

import com.careercompass.core.common.reporting.ErrorReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ErrorReporter] 의 Crashlytics 구현. 취소 필터링·문구 제거는 인터페이스의 [ErrorReporter.recordFailure] 가
 * 이미 끝냈으므로 여기서는 속성만 붙여 non-fatal 로 기록한다.
 *
 * debug 빌드는 매니페스트에서 수집이 꺼져 있어(`firebase_crashlytics_collection_enabled=false`) 호출이 무시된다.
 */
@Singleton
internal class CrashlyticsErrorReporter
    @Inject
    constructor() : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            val crashlytics = FirebaseCrashlytics.getInstance()
            attributes.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
            crashlytics.recordException(throwable)
        }
    }
