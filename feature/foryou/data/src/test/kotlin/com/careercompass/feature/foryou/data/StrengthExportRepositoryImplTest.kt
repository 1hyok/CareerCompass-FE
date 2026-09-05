package com.careercompass.feature.foryou.data

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.network.dto.StrengthExportDto
import com.careercompass.core.network.dto.StrengthExportRequestDto
import com.careercompass.core.network.model.ApiException
import com.careercompass.core.network.model.BaseResponse
import com.careercompass.core.network.service.StrengthExportApiService
import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthExportRepositoryImplTest {
    private class FakeStrengthExportApi(
        private val throws: Throwable? = null,
    ) : StrengthExportApiService {
        val requests = mutableListOf<StrengthExportRequestDto>()

        override suspend fun exportStrengths(body: StrengthExportRequestDto): BaseResponse<StrengthExportDto> {
            requests += body
            throws?.let { throw it }
            return BaseResponse(ok = true, data = StrengthExportDto(format = body.format, content = "# 정일혁"))
        }
    }

    @Test
    fun `고른 형식과 구획이 그대로 실린다`() =
        runTest {
            val api = FakeStrengthExportApi()

            val export =
                StrengthExportRepositoryImpl(api)
                    .export(ExportFormat.Notion, listOf(ExportSection.Basic, ExportSection.Awards))
                    .getOrThrow()

            assertEquals("notion", api.requests.single().format)
            assertEquals(listOf("basic", "awards"), api.requests.single().sections)
            assertEquals(ExportFormat.Notion, export.format)
            assertEquals("# 정일혁", export.content)
        }

    @Test
    fun `LLM 장애는 점검 사유로 접힌다`() =
        runTest {
            val api =
                FakeStrengthExportApi(
                    throws =
                        ApiException(
                            code = "LLM_UNAVAILABLE",
                            serverMessage = null,
                            fallbackMessage = "요청에 실패했습니다.",
                            status = 503,
                        ),
                )

            val failure =
                StrengthExportRepositoryImpl(api).export(ExportFormat.Markdown, listOf(ExportSection.Summary)).exceptionOrNull()

            assertTrue("실제 사유: $failure", failure is CoreDataFailure.ServiceUnavailable)
        }
}
