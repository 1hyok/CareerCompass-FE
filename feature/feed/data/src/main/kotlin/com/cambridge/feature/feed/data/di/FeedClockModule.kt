package com.cambridge.feature.feed.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * [Clock] 바인딩의 **단일 정본**.
 *
 * 피드 use case(마감일 필터·오늘 신규 개수)가 「오늘」을 주입된 [Clock] 으로 읽는다. 다른 feature 가
 * [Clock] 을 또 제공하면 Hilt 중복 바인딩으로 깨지므로, 시계가 필요한 모듈은 여기 것을 그대로 쓴다.
 * 테스트는 `Clock.fixed(...)` 를 생성자에 직접 넘긴다.
 */
@Module
@InstallIn(SingletonComponent::class)
public object FeedClockModule {
    @Provides
    @Singleton
    public fun provideClock(): Clock = Clock.systemDefaultZone()
}
