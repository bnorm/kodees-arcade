pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
    }

    plugins {
        val kotlinVersion = "2.1.0-firework.31"

        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        kotlin("plugin.compose") version kotlinVersion
        id("org.jetbrains.compose") version "1.7.1"
        id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.31.7"
        id("com.gradleup.shadow") version "8.3.5"
    }

    dependencyResolutionManagement {
        repositories {
            mavenCentral()
            google()
            maven("https://packages.jetbrains.team/maven/p/firework/dev")
        }
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
