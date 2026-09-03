package com.cambridge.feature.feed.presentation.postingdetail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostingDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun employmentPosting_showsDraftButtonNextToViewOriginal() {
        composeRule.setDetailContent(state = loadedState())

        composeRule.onNode(hasText("원문 보기") and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasText("지원서 초안 작성하기") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun contestPosting_hidesDraftButton() {
        composeRule.setDetailContent(
            state =
                loadedState(
                    posting =
                        samplePosting(
                            category = FeedListingCategory.Contest,
                            categoryLabel = "공모전",
                            canCreateDraft = false,
                        ),
                ),
        )

        composeRule.onNode(hasText("원문 보기") and hasClickAction()).assertIsDisplayed()
        composeRule.onAllNodesWithText("지원서 초안 작성하기").assertCountEquals(0)
    }

    @Test
    fun profileIncomplete_showsGuidanceAndEmitsCompleteProfile() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(
            state =
                loadedState(
                    posting = samplePosting(suitability = PostingSuitabilityState.ProfileIncomplete),
                ),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("프로필을 입력하면 적합도를 확인할 수 있어요").assertIsDisplayed()
        composeRule.onAllNodesWithText("88").assertCountEquals(0)
        composeRule.onNode(hasText("프로필 입력하기") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PostingDetailEvent.CompleteProfileClicked), events)
        }
    }

    @Test
    fun analyzing_showsProgressCopyWithoutScore() {
        composeRule.setDetailContent(
            state = loadedState(posting = samplePosting(suitability = PostingSuitabilityState.Analyzing)),
        )

        composeRule.onNodeWithText("AI가 분석 중이에요").assertIsDisplayed()
        composeRule.onAllNodesWithText("88").assertCountEquals(0)
    }

    @Test
    fun readySuitability_exposesGaugeProgressAndBreakdown() {
        composeRule.setDetailContent(state = loadedState())

        val gauge = composeRule.onNodeWithContentDescription("적합도 88점, 매우 적합").assertExists()
        val rangeInfo = gauge.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0.88f, rangeInfo.current, 0.001f)
        assertEquals(0f..1f, rangeInfo.range)
        composeRule
            .onNode(hasText("88") and SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("매우 적합").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("분야 유사도 가중치 40%, 95점, 충족").assertExists()
        composeRule.onNodeWithText(STRENGTH_COMMENT).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(WEAKNESS_COMMENT).performScrollTo().assertIsDisplayed()
    }

    /**
     * 충족 여부는 막대 색이 아니라 글자로도 있어야 한다(F3-3) — 색각 이상·흑백 환경에서
     * 색만 남으면 아무 정보도 아니다. 접근성 문구에도 같은 말이 실린다.
     */
    @Test
    fun breakdownAxes_labelFulfillmentInTextAndContentDescription() {
        composeRule.setDetailContent(
            state =
                loadedState(
                    posting =
                        samplePosting(
                            suitability =
                                PostingSuitabilityState.Ready(
                                    sampleSuitability().copy(
                                        breakdown =
                                            listOf(
                                                SuitabilityAxisUiModel(label = "분야 유사도", score = 60, weightLabel = "40%"),
                                                SuitabilityAxisUiModel(label = "경쟁 강도", score = 59, weightLabel = "10%"),
                                            ),
                                    ),
                                ),
                        ),
                ),
        )

        composeRule.onNodeWithContentDescription("분야 유사도 가중치 40%, 60점, 충족").assertExists()
        composeRule.onNodeWithContentDescription("경쟁 강도 가중치 10%, 59점, 미충족").assertExists()
        composeRule.onNodeWithText("충족", substring = false).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("미충족").performScrollTo().assertIsDisplayed()
    }

    /** 축이 비어 오면 「모름」 이다 — 0점짜리 축 4개를 그려 「미충족」 으로 읽히게 두지 않는다. */
    @Test
    fun emptyBreakdown_saysScoresAreUnknownInsteadOfDrawingUnfulfilledAxes() {
        composeRule.setDetailContent(
            state =
                loadedState(
                    posting =
                        samplePosting(
                            suitability =
                                PostingSuitabilityState.Ready(sampleSuitability().copy(breakdown = emptyList())),
                        ),
                ),
        )

        composeRule.onNodeWithText("축별 세부 점수는 아직 없어요").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("미충족").assertCountEquals(0)
    }

    @Test
    fun bookmark_exposesCheckedStateAndEmitsToggle() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(
            state = loadedState(posting = samplePosting(isBookmarked = true)),
            onEvent = events::add,
        )

        composeRule
            .onNodeWithContentDescription("북마크")
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "저장됨"))
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PostingDetailEvent.BookmarkToggled), events)
        }
    }

    @Test
    fun unbookmarked_exposesUncheckedState() {
        composeRule.setDetailContent(state = loadedState())

        composeRule
            .onNodeWithContentDescription("북마크")
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "저장 안 됨"))
    }

    @Test
    fun loading_showsProgressCopyWithoutActions() {
        composeRule.setDetailContent(
            state = PostingDetailUiState(content = PostingDetailContentState.Loading),
        )

        composeRule.onNodeWithText("공고를 불러오는 중이에요").assertIsDisplayed()
        composeRule.onAllNodesWithText("원문 보기").assertCountEquals(0)
    }

    @Test
    fun error_showsMessageAndEmitsRetry() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(
            state =
                PostingDetailUiState(
                    content = PostingDetailContentState.Error(message = "공고를 불러오지 못했어요"),
                ),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("공고를 불러오지 못했어요").assertIsDisplayed()
        composeRule.onNode(hasText("다시 시도") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PostingDetailEvent.RetryClicked), events)
        }
    }

    @Test
    fun maintenance_showsMaintenanceNoticeWithoutOfflineActionAndEmitsRetry() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(
            state = PostingDetailUiState(content = PostingDetailContentState.Maintenance),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("서비스가 잠시 점검 중이에요").assertIsDisplayed()
        composeRule.onNodeWithText("점검 진행 중").assertIsDisplayed()
        // 상세는 스냅샷이 없어 오프라인 경로를 열지 않는다.
        composeRule.onAllNodesWithText("오프라인 모드로 보기").assertCountEquals(0)
        composeRule.onNode(hasText("새로고침") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PostingDetailEvent.RetryClicked), events)
        }
    }

    @Test
    fun similarPosting_emitsSelectionWithListingId() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(state = loadedState(), onEvent = events::add)

        composeRule.onNodeWithText(SIMILAR_TITLE).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PostingDetailEvent.SimilarPostingSelected(SIMILAR_ID)), events)
        }
    }

    /**
     * 읽음은 목록 카드와 **같은 규칙**으로 갈린다(#140·#165) — 문구(「읽음」)와 형태(체크)가 정보를 지고
     * 색은 거들 뿐이다. 배지 자체는 스크린 리더에서 지워져 있으므로 상태는 카드에서 읽혀야 한다.
     */
    @Test
    fun readSimilarPosting_marksTheCardWithWordsAndAnAccessibilityState() {
        composeRule.setDetailContent(state = loadedState(posting = samplePosting(similarIsRead = true)))

        // 배지는 clearAndSetSemantics 로 지워져 병합 트리에 없다 — 그려졌는지는 unmerged 로 본다.
        composeRule.onNodeWithText("읽음", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeRule
            .onNode(hasText(SIMILAR_TITLE) and hasStateDescription("읽음"))
            .assertExists()
    }

    /** 표시가 없는 쪽도 침묵하지 않는다 — 읽지 않은 유사 공고는 접근성 상태로 그렇게 말한다. */
    @Test
    fun unreadSimilarPosting_saysSoInsteadOfLeavingTheStateUnspoken() {
        composeRule.setDetailContent(state = loadedState())

        composeRule
            .onNode(hasText(SIMILAR_TITLE) and hasStateDescription("읽지 않음"))
            .assertExists()
        composeRule.onAllNodesWithText("읽음", useUnmergedTree = true).assertCountEquals(0)
    }

    /**
     * 유사 공고 카드는 수집일을 싣지 않는다 — 「다음에 뭘 열까」를 가르는 것은 마감일과 읽음이고,
     * 목록 카드에서 수집일을 넣게 만든 초록 점이 이 카드엔 애초에 없다(근거는 `SimilarPostingCard`).
     */
    @Test
    fun similarPosting_leavesCollectedAtToTheFeedListAndKeepsTheDeadline() {
        composeRule.setDetailContent(state = loadedState(posting = samplePosting(similarIsRead = true)))

        composeRule.onNodeWithText("D-14").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText(SIMILAR_COLLECTED_LABEL).assertCountEquals(0)
    }

    /** 큰 글꼴에서 메타 줄이 접힐 뿐, 마감일도 읽음 배지도 잘려 사라지지 않는다. */
    @Test
    fun similarPosting_keepsDeadlineAndReadBadgeAtLargeFontScale() {
        composeRule.setDetailContent(
            state = loadedState(posting = samplePosting(similarIsRead = true)),
            fontScale = 2f,
        )

        composeRule.onNodeWithText("D-14").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("읽음", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun chromeAndFooter_emitSeparateIntents() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(state = loadedState(), onEvent = events::add)

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.onNodeWithContentDescription("공유하기").performClick()
        composeRule.onNode(hasText("원문 보기") and hasClickAction()).performClick()
        composeRule.onNode(hasText("지원서 초안 작성하기") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    PostingDetailEvent.BackClicked,
                    PostingDetailEvent.ShareClicked,
                    PostingDetailEvent.ViewOriginalClicked,
                    PostingDetailEvent.CreateDraftClicked,
                ),
                events,
            )
        }
    }

    @Test
    fun sections_renderKeywordsBulletsAndFormQuestions() {
        composeRule.setDetailContent(state = loadedState())

        composeRule.onNodeWithText("Kotlin").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("4년제 대학 재학생").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Spring 경험").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Q1. 지원 동기를 작성해 주세요").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("최대 1,000자").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyFormQuestions_showEmptyCaption() {
        composeRule.setDetailContent(
            state = loadedState(posting = samplePosting(formQuestions = emptyList())),
        )

        composeRule.onNodeWithText("자동 인식된 항목이 없어요").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun largeFontScale_keepsDraftButtonVisible() {
        composeRule.setDetailContent(state = loadedState(), fontScale = 2f)

        composeRule.onNode(hasText("지원서 초안 작성하기") and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasText("원문 보기") and hasClickAction()).assertIsDisplayed()
    }
}

private fun ComposeContentTestRule.setDetailContent(
    state: PostingDetailUiState,
    onEvent: (PostingDetailEvent) -> Unit = {},
    fontScale: Float = 1f,
) {
    setContent {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(currentDensity.density, fontScale),
        ) {
            CareerCompassTheme {
                PostingDetailScreen(state = state, onEvent = onEvent)
            }
        }
    }
}

private fun loadedState(posting: PostingDetailUiModel = samplePosting()): PostingDetailUiState =
    PostingDetailUiState(content = PostingDetailContentState.Loaded(posting))

private fun samplePosting(
    category: FeedListingCategory = FeedListingCategory.Employment,
    categoryLabel: String = "채용",
    canCreateDraft: Boolean = true,
    isBookmarked: Boolean = false,
    similarIsRead: Boolean = false,
    suitability: PostingSuitabilityState = PostingSuitabilityState.Ready(sampleSuitability()),
    formQuestions: List<PostingFormQuestionUiModel> =
        listOf(
            PostingFormQuestionUiModel(
                order = 1,
                question = "지원 동기를 작성해 주세요",
                maxCharsLabel = "최대 1,000자",
            ),
        ),
): PostingDetailUiModel =
    PostingDetailUiModel(
        id = "posting-1",
        title = "2026 카카오 SW 인턴십 (백엔드)",
        category = category,
        categoryLabel = categoryLabel,
        sourceLabel = "공식 채용",
        collectedAtLabel = "2시간 전",
        deadlineLabel = "2026.05.25",
        isDeadlineUrgent = false,
        isBookmarked = isBookmarked,
        suitability = suitability,
        keywords = listOf("Kotlin", "Spring"),
        qualifications = listOf("4년제 대학 재학생"),
        preferences = listOf("Spring 경험"),
        formQuestions = formQuestions,
        similarPostings =
            listOf(
                FeedListingUiModel(
                    id = SIMILAR_ID,
                    title = SIMILAR_TITLE,
                    category = FeedListingCategory.Employment,
                    categoryLabel = "채용",
                    sourceLabel = "네이버 채용",
                    suitability = FeedSuitabilityState.Scored(76),
                    deadlineLabel = "D-14",
                    isDeadlineUrgent = false,
                    collectedAtLabel = SIMILAR_COLLECTED_LABEL,
                    isNew = false,
                    isRead = similarIsRead,
                    isBookmarked = false,
                ),
            ),
        canCreateDraft = canCreateDraft,
    )

private fun sampleSuitability(): SuitabilityUiModel =
    SuitabilityUiModel(
        score = 88,
        levelLabel = "매우 적합",
        level = CareerCompassScoreLevel.High,
        breakdown =
            listOf(
                SuitabilityAxisUiModel(label = "분야 유사도", score = 95, weightLabel = "40%"),
                SuitabilityAxisUiModel(label = "자격 조건 충족도", score = 88, weightLabel = "30%"),
                SuitabilityAxisUiModel(label = "우대 조건 매칭", score = 78, weightLabel = "20%"),
                SuitabilityAxisUiModel(label = "경쟁 강도(역점)", score = 80, weightLabel = "10%"),
            ),
        strengthComment = STRENGTH_COMMENT,
        weaknessComment = WEAKNESS_COMMENT,
    )

private const val SIMILAR_ID = "boostcamp"
private const val SIMILAR_COLLECTED_LABEL = "수집 3일 전"
private const val SIMILAR_TITLE = "네이버 부스트캠프 9기 모집"
private const val STRENGTH_COMMENT = "Spring-JPA 프로젝트 경험이 우대 조건과 일치합니다"
private const val WEAKNESS_COMMENT = "어학 성적 정보가 프로필에 없습니다"
