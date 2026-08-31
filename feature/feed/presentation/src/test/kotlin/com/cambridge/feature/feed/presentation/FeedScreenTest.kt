package com.cambridge.feature.feed.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressCopyWithoutListings() {
        composeRule.setFeedContent(state = sampleState(content = FeedContentState.Loading))

        composeRule.onNodeWithText("공고를 불러오는 중이에요").assertIsDisplayed()
        composeRule.onAllNodesWithText(SAMPLE_LISTING_TITLE).assertCountEquals(0)
    }

    @Test
    fun emptyState_showsEmptyGuidanceWithoutListings() {
        composeRule.setFeedContent(
            state =
                sampleState(
                    totalListingCount = 0,
                    content = FeedContentState.Empty,
                ),
        )

        composeRule.onNodeWithText("조건에 맞는 공고가 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("검색어나 필터를 바꿔 보세요").assertIsDisplayed()
        composeRule.onAllNodesWithText(SAMPLE_LISTING_TITLE).assertCountEquals(0)
    }

    @Test
    fun selectedFilter_exposesCheckedAccessibilityState() {
        composeRule.setFeedContent(state = sampleState())

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
        composeRule
            .onNodeWithText("전체")
            .assertIsOn()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton,
                ),
            )
        composeRule
            .onNode(hasText("채용") and hasStateDescription("선택 안 됨"))
            .assertIsOff()
    }

    @Test
    fun longUserNameAtLargeFontScale_keepsNotificationActionVisible() {
        composeRule.setFeedContent(
            state = sampleState(userName = "아주 길어서 한 줄에 전부 표시되지 않는 사용자 이름"),
            fontScale = 2f,
        )

        composeRule
            .onNodeWithContentDescription("알림 보기")
            .assertIsDisplayed()
    }

    @Test
    fun bookmark_exposesStateAndEmitsOnlyBookmarkIntent() {
        val events = mutableListOf<FeedUiEvent>()
        composeRule.setFeedContent(
            state = sampleState(listing = sampleListing(isBookmarked = true)),
            onEvent = events::add,
        )

        composeRule
            .onNodeWithContentDescription("$SAMPLE_LISTING_TITLE 북마크")
            .assertIsOn()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "저장됨",
                ),
            ).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(FeedUiEvent.BookmarkToggled(SAMPLE_LISTING_ID)), events)
        }
    }

    @Test
    fun searchFilterSortAndCard_emitSeparateIntents() {
        val events = mutableListOf<FeedUiEvent>()
        composeRule.setFeedContent(state = sampleState(), onEvent = events::add)

        composeRule
            .onNodeWithContentDescription("공고 검색")
            .performTextReplacement("Kotlin")
        composeRule
            .onNode(hasText("채용") and hasStateDescription("선택 안 됨"))
            .performClick()
        composeRule.onNodeWithContentDescription("정렬: 적합도 높은순").performClick()
        composeRule.onNodeWithText(SAMPLE_LISTING_TITLE).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    FeedUiEvent.SearchQueryChanged("Kotlin"),
                    FeedUiEvent.FilterSelected(FeedListingCategory.Employment),
                    FeedUiEvent.SortMenuRequested,
                    FeedUiEvent.ListingSelected(SAMPLE_LISTING_ID),
                ),
                events,
            )
        }
    }
}

private fun ComposeContentTestRule.setFeedContent(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit = {},
    fontScale: Float = 1f,
) {
    setContent {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(currentDensity.density, fontScale),
        ) {
            CareerCompassTheme {
                FeedScreen(state = state, onEvent = onEvent)
            }
        }
    }
}

private fun sampleState(
    userName: String = "일혁",
    totalListingCount: Int = 1,
    content: FeedContentState = FeedContentState.Loaded(listOf(sampleListing())),
    listing: FeedListingUiModel? = null,
): FeedUiState =
    FeedUiState(
        userName = userName,
        newListingCount = 12,
        searchQuery = "",
        filters =
            listOf(
                FeedFilterUiModel(FeedListingCategory.All, "전체"),
                FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
                FeedFilterUiModel(FeedListingCategory.Scholarship, "장학금"),
            ),
        selectedFilter = FeedListingCategory.All,
        selectedSort = FeedSortUiModel(id = "fit", label = "적합도 높은순"),
        totalListingCount = totalListingCount,
        content =
            listing?.let { FeedContentState.Loaded(listOf(it)) }
                ?: content,
    )

private fun sampleListing(isBookmarked: Boolean = false): FeedListingUiModel =
    FeedListingUiModel(
        id = SAMPLE_LISTING_ID,
        title = SAMPLE_LISTING_TITLE,
        category = FeedListingCategory.Employment,
        categoryLabel = "채용",
        sourceLabel = "공식 채용",
        suitabilityScore = 88,
        deadlineLabel = "D-7",
        isDeadlineUrgent = false,
        isNew = true,
        isBookmarked = isBookmarked,
    )

private const val SAMPLE_LISTING_ID = "listing-1"
private const val SAMPLE_LISTING_TITLE = "2026 카카오 SW 인턴십 모집"
