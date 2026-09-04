package com.careercompass.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.careercompass.core.ui.failure.FailureAction
import com.careercompass.core.ui.failure.FailureDisplay
import com.careercompass.core.ui.failure.FailureKind
import com.careercompass.core.ui.failure.FailureSurface
import com.careercompass.core.ui.failure.display
import com.careercompass.core.ui.theme.CareerCompassTheme
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

    /**
     * 카드 안 표현(#221) — 문구와 인디케이터는 그대로 나오되, **아래 내용을 화면 밖으로 밀어내지 않는다.**
     * 화면 한 장 뼈대(`fillMaxSize`)를 그대로 쓰면 뒤따르는 내용이 보이지 않게 된다.
     */
    @Test
    public fun analyzingState_inlinePresentationLeavesRoomForContentBelow() {
        setStateContent {
            Column {
                CareerCompassAnalyzingState(
                    title = "AI가 분석 중이에요",
                    description = "적합도가 나오면 이 자리에 바로 보여 드릴게요",
                    progress = null,
                    progressLabel = null,
                    presentation = CareerCompassStatePresentation.Inline,
                )
                Text(text = "아래 내용", modifier = Modifier.testTag("below"))
            }
        }

        composeRule.onNodeWithText("AI가 분석 중이에요").assertIsDisplayed()
        composeRule.onNodeWithText("적합도가 나오면 이 자리에 바로 보여 드릴게요").assertIsDisplayed()
        composeRule
            .onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertCountEquals(1)
        composeRule.onNodeWithTag("below").assertIsDisplayed()
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
    public fun failureState_rendersCopyAndInvokesAction() {
        var clickCount = 0

        setStateContent {
            CareerCompassFailureState(
                title = "문제가 생겼어요",
                description = "잠시 후 다시 시도해 주세요",
                actionText = "다시 시도",
                onActionClick = { clickCount++ },
            )
        }

        composeRule.onNodeWithText("문제가 생겼어요").assertExists()
        composeRule.onNodeWithText("잠시 후 다시 시도해 주세요").assertExists()
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }

    /** 재시도해도 답이 갈리지 않는 실패 — 호출자가 버튼을 빼면 눌러도 소용없는 버튼이 생기지 않는다(#222). */
    @Test
    public fun failureState_withoutActionRendersNoButton() {
        setStateContent {
            CareerCompassFailureState(
                title = "더 담을 수 없어요",
                description = "게시판은 최대 20개까지 등록할 수 있어요",
                actionText = null,
                onActionClick = null,
            )
        }

        composeRule.onNodeWithText("더 담을 수 없어요").assertExists()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    public fun failureState_halfSuppliedActionIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setStateContent {
                    CareerCompassFailureState(
                        title = "문제가 생겼어요",
                        description = "잠시 후 다시 시도해 주세요",
                        actionText = "다시 시도",
                        onActionClick = null,
                    )
                }
            }

        assertEquals("actionText and onActionClick must both be null or both be non-null", error.message)
    }

    /** 표의 행을 그대로 — `Unexpected` 는 재시도가 붙고, 문맥(`Posting`)이 명사를 채운다. */
    @Test
    public fun failureState_fromTableRowRendersRowCopyAndRetry() {
        val actions = mutableListOf<String>()

        setStateContent {
            CareerCompassFailureState(
                display = FailureKind.Unexpected.display(FailureSurface.Posting),
                onActionClick = { actions += "retry" },
            )
        }

        composeRule.onNodeWithText("공고를 불러오지 못했어요").assertExists()
        composeRule.onNodeWithText("잠시 후 다시 시도해 주세요").assertExists()
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("retry"), actions)
        }
    }

    /**
     * 표가 「할 수 있는 일이 없다」고 하면 호출자가 콜백을 넘겨도 버튼을 그리지 않는다 — 판정은 표가 갖고
     * 화면은 그것을 뒤집지 못한다(#204·#222).
     */
    @Test
    public fun failureState_tableRowWithoutActionIgnoresCallback() {
        setStateContent {
            CareerCompassFailureState(
                display = FailureKind.ParsingFailed.display(),
                onActionClick = {},
            )
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    /** 표에 행동이 있어도 화면이 그 길을 못 열면(콜백 없음) 버튼이 없다. */
    @Test
    public fun failureState_tableRowWithActionButNoCallbackRendersNoButton() {
        val display =
            FailureDisplay(
                titleRes = android.R.string.dialog_alert_title,
                descriptionRes = android.R.string.unknownName,
                action = FailureAction.CompleteProfile,
            )

        setStateContent {
            CareerCompassFailureState(display = display, onActionClick = null)
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
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
                onOfflineClick = null,
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
                onOfflineClick = null,
                contactLabel = null,
            )
        }

        composeRule.onNodeWithText("문의 · help@careercompass.app").assertDoesNotExist()
        composeRule.onNodeWithText("오프라인 모드로 보기").assertDoesNotExist()
    }

    @Test
    public fun maintenanceState_offlineHandlerShowsOfflineAction() {
        var offlineCount = 0

        setStateContent {
            CareerCompassMaintenanceState(
                title = "서비스가 잠시 점검 중이에요",
                description = "AI 분석 서버를 업데이트하고 있어요",
                statusLabel = "● 점검 진행 중",
                onRefreshClick = {},
                onOfflineClick = { offlineCount += 1 },
                contactLabel = null,
            )
        }

        composeRule.onNode(hasText("오프라인 모드로 보기") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(1, offlineCount) }
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
