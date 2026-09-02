package com.cambridge.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
public class CareerCompassStateViewTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun networkError_rendersCopyAndHidesUnavailableOfflineAction() {
        setStateContent {
            CareerCompassNetworkErrorState(
                onRetryClick = {},
                onOfflineClick = null,
            )
        }

        composeRule.onNodeWithText("연결할 수 없어요").assertExists()
        composeRule
            .onNodeWithText("인터넷 연결을 확인하고 다시 시도해 주세요")
            .assertExists()
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).assertExists()
        composeRule.onNodeWithText("오프라인 모드로 보기").assertDoesNotExist()
    }

    @Test
    public fun networkError_actionsInvokeTheirCallbacks() {
        val actions = mutableListOf<String>()

        setStateContent {
            CareerCompassNetworkErrorState(
                onRetryClick = { actions += "retry" },
                onOfflineClick = { actions += "offline" },
            )
        }

        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()
        composeRule.onNode(hasText("오프라인 모드로 보기") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("retry", "offline"), actions)
        }
    }

    @Test
    public fun analyzingState_determinateProgressRendersProgressAndLabel() {
        setStateContent {
            CareerCompassAnalyzingState(
                title = "AI가 분석 중이에요",
                description = "당신의 경험 카드와 공고를 비교하고 있어요",
                progress = 0.75f,
                progressLabel = "3/4 단계 · 약 10초 남음",
            )
        }

        composeRule.onNodeWithText("AI가 분석 중이에요").assertExists()
        composeRule
            .onNodeWithText("당신의 경험 카드와 공고를 비교하고 있어요")
            .assertExists()
        composeRule.onNodeWithText("3/4 단계 · 약 10초 남음").assertExists()
        composeRule
            .onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        current = 0.75f,
                        range = 0f..1f,
                        steps = 0,
                    ),
                ),
            ).assertCountEquals(2)
    }

    @Test
    public fun analyzingState_indeterminateProgressRendersOneIndicator() {
        setStateContent {
            CareerCompassAnalyzingState(
                title = "분석 중",
                description = "잠시만 기다려 주세요",
                progress = null,
                progressLabel = null,
            )
        }

        composeRule
            .onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertCountEquals(1)
    }

    @Test
    public fun analyzingState_labelWithoutProgressRendersLabelAndIndeterminateIndicator() {
        setStateContent {
            CareerCompassAnalyzingState(
                title = "분석 중",
                description = "잠시만 기다려 주세요",
                progress = null,
                progressLabel = "약 10초 남음",
            )
        }

        composeRule.onNodeWithText("약 10초 남음").assertExists()
        composeRule
            .onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertCountEquals(1)
    }

    @Test
    public fun analyzingState_blankProgressLabelIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setStateContent {
                    CareerCompassAnalyzingState(
                        title = "분석 중",
                        description = "잠시만 기다려 주세요",
                        progress = 0.5f,
                        progressLabel = " \t\n",
                    )
                }
            }

        assertEquals("progressLabel must be null or non-blank", error.message)
    }

    @Test
    public fun analyzingState_invalidProgressIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setStateContent {
                    CareerCompassAnalyzingState(
                        title = "분석 중",
                        description = "잠시만 기다려 주세요",
                        progress = 1.1f,
                        progressLabel = "완료 임박",
                    )
                }
            }

        assertEquals("progress must be null or between 0 and 1", error.message)
    }

    @Test
    public fun emptyState_optionalActionControlsRenderingAndInvokesCallback() {
        var clickCount = 0

        setStateContent {
            CareerCompassEmptyState(
                title = "결과가 없어요",
                description = "관심 분야를 추가해 보세요",
                actionText = "관심 분야 추가",
                onActionClick = { clickCount += 1 },
            )
        }

        composeRule.onNodeWithText("결과가 없어요").assertExists()
        composeRule.onNodeWithText("관심 분야를 추가해 보세요").assertExists()
        composeRule.onNode(hasText("관심 분야 추가") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(1, clickCount) }
    }

    @Test
    public fun emptyState_nullActionDoesNotExposeClickSemantics() {
        setStateContent {
            CareerCompassEmptyState(
                title = "결과가 없어요",
                description = "검색 조건을 바꿔 보세요",
                actionText = null,
                onActionClick = null,
            )
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    public fun emptyState_mismatchedActionArgumentsAreRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setStateContent {
                    CareerCompassEmptyState(
                        title = "결과가 없어요",
                        description = "검색 조건을 바꿔 보세요",
                        actionText = "다시 검색",
                        onActionClick = null,
                    )
                }
            }

        assertEquals(
            "actionText and onActionClick must both be null or both be non-null",
            error.message,
        )
    }

    @Test
    public fun permissionDenied_rendersBenefitsAndInvokesBothActions() {
        val actions = mutableListOf<String>()

        setStateContent {
            CareerCompassPermissionDeniedState(
                title = "알림 권한이 꺼져 있어요",
                description = "마감 알림과 새 공고를 받으려면 권한이 필요해요",
                benefits =
                    listOf(
                        "⏰ 마감 D-3, D-1 알림",
                        "✨ 적합도 80+ 신규 공고",
                    ),
                onOpenSettingsClick = { actions += "settings" },
                onLaterClick = { actions += "later" },
            )
        }

        composeRule.onNodeWithText("⏰ 마감 D-3, D-1 알림").assertExists()
        composeRule.onNodeWithText("✨ 적합도 80+ 신규 공고").assertExists()
        composeRule.onNode(hasText("설정에서 권한 켜기") and hasClickAction()).performClick()
        composeRule.onNode(hasText("나중에") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("settings", "later"), actions)
        }
    }

    @Test
    public fun permissionDenied_blankBenefitIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setStateContent {
                    CareerCompassPermissionDeniedState(
                        title = "알림 권한이 꺼져 있어요",
                        description = "권한이 필요해요",
                        benefits = listOf(" "),
                        onOpenSettingsClick = {},
                        onLaterClick = {},
                    )
                }
            }

        assertEquals("benefits must contain at least one non-blank item", error.message)
    }

    @Test
    public fun maintenanceState_rendersStatusAndContactAndInvokesRefresh() {
        var refreshCount = 0

        setStateContent {
            CareerCompassMaintenanceState(
                title = "서비스가 잠시 점검 중이에요",
                description = "AI 분석 서버를 업데이트하고 있어요",
                statusLabel = "● 점검 진행 중",
                onRefreshClick = { refreshCount += 1 },
                contactLabel = "문의 · help@careercompass.app",
            )
        }

        composeRule.onNodeWithText("● 점검 진행 중").assertExists()
        composeRule.onNodeWithText("문의 · help@careercompass.app").assertExists()
        composeRule.onNode(hasText("새로고침") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(1, refreshCount) }
    }

    @Test
    public fun maintenanceState_nullContactHidesContactLabel() {
        setStateContent {
            CareerCompassMaintenanceState(
                title = "서비스가 잠시 점검 중이에요",
                description = "AI 분석 서버를 업데이트하고 있어요",
                statusLabel = "● 점검 진행 중",
                onRefreshClick = {},
                contactLabel = null,
            )
        }

        composeRule.onNodeWithText("문의 · help@careercompass.app").assertDoesNotExist()
    }

    @Test
    public fun blankTitleIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setStateContent {
                    CareerCompassEmptyState(
                        title = " \t\n",
                        description = "검색 조건을 바꿔 보세요",
                        actionText = null,
                        onActionClick = null,
                    )
                }
            }

        assertEquals("title must not be blank", error.message)
    }

    @Test
    public fun largeFontScale_keepsNetworkActionsVisible() {
        setStateContent(fontScale = 2f) {
            CareerCompassNetworkErrorState(
                onRetryClick = {},
                onOfflineClick = {},
            )
        }

        composeRule
            .onNode(hasText("다시 시도") and hasClickAction())
            .assertIsDisplayed()
        composeRule
            .onNode(hasText("오프라인 모드로 보기") and hasClickAction())
            .assertIsDisplayed()
    }

    private fun setStateContent(
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme(content = content)
            }
        }
    }
}
