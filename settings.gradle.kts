pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "careercompass-fe"

include(":app")
include(":baselineprofile")


// Core Modules
include(":core:common")
include(":core:data")
include(":core:datastore")
include(":core:domain")
include(":core:model")
include(":core:network")
include(":core:ui")

// Feature Modules

include(":feature:onboarding:data")
include(":feature:onboarding:domain")
include(":feature:onboarding:presentation")

include(":feature:feed:data")
include(":feature:feed:domain")
include(":feature:feed:presentation")

include(":feature:editor:data")
include(":feature:editor:domain")
include(":feature:editor:presentation")

include(":feature:profile:data")
include(":feature:profile:domain")
include(":feature:profile:presentation")

include(":feature:foryou:data")
include(":feature:foryou:domain")
include(":feature:foryou:presentation")

include(":feature:notification:data")
include(":feature:notification:domain")
include(":feature:notification:presentation")

// Architecture test module (Konsist) — 레이어 의존 방향 회귀 가드
include(":konsist")
