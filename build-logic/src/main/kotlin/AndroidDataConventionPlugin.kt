import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("careercompass.android.library")
            pluginManager.apply("careercompass.android.hilt")
            pluginManager.apply("careercompass.android.retrofit")

            careerCompassDependencies {
                project(":core:datastore")
                project(":core:domain")
                project(":core:model")
                project(":core:network")
            }
        }
    }
}
