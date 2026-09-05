import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * `apiBaseUrls`·`requireDebugBaseUrlIsNotProduction` 의 빌드 계약 회귀 테스트 (Gradle TestKit).
 *
 * ReleaseKeyGuardTest 와 같은 구조다 — AGP 를 올리지 않은 스텁 프로젝트에 가짜 `preDebugBuild` 를
 * 두고 가드 계약만 고정한다. 실제 AGP 그래프(debug 컴파일이 `preDebugBuild` 를 경유하는 것)는 AGP
 * 소관이라 여기서 재검증하지 않는다(그 배선 증거는 `:app:assembleDebug` 로 운영 주소를 주입해 본
 * 실측이다).
 *
 * 판정 자체는 인자만 보는 `requireDebugBaseUrlIsNotProduction` 으로 고정하고, 키를 읽는 경로는
 * `local.properties` 를 쓰는 두 테스트로만 확인한다 — 환경변수까지 얽으면 CI 환경에 따라 결과가
 * 흔들린다.
 */
class BaseUrlGuardTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Test
    fun `debug 가 운영 호스트를 가리키면 debug 경로가 지정 메시지로 실패한다`() {
        writeStubProject(
            """
            requireDebugBaseUrlIsNotProduction(
                "https://api.careercompass.example/api/v1/",
                "https://api.careercompass.example/api/v1/",
            )
            """.trimIndent(),
        )

        val result = runner("assembleDebug").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkDebugBaseUrlIsNotProduction")?.outcome)
        assertTrue(result.output.contains("debug 빌드가 운영 API 주소를 가리켜 빌드를 중단합니다"))
        assertTrue(result.output.contains("api.careercompass.example"))
    }

    @Test
    fun `경로만 다르고 호스트가 같아도 운영으로 판정한다`() {
        writeStubProject(
            """
            requireDebugBaseUrlIsNotProduction(
                "https://api.careercompass.example/api/v2/",
                "https://api.careercompass.example/api/v1/",
            )
            """.trimIndent(),
        )

        val result = runner("assembleDebug").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkDebugBaseUrlIsNotProduction")?.outcome)
    }

    @Test
    fun `호스트가 다르면 debug 경로가 통과한다`() {
        writeStubProject(
            """
            requireDebugBaseUrlIsNotProduction(
                "https://dev.careercompass.example/api/v1/",
                "https://api.careercompass.example/api/v1/",
            )
            """.trimIndent(),
        )

        val result = runner("assembleDebug").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkDebugBaseUrlIsNotProduction")?.outcome)
    }

    @Test
    fun `운영 주소가 주입되지 않았으면 자리표시자끼리 같아도 통과한다`() {
        writeStubProject(
            """
            requireDebugBaseUrlIsNotProduction(
                PLACEHOLDER_API_BASE_URL,
                PLACEHOLDER_API_BASE_URL,
            )
            """.trimIndent(),
        )

        val result = runner("assembleDebug").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkDebugBaseUrlIsNotProduction")?.outcome)
    }

    @Test
    fun `debug 가 운영을 가리켜도 release 경로는 가드를 태우지 않는다`() {
        writeStubProject(
            """
            requireDebugBaseUrlIsNotProduction(
                "https://api.careercompass.example/api/v1/",
                "https://api.careercompass.example/api/v1/",
            )
            """.trimIndent(),
        )

        val result = runner("assembleRelease").build()

        assertNull(result.task(":checkDebugBaseUrlIsNotProduction"))
    }

    @Test
    fun `apiBaseUrls 는 local properties 의 두 주소를 빌드 타입별로 나눠 준다`() {
        writeStubProject(
            """
            val urls = apiBaseUrls()
            println("debug=[" + urls.debug + "] release=[" + urls.release + "]")
            """.trimIndent(),
        )
        projectDir.newFile("local.properties").writeText(
            """
            BASE_URL_DEV=https://dev.careercompass.example/api/v1/
            BASE_URL_PROD=https://api.careercompass.example/api/v1/
            """.trimIndent() + "\n",
        )

        val result = runner("assembleDebug").build()

        assertTrue(
            result.output.contains(
                "debug=[https://dev.careercompass.example/api/v1/] " +
                    "release=[https://api.careercompass.example/api/v1/]",
            ),
        )
    }

    @Test
    fun `apiBaseUrls 는 개발 주소만 있으면 운영 주소를 자리표시자로 폴백한다`() {
        // BASE_URL_PROD 환경변수가 없는 환경을 전제한다 — local.properties 가 우선이라 개발 주소는
        // 고정되고, 폴백을 보려면 운영 키를 어느 경로에도 두지 않는 수밖에 없다.
        writeStubProject(
            """
            println("release=[" + apiBaseUrls().release + "]")
            """.trimIndent(),
        )
        projectDir.newFile("local.properties").writeText(
            "BASE_URL_DEV=https://dev.careercompass.example/api/v1/\n",
        )

        val result = runner("assembleDebug").build()

        assertTrue(result.output.contains("release=[https://api.careercompass.invalid/api/v1/]"))
    }

    @Test
    fun `슬래시로 끝나지 않는 주소는 설정 단계에서 끊는다`() {
        writeStubProject("""apiBaseUrls()""")
        projectDir.newFile("local.properties").writeText(
            "BASE_URL_DEV=https://dev.careercompass.example/api/v1\n",
        )

        val result = runner("assembleDebug").buildAndFail()

        assertTrue(result.output.contains("BASE_URL_DEV 의 주소 형식이 잘못돼 빌드를 중단합니다"))
    }

    @Test
    fun `스킴이 없는 주소도 설정 단계에서 끊는다`() {
        writeStubProject("""apiBaseUrls()""")
        projectDir.newFile("local.properties").writeText(
            "BASE_URL_PROD=api.careercompass.example/api/v1/\n",
        )

        val result = runner("assembleDebug").buildAndFail()

        assertTrue(result.output.contains("BASE_URL_PROD 의 주소 형식이 잘못돼 빌드를 중단합니다"))
    }

    private fun writeStubProject(guardWiring: String) {
        val classpathLiteral =
            guardClasspath()
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .joinToString(", ") { "\"${it.replace("\\", "/")}\"" }
        projectDir.newFile("settings.gradle.kts").writeText("rootProject.name = \"base-url-stub\"\n")
        projectDir.newFile("build.gradle.kts").writeText(
            """
            buildscript {
                dependencies {
                    classpath(files($classpathLiteral))
                }
            }

            $guardWiring

            tasks.register("preDebugBuild")
            tasks.register("assembleDebug") { dependsOn("preDebugBuild") }
            tasks.register("preReleaseBuild")
            tasks.register("assembleRelease") { dependsOn("preReleaseBuild") }
            """.trimIndent() + "\n",
        )
    }

    private fun guardClasspath(): String =
        System.getProperty("guardClasspath")
            ?: error("guardClasspath 시스템 프로퍼티 누락 — build-logic/build.gradle.kts 의 Test 태스크 설정 참고")

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir.root)
            .withArguments(*arguments)
}
