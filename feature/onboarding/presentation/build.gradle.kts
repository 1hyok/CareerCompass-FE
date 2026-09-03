plugins {
    id("careercompass.android.library.compose")
    id("careercompass.android.hilt")
    id("careercompass.android.navigation")
    alias(libs.plugins.compose.screenshot)
    id("careercompass.kover")
}

// Google 로그인(Credential Manager)의 서버 클라이언트 ID. 읽기와 release 가드는 socialLoginKey 가 한 호출로 배선한다 —
// 앱 모듈의 KAKAO_NATIVE_APP_KEY 와 같은 경로(local.properties → 환경변수)다.
val googleWebClientId = socialLoginKey("GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "com.cambridge.feature.onboarding.presentation"
    resourcePrefix = "onboarding_"
    testOptions.unitTests.isIncludeAndroidResources = true
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.ui)

    implementation(libs.androidx.lifecycle.runtime.compose)
    // SavedStateHandle — 온보딩 입력 초안이 프로세스 사망을 건너게 한다(#133).
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kakao.sdk.user)
    implementation(libs.kakao.sdk.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.biometric)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(testFixtures(projects.feature.onboarding.domain))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
