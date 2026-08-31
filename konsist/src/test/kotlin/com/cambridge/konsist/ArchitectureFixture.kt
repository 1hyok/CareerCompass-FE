package com.cambridge.konsist

import com.lemonappdev.konsist.api.Konsist

private const val ARCHITECTURE_FIXTURE_ROOT = "konsist/src/test/resources/architecture-fixtures"

/** 실제 Kotlin fixture 를 Konsist 와 같은 경로로 파싱해 import 이름을 반환한다. */
internal fun architectureFixtureImports(
    relativePath: String,
    expectedImports: Set<String>,
): Set<String> {
    val imports =
        Konsist
            .scopeFromFile("$ARCHITECTURE_FIXTURE_ROOT/$relativePath")
            .files
            .flatMap { file -> file.imports.map { it.name } }
            .toSet()

    check(imports == expectedImports) {
        "fixture import 파싱 결과가 다르다 ($relativePath): expected=$expectedImports, actual=$imports"
    }

    return imports
}
