package com.careercompass.feature.foryou.domain.usecase

import com.careercompass.feature.foryou.domain.error.ForYouFailure
import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import com.careercompass.feature.foryou.domain.testing.FakeStrengthExportRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportStrengthsUseCaseTest {
    private val repository = FakeStrengthExportRepository()
    private val useCase = ExportStrengthsUseCase(repository)

    @Test
    fun `구획을 하나도 고르지 않으면 서버를 부르지 않는다`() =
        runTest {
            val result = useCase(format = ExportFormat.Markdown, sections = emptyList())

            assertTrue(result.exceptionOrNull() is ForYouFailure.NoExportSection)
            assertTrue(repository.requests.isEmpty())
        }

    @Test
    fun `같은 구획을 두 번 고르면 처음 자리만 남긴다`() =
        runTest {
            useCase(
                format = ExportFormat.Notion,
                sections = listOf(ExportSection.Skills, ExportSection.Basic, ExportSection.Skills),
            ).getOrThrow()

            assertEquals(
                listOf(ExportFormat.Notion to listOf(ExportSection.Skills, ExportSection.Basic)),
                repository.requests.toList(),
            )
        }

    @Test
    fun `서버가 만든 문서를 그대로 돌려준다`() =
        runTest {
            val export = useCase(format = ExportFormat.Markdown, sections = listOf(ExportSection.Summary)).getOrThrow()

            assertEquals(ExportFormat.Markdown, export.format)
            assertEquals("# 강점", export.content)
        }
}
