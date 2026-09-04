package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakePostingRepository
import com.careercompass.feature.feed.domain.posting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class TogglePostingBookmarkUseCaseTest {
    @Test
    fun `북마크를 뒤집어 저장하고 새 값을 돌려준다`() =
        runTest {
            val repository = FakePostingRepository(initial = listOf(posting(id = 3, isBookmarked = false)))
            val useCase = TogglePostingBookmarkUseCase(repository)

            assertEquals(Result.success(true), useCase(3, currentlyBookmarked = false))
            assertEquals(Result.success(false), useCase(3, currentlyBookmarked = true))
            assertEquals(listOf(3L to true, 3L to false), repository.bookmarkCalls.toList())
        }

    @Test
    fun `저장이 실패하면 실패를 그대로 돌려준다`() =
        runTest {
            val repository =
                FakePostingRepository.strict().apply {
                    onSetBookmarked = { _, _ -> Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
                }

            val outcome = TogglePostingBookmarkUseCase(repository)(3, currentlyBookmarked = false)

            assertTrue(outcome.exceptionOrNull() is CoreDataFailure.NetworkUnavailable)
        }
}
