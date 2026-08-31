plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.feature.feed.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.feed.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)

    testImplementation(libs.coroutines.test)
}
