package com.cambridge.feature.feed.presentation.feed

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedFailureContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun networkUnavailable_showsConnectionNotice() {
        composeRule.setFailureContent(reason = FeedFailureReason.NetworkUnavailable)

        composeRule.onNodeWithText("연결할 수 없어요").assertIsDisplayed()
        composeRule.onAllNodesWithText("점검 진행 중").assertCountEquals(0)
    }

    @Test
    fun maintenance_showsMaintenanceNoticeAndEmitsRetry() {
        var retryCount = 0
        composeRule.setFailureContent(
            reason = FeedFailureReason.Maintenance,
            onRetryClick = { retryCount += 1 },
        )

        composeRule.onNodeWithText("서비스가 잠시 점검 중이에요").assertIsDisplayed()
        composeRule.onNodeWithText("점검 진행 중").assertIsDisplayed()
        composeRule.onNode(hasText("새로고침") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }

    @Test
    fun maintenanceWithSnapshot_keepsOfflineRouteOpen() {
        var offlineCount = 0
        composeRule.setFailureContent(
            reason = FeedFailureReason.Maintenance,
            onOfflineClick = { offlineCount += 1 },
        )

        composeRule.onNode(hasText("오프라인 모드로 보기") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(1, offlineCount) }
    }

    @Test
    fun maintenanceWithoutSnapshot_hidesOfflineRoute() {
        composeRule.setFailureContent(reason = FeedFailureReason.Maintenance, onOfflineClick = null)

        composeRule.onAllNodesWithText("오프라인 모드로 보기").assertCountEquals(0)
    }

    @Test
    fun generic_showsRetryNoticeWithoutMaintenanceCopy() {
        var retryCount = 0
        composeRule.setFailureContent(
            reason = FeedFailureReason.Generic,
            onRetryClick = { retryCount += 1 },
        )

        composeRule.onNodeWithText("공고를 불러오지 못했어요").assertIsDisplayed()
        composeRule.onAllNodesWithText("점검 진행 중").assertCountEquals(0)
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }
}

private fun ComposeContentTestRule.setFailureContent(
    reason: FeedFailureReason,
    onRetryClick: () -> Unit = {},
    onOfflineClick: (() -> Unit)? = null,
) {
    setContent {
        CareerCompassTheme {
            FeedFailureContent(
                reason = reason,
                onRetryClick = onRetryClick,
                onOfflineClick = onOfflineClick,
            )
        }
    }
}
