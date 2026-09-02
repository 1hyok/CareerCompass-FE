package com.cambridge.feature.onboarding.data.di

import javax.inject.Qualifier

/** Hilt 한정자: 온보딩 진행 상태 저장 전용 [androidx.datastore.core.DataStore] 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class OnboardingProgressDataStore
