plugins {
    id("careercompass.android.data")
    id("careercompass.kover")
}

android {
    namespace = "com.cambridge.feature.editor.data"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.feature.editor.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)

    testImplementation(libs.coroutines.test)
}
