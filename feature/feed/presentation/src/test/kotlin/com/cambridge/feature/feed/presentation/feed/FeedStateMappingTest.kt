package com.cambridge.feature.feed.presentation.feed

import android.content.res.Resources
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.FeedContentState
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import com.cambridge.feature.feed.presentation.posting
import com.cambridge.feature.feed.presentation.profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 상태 조합(프로필 미입력·입력됨·모름 × 점수 있음·없음)이 화면 계약으로 어떻게 옮겨지는지. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedStateMappingTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().resources

    private val emptyProfile: UserProfile = profile(jobInterests = emptyList(), tags = emptyList())

    private fun uiState(profile: UserProfile?) =
        FeedViewState(
            profile = profile,
            postings = listOf(posting(id = 1), posting(id = 2, score = 88)),
            loadState = FeedLoadState.Loaded,
        ).toFeedUiState(resources, FIXED_CLOCK)

    private fun suitabilities(profile: UserProfile?): List<FeedSuitabilityState> {
        val content = uiState(profile).content
        return (content as FeedContentState.Loaded).listings.map(FeedListingUiModel::suitability)
    }

    @Test
    fun `프로필이 비면 점수 없는 카드만 프로필 미입력이고 안내가 켜진다`() {
        assertEquals(
            listOf(FeedSuitabilityState.ProfileIncomplete, FeedSuitabilityState.Scored(88)),
            suitabilities(emptyProfile),
        )
        assertTrue(uiState(emptyProfile).isProfileNoticeVisible)
    }

    @Test
    fun `프로필이 채워져 있으면 점수 없는 카드는 분석 중이다`() {
        assertEquals(
            listOf(FeedSuitabilityState.Analyzing, FeedSuitabilityState.Scored(88)),
            suitabilities(profile()),
        )
        assertFalse(uiState(profile()).isProfileNoticeVisible)
    }

    @Test
    fun `프로필을 모르면 점수 없는 카드는 분석 중이고 안내도 없다`() {
        assertEquals(
            listOf(FeedSuitabilityState.Analyzing, FeedSuitabilityState.Scored(88)),
            suitabilities(null),
        )
        assertFalse(uiState(null).isProfileNoticeVisible)
    }

    @Test
    fun `사용자 이름은 프로필에서 오고 없으면 기본 호칭이다`() {
        assertEquals("일혁", uiState(profile()).userName)
        assertEquals("회원", uiState(profile(name = null)).userName)
        assertEquals("회원", uiState(null).userName)
    }
}
