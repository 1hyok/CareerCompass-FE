package com.cambridge.feature.feed.presentation.postingraw

import androidx.lifecycle.SavedStateHandle
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakePostingRepository
import com.cambridge.feature.feed.domain.usecase.OpenPostingDetailUseCase
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.navigation.FEED_ARG_POSTING_ID
import com.cambridge.feature.feed.presentation.postingDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostingRawViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    private fun viewModel(repository: FakePostingRepository): PostingRawViewModel =
        PostingRawViewModel(
            savedStateHandle = SavedStateHandle(mapOf(FEED_ARG_POSTING_ID to POSTING_ID)),
            openPostingDetail = OpenPostingDetailUseCase(repository),
            errorReporter = reporter,
            clock = FIXED_CLOCK,
        )

    @Test
    fun `원문을 받아 오고 원본 링크·뒤로가기는 단발 신호로 올라간다`() {
        val viewModel = viewModel(FakePostingRepository(details = listOf(postingDetail(id = POSTING_ID, isRead = true))))

        assertTrue(viewModel.state.value.loadState is PostingRawLoadState.Loaded)

        viewModel.onEvent(PostingRawEvent.OpenOriginalClicked)
        assertEquals("https://example.com/postings/$POSTING_ID", viewModel.state.value.openUrl)
        viewModel.onOpenUrlConsumed()
        assertNull(viewModel.state.value.openUrl)

        viewModel.onEvent(PostingRawEvent.BackClicked)
        assertTrue(viewModel.state.value.isBackRequested)
        viewModel.onBackConsumed()
        assertTrue(!viewModel.state.value.isBackRequested)
    }

    @Test
    fun `실패는 네트워크 여부를 구분하고 다시 시도로 복구한다`() {
        val repository = FakePostingRepository(details = listOf(postingDetail(id = POSTING_ID)))
        repository.onGetPostingDetail = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
        val viewModel = viewModel(repository)

        assertEquals(PostingRawLoadState.Failed(isNetworkUnavailable = true), viewModel.state.value.loadState)
        // 화면이 사유를 그대로 안내하는 실패다 — 세션 표본 한 건만 남기고 재시도는 접는다.
        assertEquals(listOf("posting_raw"), reporter.stages)

        repository.onGetPostingDetail = null
        viewModel.retry()

        assertTrue(viewModel.state.value.loadState is PostingRawLoadState.Loaded)
    }

    @Test
    fun `401 은 세션 종료 신호를 올린다`() {
        val repository =
            FakePostingRepository.strict().apply {
                onGetPostingDetail = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", RuntimeException())) }
            }

        assertTrue(viewModel(repository).state.value.sessionEnded)
    }

    private companion object {
        const val POSTING_ID = 7L
    }
}
