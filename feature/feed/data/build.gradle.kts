plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.careercompass.feature.feed.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.feed.domain)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coroutines.core)

    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.coroutines.test)
}
