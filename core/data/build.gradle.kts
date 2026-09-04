plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.careercompass.core.data"
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coroutines.core)

    testImplementation(testFixtures(projects.core.domain))
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.coroutines.test)
}
