plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.feature.onboarding.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.onboarding.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)

    testImplementation(libs.coroutines.test)
}
