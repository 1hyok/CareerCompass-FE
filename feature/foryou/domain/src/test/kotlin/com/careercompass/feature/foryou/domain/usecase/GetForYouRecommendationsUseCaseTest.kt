package com.careercompass.feature.foryou.domain.usecase

import com.careercompass.feature.foryou.domain.model.ForYouRecommendation
import com.careercompass.feature.foryou.domain.model.ForYouRecommendations
import com.careercompass.feature.foryou.domain.model.MAX_FOR_YOU_PER_CATEGORY
import com.careercompass.feature.foryou.domain.testing.FakeForYouRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GetForYouRecommendationsUseCaseTest {
    private val repository = FakeForYouRepository()
    private val useCase = GetForYouRecommendationsUseCase(repository)

    @Test
    fun `카테고리별로 최대 5건까지만 남긴다`() =
        runTest {
            repository.returns(
                Result.success(
                    ForYouRecommendations(
                        topPick = recommendation(1),
                        byStrength = (1L..7L).map(::recommendation),
                        byGap = (11L..13L).map(::recommendation),
                    ),
                ),
            )

            val result = useCase().getOrThrow()

            assertEquals(MAX_FOR_YOU_PER_CATEGORY, result.byStrength.size)
            assertEquals(listOf(1L, 2L, 3L, 4L, 5L), result.byStrength.map { it.postingId })
            assertEquals(3, result.byGap.size)
        }

    @Test
    fun `톱 픽은 단수라 자르지 않는다`() =
        runTest {
            repository.returns(
                Result.success(
                    ForYouRecommendations(topPick = recommendation(42), byStrength = emptyList(), byGap = emptyList()),
                ),
            )

            val result = useCase().getOrThrow()

            assertEquals(42L, result.topPick?.postingId)
        }

    @Test
    fun `추천이 하나도 없으면 비어 있다고 답한다`() =
        runTest {
            repository.returns(
                Result.success(ForYouRecommendations(topPick = null, byStrength = emptyList(), byGap = emptyList())),
            )

            assertTrue(useCase().getOrThrow().isEmpty)
        }

    @Test
    fun `실패는 그대로 흘려보낸다`() =
        runTest {
            val failure = IOException("offline")
            repository.returns(Result.failure(failure))

            assertSame(failure, useCase().exceptionOrNull())
        }

    private fun recommendation(id: Long): ForYouRecommendation = ForYouRecommendation(postingId = id, reasons = listOf("이유 $id"))
}
