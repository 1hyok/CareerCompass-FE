package com.cambridge.feature.feed.presentation.postingdetail

import androidx.lifecycle.SavedStateHandle
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakePostingRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.posting.Suitability
import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.feed.domain.usecase.OpenPostingDetailUseCase
import com.cambridge.feature.feed.domain.usecase.TogglePostingBookmarkUseCase
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.navigation.FEED_ARG_POSTING_ID
import com.cambridge.feature.feed.presentation.postingDetail
import com.cambridge.feature.feed.presentation.profile
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.cambridge.feature.feed.presentation.shared.model.SuitabilityJudgement
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostingDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    private fun viewModel(
        repository: FakePostingRepository,
        profile: UserProfile? = profile(),
        postingId: Long = POSTING_ID,
    ): PostingDetailViewModel =
        PostingDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(FEED_ARG_POSTING_ID to postingId)),
            openPostingDetail = OpenPostingDetailUseCase(repository),
            togglePostingBookmark = TogglePostingBookmarkUseCase(repository),
            userProfileRepository = FakeUserProfileRepository(initialProfile = profile),
            errorReporter = reporter,
            clock = FIXED_CLOCK,
        )

    private fun repositoryWith(detail: PostingDetail) = FakePostingRepository(details = listOf(detail))

    @Test
    fun `상세를 열면 읽음 처리하고 읽은 상태로 보여 준다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID, isRead = false))

        val state = viewModel(repository).state.value

        assertEquals(listOf(POSTING_ID), repository.readCalls.toList())
        assertTrue(requireNotNull(state.detail).isRead)
    }

    @Test
    fun `직무·태그가 모두 비면 프로필 미입력이다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID))

        val state = viewModel(repository, profile = profile(jobInterests = emptyList(), tags = emptyList())).state.value

        assertEquals(SuitabilityJudgement.ProfileIncomplete, state.suitabilityJudgement)
    }

    @Test
    fun `프로필이 있어도 점수가 없으면 분석 중이다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID))

        assertEquals(SuitabilityJudgement.Analyzing, viewModel(repository).state.value.suitabilityJudgement)
        assertEquals(SuitabilityJudgement.Analyzing, viewModel(repository, profile = null).state.value.suitabilityJudgement)
    }

    @Test
    fun `점수가 있으면 프로필과 무관하게 준비됨이다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID, suitability = sampleSuitability()))

        val state = viewModel(repository, profile = profile(jobInterests = emptyList(), tags = emptyList())).state.value

        assertEquals(SuitabilityJudgement.Ready, state.suitabilityJudgement)
    }

    @Test
    fun `북마크는 먼저 뒤집고 실패하면 되돌린다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakePostingRepository(
                    details = listOf(postingDetail(id = POSTING_ID, isBookmarked = false)),
                    onSetBookmarked = { _, _ ->
                        gate.await()
                        Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException()))
                    },
                )
            val viewModel = viewModel(repository)

            viewModel.onEvent(PostingDetailEvent.BookmarkToggled)
            assertTrue(requireNotNull(viewModel.state.value.detail).isBookmarked)

            gate.complete(Unit)

            assertFalse(requireNotNull(viewModel.state.value.detail).isBookmarked)
            assertEquals(PostingDetailMessage.BookmarkFailed, viewModel.state.value.message)
            assertEquals(listOf("bookmark"), reporter.stages)
        }

    @Test
    fun `북마크 성공은 서버 값으로 확정한다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID, isBookmarked = false))
        val viewModel = viewModel(repository)

        viewModel.onEvent(PostingDetailEvent.BookmarkToggled)

        assertTrue(requireNotNull(viewModel.state.value.detail).isBookmarked)
        assertEquals(listOf(POSTING_ID to true), repository.bookmarkCalls.toList())
    }

    @Test
    fun `공유·원문·유사 공고·프로필·뒤로가기는 단발 신호로 올라간다`() {
        val viewModel = viewModel(repositoryWith(postingDetail(id = POSTING_ID)))

        viewModel.onEvent(PostingDetailEvent.ShareClicked)
        assertEquals(
            PostingShareRequest(title = "공고 $POSTING_ID", url = "https://example.com/postings/$POSTING_ID"),
            viewModel.state.value.shareRequest,
        )
        viewModel.onShareConsumed()
        assertNull(viewModel.state.value.shareRequest)

        viewModel.onEvent(PostingDetailEvent.ViewOriginalClicked)
        assertEquals(PostingDetailDestination.Raw(POSTING_ID), viewModel.state.value.pendingNavigation)
        viewModel.onEvent(PostingDetailEvent.SimilarPostingSelected("9"))
        assertEquals(PostingDetailDestination.Posting(9L), viewModel.state.value.pendingNavigation)
        viewModel.onEvent(PostingDetailEvent.CompleteProfileClicked)
        assertEquals(PostingDetailDestination.Profile, viewModel.state.value.pendingNavigation)
        viewModel.onEvent(PostingDetailEvent.BackClicked)
        assertEquals(PostingDetailDestination.Back, viewModel.state.value.pendingNavigation)
        viewModel.onNavigationConsumed()
        assertNull(viewModel.state.value.pendingNavigation)
    }

    @Test
    fun `초안 작성은 준비 중 안내로 끝난다`() {
        val viewModel = viewModel(repositoryWith(postingDetail(id = POSTING_ID)))

        viewModel.onEvent(PostingDetailEvent.CreateDraftClicked)

        assertEquals(PostingDetailMessage.DraftComingSoon, viewModel.state.value.message)
        assertNull(viewModel.state.value.pendingNavigation)
    }

    @Test
    fun `네트워크 단절과 서버 오류를 구분하고 다시 시도로 복구한다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID))
        repository.onGetPostingDetail = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
        val viewModel = viewModel(repository)

        assertEquals(PostingDetailLoadState.Failed(FeedFailureReason.NetworkUnavailable), viewModel.state.value.loadState)

        repository.onGetPostingDetail = null
        viewModel.onEvent(PostingDetailEvent.RetryClicked)

        assertTrue(viewModel.state.value.loadState is PostingDetailLoadState.Loaded)
    }

    @Test
    fun `서버 점검 503 은 점검 상태가 되고 다시 시도로 복구한다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID))
        repository.onGetPostingDetail = { Result.failure(CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", RuntimeException())) }
        val viewModel = viewModel(repository)

        assertEquals(PostingDetailLoadState.Failed(FeedFailureReason.Maintenance), viewModel.state.value.loadState)

        repository.onGetPostingDetail = null
        viewModel.onEvent(PostingDetailEvent.RetryClicked)

        assertTrue(viewModel.state.value.loadState is PostingDetailLoadState.Loaded)
    }

    @Test
    fun `그 밖의 실패만 일반 실패로 접고 결함으로 기록한다`() {
        val repository = repositoryWith(postingDetail(id = POSTING_ID))
        repository.onGetPostingDetail = { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())) }

        assertEquals(PostingDetailLoadState.Failed(FeedFailureReason.Generic), viewModel(repository).state.value.loadState)
        assertEquals(listOf("posting_detail"), reporter.stages)
    }

    @Test
    fun `401 은 세션 종료 신호를 올린다`() {
        val repository =
            FakePostingRepository.strict().apply {
                onGetPostingDetail = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", RuntimeException())) }
            }

        assertTrue(viewModel(repository).state.value.sessionEnded)
    }

    private fun sampleSuitability(): Suitability =
        Suitability(
            score = 88,
            label = SuitabilityLabel.VerySuitable,
            breakdown = emptyList(),
            strengthComment = null,
            weaknessComment = null,
        )

    private companion object {
        const val POSTING_ID = 7L
    }
}
