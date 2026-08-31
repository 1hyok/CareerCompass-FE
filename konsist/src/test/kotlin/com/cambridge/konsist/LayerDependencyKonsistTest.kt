package com.cambridge.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * 레이어 의존 방향 회귀 가드.
 *
 * `presentation(UI) → domain ← data` — UI·data 가 domain 에 의존하고, domain 은 다른 레이어나
 * 비-`core:model` 코어에 **역의존하지 않는다**(의존 역전). Repository 추상을 domain 에 두는 구조를 보호한다.
 *
 * domain 패키지(`com.cambridge..domain..`) 파일의 import 만 검사한다.
 * - 허용: `core:model`(`com.cambridge.core.model.*`), 같은 domain 레이어(`com.cambridge..domain..`; cross-domain 포함)
 * - 금지: Android SDK, data·presentation 레이어, 비-`core:model` 코어(network/ui/common/datastore/di)
 *
 * 외부 빌드 그래프가 아니라 소스 import 를 스캔하므로, Gradle 사이클 탐지가 못 잡는
 * `domain → core:network` 같은 위반까지 막는다.
 */
class LayerDependencyKonsistTest {
    private fun domainFiles() = Konsist.scopeFromProject().files.withPackage("com.cambridge..domain..")

    @Test
    fun `domain 은 data 와 presentation 레이어에 의존하지 않는다`() {
        domainFiles().assertFalse { file ->
            file.imports.any { import -> FORBIDDEN_LAYER_IMPORT.matches(import.name) }
        }
    }

    @Test
    fun `domain 은 core_model 과 같은 domain 레이어 외 careercompass 모듈에 의존하지 않는다`() {
        domainFiles().assertFalse { file ->
            file.imports.any { import ->
                import.name.startsWith(CAREERCOMPASS_PREFIX) && !ALLOWED_INTERNAL_IMPORT.matches(import.name)
            }
        }
    }

    @Test
    fun `domain 은 Android SDK 타입에 의존하지 않는다`() {
        domainFiles().assertFalse { file ->
            file.imports.any { import -> import.name.startsWith(ANDROID_SDK_PREFIX) }
        }
    }

    @Test
    fun `fixture 의 data 와 presentation import 는 모두 금지한다`() {
        val imports =
            architectureFixtureImports(
                relativePath = "domain/ForbiddenLayerImports.kt",
                expectedImports =
                    setOf(
                        "com.cambridge.core.data.UserRepositoryImpl",
                        "com.cambridge.feature.feed.data.FeedRepositoryImpl",
                        "com.cambridge.feature.feed.presentation.FeedScreen",
                    ),
            )
        val missedImports = imports.filterNot(FORBIDDEN_LAYER_IMPORT::matches)

        check(missedImports.isEmpty()) {
            "domain 금지 import 를 놓쳤다: ${missedImports.sorted()}"
        }
    }

    @Test
    fun `fixture 의 model 과 domain 과 표준 라이브러리 import 는 허용한다`() {
        val imports =
            architectureFixtureImports(
                relativePath = "domain/AllowedImports.kt",
                expectedImports =
                    setOf(
                        "com.cambridge.core.domain.UserRepository",
                        "com.cambridge.core.model.User",
                        "com.cambridge.feature.feed.domain.GetFeedUseCase",
                        "kotlin.collections.List",
                    ),
            )
        val falsePositives = imports.filter(::isForbiddenInternalImport)

        check(falsePositives.isEmpty()) {
            "domain 허용 import 를 금지했다: ${falsePositives.sorted()}"
        }
    }

    private fun isForbiddenInternalImport(importName: String): Boolean =
        FORBIDDEN_LAYER_IMPORT.matches(importName) ||
            (importName.startsWith(CAREERCOMPASS_PREFIX) && !ALLOWED_INTERNAL_IMPORT.matches(importName))

    private companion object {
        const val ANDROID_SDK_PREFIX = "android."
        const val CAREERCOMPASS_PREFIX = "com.cambridge."

        /** data / presentation 레이어 패키지. */
        val FORBIDDEN_LAYER_IMPORT = Regex("""^com\.cambridge\..*\.(data|presentation)(?:\..*)?$""")

        /** core:model 또는 같은 domain 레이어만 허용 (그 외 com.cambridge.* 는 위반). */
        val ALLOWED_INTERNAL_IMPORT =
            Regex("""^com\.cambridge\.core\.model(?:\..*)?$|^com\.cambridge\..*\.domain(?:\..*)?$""")
    }
}
