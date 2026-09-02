package com.cambridge.feature.feed.presentation.postingdetail

import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PostingDetailContractTest {
    @Test
    fun canCreateDraft_isAllowedOnlyForEmploymentAndScholarship() {
        samplePosting(category = FeedListingCategory.Employment, canCreateDraft = true)
        samplePosting(category = FeedListingCategory.Scholarship, canCreateDraft = true)
        samplePosting(category = FeedListingCategory.Contest, canCreateDraft = false)
        samplePosting(category = FeedListingCategory.ExternalActivity, canCreateDraft = false)

        listOf(FeedListingCategory.Contest, FeedListingCategory.ExternalActivity).forEach { category ->
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    samplePosting(category = category, canCreateDraft = true)
                }
            assertEquals(
                "canCreateDraft is only allowed for Employment or Scholarship postings",
                exception.message,
            )
        }
    }

    @Test
    fun posting_rejectsAllAsCategory() {
        assertThrows(IllegalArgumentException::class.java) {
            samplePosting(category = FeedListingCategory.All, canCreateDraft = false)
        }
    }

    @Test
    fun scores_mustStayWithinZeroToHundred() {
        listOf(-1, 101).forEach { score ->
            assertThrows(IllegalArgumentException::class.java) {
                sampleSuitability().copy(score = score)
            }
            assertThrows(IllegalArgumentException::class.java) {
                sampleAxis().copy(score = score)
            }
        }
        assertEquals(0, sampleSuitability().copy(score = 0).score)
        assertEquals(100, sampleAxis().copy(score = 100).score)
    }

    @Test
    fun similarPostings_areCappedAtThreeUniqueListings() {
        val threeListings = List(3) { index -> sampleListing(id = "similar-$index") }
        assertEquals(3, samplePosting(similarPostings = threeListings).similarPostings.size)

        assertThrows(IllegalArgumentException::class.java) {
            samplePosting(similarPostings = threeListings + sampleListing(id = "similar-3"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            samplePosting(
                similarPostings = listOf(sampleListing(id = "dup"), sampleListing(id = "dup")),
            )
        }
    }

    @Test
    fun breakdown_isCappedAtFourUniqueAxes() {
        val fourAxes = List(4) { index -> sampleAxis(label = "axis-$index") }
        assertEquals(4, sampleSuitability().copy(breakdown = fourAxes).breakdown.size)

        assertThrows(IllegalArgumentException::class.java) {
            sampleSuitability().copy(breakdown = fourAxes + sampleAxis(label = "axis-4"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            sampleSuitability().copy(
                breakdown = listOf(sampleAxis(label = "dup"), sampleAxis(label = "dup")),
            )
        }
    }

    @Test
    fun keywordsAndFormQuestions_rejectDuplicatesAndBlankEntries() {
        assertThrows(IllegalArgumentException::class.java) {
            samplePosting(keywords = listOf("Kotlin", "Kotlin"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            samplePosting(keywords = listOf("Kotlin", " "))
        }
        assertThrows(IllegalArgumentException::class.java) {
            samplePosting(
                formQuestions =
                    listOf(
                        PostingFormQuestionUiModel(order = 1, question = "지원 동기", maxCharsLabel = null),
                        PostingFormQuestionUiModel(order = 1, question = "성장 과정", maxCharsLabel = null),
                    ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PostingFormQuestionUiModel(order = 0, question = "지원 동기", maxCharsLabel = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PostingFormQuestionUiModel(order = 1, question = "지원 동기", maxCharsLabel = " ")
        }
    }

    @Test
    fun displayReadyModels_rejectBlankRequiredStrings() {
        val invalidFactories: List<Pair<String, () -> Any>> =
            listOf(
                "label must not be blank" to { sampleAxis(label = " ") },
                "weightLabel must not be blank" to { sampleAxis().copy(weightLabel = " ") },
                "levelLabel must not be blank" to { sampleSuitability().copy(levelLabel = " ") },
                "strengthComment must be null or non-blank" to {
                    sampleSuitability().copy(strengthComment = " ")
                },
                "id must not be blank" to { samplePosting().copy(id = " ") },
                "title must not be blank" to { samplePosting().copy(title = " ") },
                "sourceLabel must not be blank" to { samplePosting().copy(sourceLabel = " ") },
                "collectedAtLabel must not be blank" to { samplePosting().copy(collectedAtLabel = " ") },
                "deadlineLabel must not be blank" to { samplePosting().copy(deadlineLabel = " ") },
                "message must not be blank" to { PostingDetailContentState.Error(message = " ") },
            )

        invalidFactories.forEach { (expectedMessage, factory) ->
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    factory()
                }
            assertEquals(expectedMessage, exception.message)
        }
    }

    @Test
    fun suitabilityComments_acceptNull() {
        val suitability = sampleSuitability().copy(strengthComment = null, weaknessComment = null)

        assertEquals(null, suitability.strengthComment)
        assertEquals(null, suitability.weaknessComment)
    }
}

private fun sampleAxis(label: String = "분야 유사도"): SuitabilityAxisUiModel =
    SuitabilityAxisUiModel(label = label, score = 95, weightLabel = "40%")

private fun sampleSuitability(): SuitabilityUiModel =
    SuitabilityUiModel(
        score = 88,
        levelLabel = "매우 적합",
        level = CareerCompassScoreLevel.High,
        breakdown = listOf(sampleAxis()),
        strengthComment = "강점",
        weaknessComment = "약점",
    )

private fun sampleListing(id: String): FeedListingUiModel =
    FeedListingUiModel(
        id = id,
        title = "유사 공고",
        category = FeedListingCategory.Employment,
        categoryLabel = "채용",
        sourceLabel = "공식 채용",
        suitability = FeedSuitabilityState.Scored(70),
        deadlineLabel = "D-10",
        isDeadlineUrgent = false,
        isNew = false,
        isBookmarked = false,
    )

private fun samplePosting(
    category: FeedListingCategory = FeedListingCategory.Employment,
    canCreateDraft: Boolean = true,
    keywords: List<String> = listOf("Kotlin"),
    formQuestions: List<PostingFormQuestionUiModel> = emptyList(),
    similarPostings: List<FeedListingUiModel> = emptyList(),
): PostingDetailUiModel =
    PostingDetailUiModel(
        id = "posting-1",
        title = "2026 카카오 SW 인턴십",
        category = category,
        categoryLabel = "채용",
        sourceLabel = "공식 채용",
        collectedAtLabel = "2시간 전",
        deadlineLabel = "2026.05.25",
        isDeadlineUrgent = false,
        isBookmarked = false,
        suitability = PostingSuitabilityState.Ready(sampleSuitability()),
        keywords = keywords,
        qualifications = emptyList(),
        preferences = emptyList(),
        formQuestions = formQuestions,
        similarPostings = similarPostings,
        canCreateDraft = canCreateDraft,
    )
