import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    // core-ktx 1.19 / lifecycle 2.11 이 android-37 컴파일을 요구한다 (targetSdk 는 36 유지).
    extension.compileSdk = 37

    extension.configureDefaultConfig(this)

    extensions
        .findByType(org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension::class.java)
        ?.compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

    configureEmptyModuleTestDiscovery()

    careerCompassDependencies {
        implementation("androidx-core-ktx")
        testImplementation("junit")
        androidTestImplementation("androidx-junit")
        androidTestImplementation("androidx-espresso-core")
    }
}

/**
 * Gradle 9 는 「테스트 태스크가 하나도 발견하지 못함」을 실패로 본다. 옳은 기본값이지만, 아직
 * 화면이 붙지 않아 `src/test` 디렉터리 자체가 없는 골격 모듈까지 빨갛게 만들어 Unit Test 신호를
 * 죽인다 — 실제 회귀와 「아직 안 썼다」가 구별되지 않는다.
 *
 * 그래서 면제는 `src/test` 가 없는 모듈로만 좁힌다. 첫 테스트를 넣는 순간 가드가 되살아나므로,
 * 「테스트를 넣었는데 발견이 끊긴」 회귀는 그대로 잡힌다. 모듈이 테스트를 통째로 지워 면제로
 * 되돌아가는 경우는 Kover 커버리지 정책이 0% 로 잡는다.
 */
private fun Project.configureEmptyModuleTestDiscovery() {
    val hasTestSources = file("src/test").exists()
    tasks.withType(Test::class.java).configureEach {
        failOnNoDiscoveredTests.set(hasTestSources)
    }
}

private fun CommonExtension.configureDefaultConfig(project: Project) {
    when (this) {
        is ApplicationExtension -> {
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        is LibraryExtension -> {
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                // consumer-rules.pro 가 있는 모듈만 등록 — 없는 모듈(domain·res 등)엔 빈 파일을 강요하지 않음
                if (project.file("consumer-rules.pro").exists()) {
                    consumerProguardFiles("consumer-rules.pro")
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

internal fun Project.configureCompose(extension: CommonExtension) {
    when (extension) {
        is ApplicationExtension -> extension.buildFeatures { compose = true }
        is LibraryExtension -> extension.buildFeatures { compose = true }
    }

    careerCompassDependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        implementation(platform(bom))
        androidTestImplementation(platform(bom))
        implementation("androidx-compose-ui")
        implementation("androidx-compose-material3")
        implementation("androidx-activity-compose")
        implementation("androidx-compose-ui-tooling-preview")
        debugImplementation("androidx-compose-ui-tooling")
    }
}
