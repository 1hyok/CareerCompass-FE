import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.Properties

/**
 * 빌드 입력값을 저장소 밖에서 읽는다 — 루트 `local.properties`(gitignore) → 환경변수 순.
 *
 * 키·주소를 빌드 스크립트에 직접 박지 않기 위한 경로 제한이다(저장소 유출 시 도용 위험, 그리고
 * 사람마다 다른 값을 커밋 없이 갖게 하려는 목적). 값이 없으면 null 을 주고, 무엇으로 폴백할지는
 * 호출부가 정한다 — 빈 문자열(ReleaseKeyGuard.kt)이거나 자리표시자 주소(BaseUrlGuard.kt)다.
 */
internal fun Project.externalBuildValue(keyName: String): String? {
    val localPropertiesFile = rootProject.file("local.properties")
    val fromLocalProperties =
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                Properties().apply { load(stream) }.getProperty(keyName)
            }
        } else {
            null
        }
    return fromLocalProperties ?: System.getenv(keyName)
}

/**
 * `pre<Variant>Build` 에 매달아 그 variant 를 실제로 빌드할 때만 도는 검증 태스크를 등록한다.
 *
 * 판정과 메시지를 값으로 받는 이유는 configuration cache 다 — 태스크 상태가 직렬화되므로 액션
 * 람다가 [Project] 를 캡처하면 안 된다.
 *
 * 쓰는 쪽은 둘이다: release 서명·소셜 키 가드(ReleaseKeyGuard.kt)가 `preReleaseBuild` 에,
 * debug API 주소 가드(BaseUrlGuard.kt)가 `preDebugBuild` 에 건다.
 */
internal fun Project.registerVariantBuildGuard(
    preBuildTaskName: String,
    taskName: String,
    taskDescription: String,
    shouldFail: Boolean,
    failureMessage: String,
) {
    val guard =
        tasks.register(taskName) {
            group = "verification"
            description = taskDescription
            doFirst {
                if (shouldFail) {
                    throw GradleException(failureMessage)
                }
            }
        }
    tasks.matching { it.name == preBuildTaskName }.configureEach { dependsOn(guard) }
}
