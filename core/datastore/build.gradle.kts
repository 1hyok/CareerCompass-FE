plugins {
    id("careercompass.android.datastore")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.core.datastore"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coroutines.core)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
}
