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
        composeRule.onNodeWithContentDescription("분야 유사도 가중치 40%, 95점").assertExists()
        composeRule.onNodeWithText(STRENGTH_COMMENT).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(WEAKNESS_COMMENT).performScrollTo().assertIsDisplayed()
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
    fun similarPosting_emitsSelectionWithListingId() {
        val events = mutableListOf<PostingDetailEvent>()
        composeRule.setDetailContent(state = loadedState(), onEvent = events::add)

        composeRule.onNodeWithText(SIMILAR_TITLE).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PostingDetailEvent.SimilarPostingSelected(SIMILAR_ID)), events)
        }
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
                    isNew = false,
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
private const val SIMILAR_TITLE = "네이버 부스트캠프 9기 모집"
private const val STRENGTH_COMMENT = "Spring-JPA 프로젝트 경험이 우대 조건과 일치합니다"
private const val WEAKNESS_COMMENT = "어학 성적 정보가 프로필에 없습니다"
