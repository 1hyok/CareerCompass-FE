plugins {
    id("careercompass.android.library.compose")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
    id("careercompass.kover")
}

android {
    namespace = "com.careercompass.core.ui"
    resourcePrefix = "core_ui_"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testFixtures.enable = true
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    // MviViewModel 이 androidx.lifecycle.ViewModel 을 상속해 노출하므로 feature 컴파일 클래스패스에도
    // 올라가야 한다. implementation 이면 상속체가 supertype 을 못 본다 (#244).
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(projects.core.common)
    // 실패 → 표시 계약(FailureDisplay)이 도메인 사유를 읽고 상한 상수를 인용한다. 공개 시그니처에는
    // 두 모듈의 타입이 나오지 않으므로 계약만 쓰고 구현은 보지 않는다.
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.ui.test.junit4)
}
