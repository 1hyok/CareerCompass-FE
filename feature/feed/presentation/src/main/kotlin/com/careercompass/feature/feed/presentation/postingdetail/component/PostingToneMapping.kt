package com.careercompass.feature.feed.presentation.postingdetail.component

import androidx.compose.ui.graphics.Color
import com.careercompass.core.model.posting.SuitabilityLabel
import com.careercompass.core.ui.component.CareerCompassBadgeTone
import com.careercompass.core.ui.theme.CareerCompassColors
import com.careercompass.feature.feed.presentation.FeedListingCategory

/** Badge tone per listing category, matching the feed listing card. */
internal fun FeedListingCategory.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        FeedListingCategory.All,
        FeedListingCategory.Employment,
        -> CareerCompassBadgeTone.Brand

        FeedListingCategory.Scholarship -> CareerCompassBadgeTone.Info

        FeedListingCategory.Contest -> CareerCompassBadgeTone.Warning

        FeedListingCategory.ExternalActivity,
        FeedListingCategory.Other,
        -> CareerCompassBadgeTone.Neutral
    }

/**
 * 적합도 레이블의 배지 톤 — **네 레이블에 네 톤**(이슈 #200).
 *
 * 게이지 색([gaugeColor])과 **같은 표**를 쓴다. 배지와 막대가 나란히 있는데 한쪽만 색을 갈면 같은 점수를
 * 두고 두 가지 말을 하는 셈이 된다.
 */
internal fun SuitabilityLabel.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        SuitabilityLabel.VerySuitable -> CareerCompassBadgeTone.Brand
        SuitabilityLabel.Suitable -> CareerCompassBadgeTone.Info
        SuitabilityLabel.Neutral -> CareerCompassBadgeTone.Warning
        SuitabilityLabel.Low -> CareerCompassBadgeTone.Neutral
    }

/**
 * 점수 게이지 막대의 색 — **구간이 곧 F3-2 의 레이블 구간이다**(이슈 #200).
 *
 * 예전에는 막대가 점수와 무관하게 언제나 `primary` 한 색이었다. 색이 아무것도 말하지 않으니 틀릴 일도
 * 없었지만, 40점과 95점이 같은 초록으로 차오르는 화면이었다. 구간을 넣으면서 **경계를 새로 만들지 않고**
 * 도메인의 [SuitabilityLabel] 을 그대로 받는다 — 색이 갈리는 지점과 배지에 적히는 이름이 어긋날 수 있는
 * 자리를 아예 없앤다.
 *
 * 역할 색만 쓰고 새 토큰을 만들지 않는다. `success` 는 `primary` 와 같은 brand500 이라 「매우 적합」이
 * 여태 쓰던 초록 그대로이고, 아래로 info(파랑) → warning(주황) → mutedContent(회색)로 내려간다.
 *
 * **색만으로 구분되게 두지 않는다** — 같은 줄의 레이블 배지가 「매우 적합」·「적합」·「보통」·「낮음」을
 * 글자로 적고, 접근성 문구(`feed_posting_detail_suitability_content_description`)에도 점수와 함께 실린다.
 * 색은 훑어볼 때를 돕는 덧표시일 뿐이다.
 */
internal fun SuitabilityLabel.gaugeColor(colors: CareerCompassColors): Color =
    when (this) {
        SuitabilityLabel.VerySuitable -> colors.success
        SuitabilityLabel.Suitable -> colors.info
        SuitabilityLabel.Neutral -> colors.warning
        SuitabilityLabel.Low -> colors.mutedContent
    }
