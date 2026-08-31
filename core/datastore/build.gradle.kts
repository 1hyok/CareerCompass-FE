plugins {
    id("careercompass.android.datastore")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
