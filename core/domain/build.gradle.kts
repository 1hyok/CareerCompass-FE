plugins {
    id("careercompass.jvm.domain")
    id("careercompass.kover")
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.coroutines.core)

    testFixturesImplementation(projects.core.model)
    testFixturesImplementation(libs.coroutines.core)
}
