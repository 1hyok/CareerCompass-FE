plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.careercompass.feature.foryou.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.foryou.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)
    // API_SPEC §9 실패 번역표(`mapDataFailure`)를 §7 도 같은 것으로 쓴다 — 표를 모듈마다 다시 적지 않는다.
    implementation(projects.core.data)

    testImplementation(libs.coroutines.test)
}
