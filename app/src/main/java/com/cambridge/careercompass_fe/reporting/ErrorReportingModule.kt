package com.cambridge.careercompass_fe.reporting

import com.careercompass.core.common.reporting.ErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ErrorReportingModule {
    @Binds
    @Singleton
    abstract fun bindErrorReporter(impl: CrashlyticsErrorReporter): ErrorReporter
}
