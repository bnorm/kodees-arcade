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

rootProject.name = "kodees-arcade"

include(":arcade-agent")
include(":arcade-app")
include(":arcade-samples")
include(":arcade-samples:shared")
include(":arcade-samples:kodee")
include(":arcade-samples:snail")
