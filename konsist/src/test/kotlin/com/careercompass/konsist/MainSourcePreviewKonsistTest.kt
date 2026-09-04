package com.careercompass.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * main 소스셋 `@Preview` 재유입 가드.
 *
 * main `@Preview` 는 한 번 걷어내도 가드가 없으면 슬금슬금 다시 들어온다.
 * 그때마다 같은 청소를 되풀이하지 않으려고 처음부터 막는다.
 *
 * ### 왜 main 프리뷰를 두지 않는가
 * 1. **CI 가 검증하지 않는다** — 골든이 없어 렌더가 깨져도 아무도 모른다. 같은 그림을 검증받는
 *    `screenshotTest` 쪽이 정본이다.
 * 2. 같은 그림을 두 소스셋에서 관리하면 시안이 바뀔 때 **한쪽만 고쳐진다**.
 * 3. 프리뷰용 더미 데이터와 no-op 배선이 프로덕션 소스셋에 남는다 이 막으려던
 *    「전 액션을 한 줄로 죽이는 우회로」가 `FeedHomeActions.Noop` 으로 프로덕션 API 에
 *    새어 나온 것이 그 실사례다.
 *
 * ### 검사 대상 — [GUARDED_MODULE_PREFIXES]
 * 접두어 목록에 오른 모듈만 본다. 새 feature 모듈은 첫 화면을 넣을 때 접두어를 여기에 더해 처음부터
 * 가드 아래에서 시작한다.
 *
 * 프리뷰가 필요하면 `src/screenshotTest` 에 `@PreviewTest` + `@Preview` 로 둔다 — 골든이 붙어
 * CI 가 렌더를 지킨다.
 */
class MainSourcePreviewKonsistTest {
    @Test
    fun `청소를 마친 모듈의 main 소스셋에는 Preview 를 두지 않는다`() {
        val violations =
            guardedFiles().flatMap { file ->
                file
                    .functions()
                    .filter { function -> function.annotations.any { it.name == PREVIEW } }
                    .map { function -> "${file.normalizedProjectPath()} — ${function.name}" }
            }

        check(violations.isEmpty()) {
            buildString {
                appendLine("main 소스셋에 @Preview 가 다시 들어왔다 (${violations.size}건).")
                appendLine("골든이 없어 CI 가 렌더를 검증하지 못하고, 프리뷰용 더미·no-op 배선이 프로덕션 소스에 남는다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("프리뷰가 필요하면 src/screenshotTest 에 @PreviewTest + @Preview 로 둔다.")
            }
        }
    }

    private fun guardedFiles(): List<KoFileDeclaration> =
        Konsist
            .scopeFromProject()
            .files
            .filter { file ->
                val path = file.normalizedProjectPath()
                "/src/main/" in path && GUARDED_MODULE_PREFIXES.any { path.startsWith(it) }
            }

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val PREVIEW = "Preview"

        /**
         * 가드가 적용되는 모듈. `@Preview` 를 담을 수 있는 모듈은 전부 여기 있어야 한다 —
         * 빠진 모듈은 검사 자체를 받지 않으므로, presentation 모듈을 새로 만들면 함께 더한다.
         */
        val GUARDED_MODULE_PREFIXES =
            listOf(
                "core/ui/",
                "feature/onboarding/presentation/",
                "feature/feed/presentation/",
                "feature/editor/presentation/",
                "feature/profile/presentation/",
                "feature/foryou/presentation/",
                "feature/notification/presentation/",
            )
    }
}
