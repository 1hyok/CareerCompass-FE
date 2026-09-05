package com.careercompass.feature.foryou.domain.testing

import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import com.careercompass.feature.foryou.domain.model.StrengthExport
import com.careercompass.feature.foryou.domain.repository.StrengthExportRepository
import java.util.concurrent.CopyOnWriteArrayList

/** [StrengthExportRepository] fake 정본 — 실제로 실려 간 형식·구획을 기록한다. */
public class FakeStrengthExportRepository(
    private var result: Result<StrengthExport> =
        Result.success(StrengthExport(format = ExportFormat.Markdown, content = "# 강점")),
    public var onExport: (suspend (ExportFormat, List<ExportSection>) -> Result<StrengthExport>)? = null,
) : StrengthExportRepository {
    public val requests: CopyOnWriteArrayList<Pair<ExportFormat, List<ExportSection>>> = CopyOnWriteArrayList()

    public fun returns(value: Result<StrengthExport>) {
        result = value
    }

    override suspend fun export(
        format: ExportFormat,
        sections: List<ExportSection>,
    ): Result<StrengthExport> {
        requests += format to sections
        onExport?.let { return it(format, sections) }
        return result
    }
}
