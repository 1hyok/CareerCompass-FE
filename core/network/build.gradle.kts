plugins {
    id("careercompass.android.library")
    id("careercompass.android.retrofit")
    id("careercompass.android.hilt")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.core.network"
    buildFeatures {
        buildConfig = true
    }
    // TODO(BE): CareerCompass 백엔드가 서면 실제 주소로 바꾼다. 지금은 컴파일만 통과시키는 자리표시자다.
    defaultConfig { buildConfigField("String", "BASE_URL", "\"https://api.careercompass.invalid/api/v1/\"") }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.testcontainers.mockserver)
    testImplementation(testFixtures(projects.core.domain))
}
