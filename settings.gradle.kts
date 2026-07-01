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
include(":arcade-samples")
include(":arcade-samples:shared")
include(":arcade-samples:kodee")
include(":arcade-samples:snail")
include(":arcade-service:arcade-api")
include(":arcade-service:arcade-client")
include(":arcade-service:arcade-server")
include(":arcade-service:arcade-webapp")
