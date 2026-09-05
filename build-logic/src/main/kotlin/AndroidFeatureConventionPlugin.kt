import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // 1. 우리가 만든 플러그인들을 조합하여 'Feature' 성격 정의
                apply("careercompass.android.library.compose") // 기본 SDK + UI
                apply("careercompass.android.hilt") // DI
                apply("careercompass.android.navigation") // 로컬 Nav3 스택 + NavKey 직렬화
            }

            careerCompassDependencies {
                // 2. 모든 피처 모듈이 공통으로 의존하는 내부 모듈 연결
                project(":core:ui")
                project(":core:model")
            }
        }
    }
}
