plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.careercompass.feature.notification.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.notification.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)

    testImplementation(libs.coroutines.test)
}
