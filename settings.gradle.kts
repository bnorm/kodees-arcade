pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    plugins {
        val kotlinVersion = "2.4.0"

        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        kotlin("plugin.compose") version kotlinVersion
        id("org.jetbrains.compose") version "1.11.1"
        id("com.gradleup.shadow") version "8.3.5"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "kodee-arcade"

include(":arcade-agent")
include(":arcade-core")
include(":arcade-engine")
include(":arcade-main")
include(":arcade-render")
include(":arcade-runner")
include(":arcade-ui")

include(":games:cybertanks:cybertanks-api")
include(":games:cybertanks:cybertanks-cartridge")
include(":games:cybertanks:cybertanks-sample")
