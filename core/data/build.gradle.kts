plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.core.data"
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(projects.core.common)
    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.androidx.datastore.preferences)
}
