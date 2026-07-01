pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kodees-arcade"

include(":arcade-machine:arcade-app")
include(":arcade-machine:arcade-display")
include(":arcade-machine:arcade-engine")
include(":arcade-machine:arcade-multicade")
include(":arcade-player")
include(":arcade-player-samples")
include(":arcade-player-samples:shared")
include(":arcade-player-samples:kodee")
include(":arcade-player-samples:snail")
include(":arcade-service:arcade-api")
include(":arcade-service:arcade-client")
include(":arcade-service:arcade-server")
include(":arcade-service:arcade-webapp")
include(":arcade-service:arcade-worker")
