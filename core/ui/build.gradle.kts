plugins {
    id("careercompass.android.library.compose")
    kotlin("plugin.serialization")
    alias(libs.plugins.compose.screenshot)
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.core.ui"
    resourcePrefix = "core_ui_"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testFixtures.enable = true
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.common)
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
