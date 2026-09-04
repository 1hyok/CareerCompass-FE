plugins {
    id("careercompass.android.library")
    id("careercompass.android.hilt")
    id("careercompass.kover")
}

android {
    namespace = "com.careercompass.core.common"
    resourcePrefix = "core_common_"
}

dependencies {
    // 리포팅 정책이 CoreDataFailure·CoreAuthFailure 로 접힌 원인을 되읽는다 — 계약만 쓰고 구현은 보지 않는다.
    implementation(projects.core.domain)
    implementation(libs.coroutines.core)
}
