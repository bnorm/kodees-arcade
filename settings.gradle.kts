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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "kodees-arcade"

include(":arcade-agent")
include(":arcade-app")
include(":arcade-engine")
include(":arcade-samples")
include(":arcade-samples:shared")
include(":arcade-samples:kodee")
include(":arcade-samples:snail")
include(":arcade-service:arcade-api")
include(":arcade-service:arcade-client")
include(":arcade-service:arcade-server")
include(":arcade-service:arcade-webapp")
