package com.careercompass.feature.feed.presentation.postingdetail

import com.careercompass.core.model.posting.SuitabilityLabel
import com.careercompass.core.ui.component.CareerCompassBadgeTone
import com.careercompass.feature.feed.domain.model.FeedQuery
import com.careercompass.feature.feed.presentation.feedfilter.FeedMinScoreFilter
import com.careercompass.feature.feed.presentation.postingdetail.component.badgeTone
import com.careercompass.feature.feed.presentation.shared.util.toMinScore
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 적합도 경계가 **한 벌**이라는 것을 지키는 테스트 — 이슈 #200.
 *
 * 이 파일의 기대값은 전부 **숫자로 적혀 있다.** 도메인 상수를 읽어 와 비교하면 상수가 틀어져도 양쪽이 같이
 * 움직여 초록으로 남는다 — 「무는 테스트」와 「초록인 테스트」가 갈리는 자리라 여기서만은 사본을 만든다.
 */
class SuitabilityBoundaryTest {
    /** 기능 스펙 F3-2 「점수 해석 레이블」 표 그대로. 경계값과 그 바로 아래를 함께 본다. */
    @Test
    fun `레이블 경계는 80·60·40 이다`() {
        assertEquals(SuitabilityLabel.Low, SuitabilityLabel.fromScore(0))
        assertEquals(SuitabilityLabel.Low, SuitabilityLabel.fromScore(39))
        assertEquals(SuitabilityLabel.Neutral, SuitabilityLabel.fromScore(40))
        assertEquals(SuitabilityLabel.Neutral, SuitabilityLabel.fromScore(59))
        assertEquals(SuitabilityLabel.Suitable, SuitabilityLabel.fromScore(60))
        assertEquals(SuitabilityLabel.Suitable, SuitabilityLabel.fromScore(79))
        assertEquals(SuitabilityLabel.VerySuitable, SuitabilityLabel.fromScore(80))
        assertEquals(SuitabilityLabel.VerySuitable, SuitabilityLabel.fromScore(100))
    }

    /**
     * 필터에서 **고를 수 있는 값**은 전부 레이블 경계다.
     *
     * 이것이 이 이슈의 완료 조건이다 — 「70점 이상」처럼 어떤 레이블의 경계도 아닌 값으로 거르면
     * 「적합」(60~79) 구간이 반으로 잘려 나오고, 왜 어떤 「적합」은 빠졌는지 화면이 설명할 말이 없다.
     */
    @Test
    fun `필터 선택지는 모두 레이블 경계다`() {
        assertEquals(setOf(60, 80), FeedQuery.ALLOWED_MIN_SCORES)
        assertEquals(null, FeedMinScoreFilter.All.toMinScore())
        assertEquals(60, FeedMinScoreFilter.AtLeast60.toMinScore())
        assertEquals(80, FeedMinScoreFilter.AtLeast80.toMinScore())

        val optionScores = FeedMinScoreFilter.entries.mapNotNull(FeedMinScoreFilter::toMinScore)
        assertEquals(FeedQuery.ALLOWED_MIN_SCORES, optionScores.toSet())
        optionScores.forEach { score ->
            // 고른 값 그 자체가 어떤 레이블의 시작점이어야 한다.
            assertEquals(score, SuitabilityLabel.fromScore(score).minScore)
        }
    }

    /** 4축 「충족」 경계도 같은 표에서 나온다(이슈 #141 의 판정). */
    @Test
    fun `축 충족 경계는 적합 경계와 같은 60 이다`() {
        assertEquals(60, SUITABILITY_AXIS_FULFILLED_THRESHOLD)
    }

    /**
     * 게이지 색·배지 톤이 **레이블마다 다르다** — 색 구간의 경계가 곧 레이블 구간의 경계다.
     *
     * 실제 색상값은 테마에 있어 여기서 못 읽으므로 톤이 넷 다 다른 것까지 본다. 「그 톤이 어떤 색인가」는
     * 스크린샷 골든의 몫이다.
     */
    @Test
    fun `레이블 넷은 각각 다른 배지 톤을 쓴다`() {
        assertEquals(CareerCompassBadgeTone.Brand, SuitabilityLabel.VerySuitable.badgeTone())
        assertEquals(CareerCompassBadgeTone.Info, SuitabilityLabel.Suitable.badgeTone())
        assertEquals(CareerCompassBadgeTone.Warning, SuitabilityLabel.Neutral.badgeTone())
        assertEquals(CareerCompassBadgeTone.Neutral, SuitabilityLabel.Low.badgeTone())

        val tones = SuitabilityLabel.entries.map(SuitabilityLabel::badgeTone)
        assertEquals(SuitabilityLabel.entries.size, tones.distinct().size)
    }
}
