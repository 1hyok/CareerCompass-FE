plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.feature.profile.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.profile.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)

    testImplementation(libs.coroutines.test)
}
