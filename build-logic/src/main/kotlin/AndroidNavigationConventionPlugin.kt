import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 네비게이션을 쓰는 모듈의 공통 의존성 — Navigation 3 runtime/UI, NavEntry 범위 ViewModel add-on, NavKey 직렬화.
 *
 * 루트와 피처 로컬 스택이 전부 `NavDisplay` 라 Navigation 2 는 남아 있지 않다(#259 · #260).
 */
class AndroidNavigationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            careerCompassDependencies {
                implementation("androidx-navigation3-runtime")
                implementation("androidx-navigation3-ui")
                // NavEntry 범위 ViewModel 스코프 — rememberViewModelStoreNavEntryDecorator()
                implementation("androidx-lifecycle-viewmodel-navigation3")
                implementation("kotlinx-serialization-json")
            }
        }
    }
}
