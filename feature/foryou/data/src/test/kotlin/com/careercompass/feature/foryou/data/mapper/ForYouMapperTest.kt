package com.careercompass.feature.foryou.data.mapper

import com.careercompass.core.network.dto.ForYouFeedDto
import com.careercompass.core.network.dto.ForYouPickDto
import com.careercompass.core.network.dto.ForYouTopPickDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 스키마가 어긋난 `reason` 두 모양이 도메인에서 하나로 모이는지 — API_SPEC v0.1 §7. */
class ForYouMapperTest {
    @Test
    fun `배열 이유와 문자열 이유가 같은 목록 타입으로 모인다`() {
        val recommendations =
            ForYouMapper.toRecommendations(
                ForYouFeedDto(
                    topPick = ForYouTopPickDto(postingId = 101, reason = listOf("전공 적합", "마감 임박")),
                    byStrength = listOf(ForYouPickDto(postingId = 102, reason = "Kotlin 경험이 많아요")),
                    byGap = listOf(ForYouPickDto(postingId = 103, reason = "어학·인턴 보완용")),
                ),
            )

        assertEquals(listOf("전공 적합", "마감 임박"), recommendations.topPick?.reasons)
        assertEquals(listOf("Kotlin 경험이 많아요"), recommendations.byStrength.single().reasons)
        assertEquals(listOf("어학·인턴 보완용"), recommendations.byGap.single().reasons)
        assertEquals(101L, recommendations.topPick?.postingId)
    }

    @Test
    fun `톱 픽이 없으면 없는 채로 둔다`() {
        val recommendations =
            ForYouMapper.toRecommendations(ForYouFeedDto(topPick = null, byStrength = emptyList(), byGap = emptyList()))

        assertNull(recommendations.topPick)
        assertTrue(recommendations.isEmpty)
    }

    @Test
    fun `빈 이유는 버리고 공고는 남긴다`() {
        val recommendations =
            ForYouMapper.toRecommendations(
                ForYouFeedDto(
                    topPick = ForYouTopPickDto(postingId = 101, reason = listOf(" ", "전공 적합 ")),
                    byStrength = listOf(ForYouPickDto(postingId = 102, reason = "  ")),
                    byGap = emptyList(),
                ),
            )

        assertEquals(listOf("전공 적합"), recommendations.topPick?.reasons)
        assertEquals(102L, recommendations.byStrength.single().postingId)
        assertTrue(
            recommendations.byStrength
                .single()
                .reasons
                .isEmpty(),
        )
    }
}
