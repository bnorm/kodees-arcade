import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    kotlin("plugin.compose") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    group = "dev.bnorm.arcade"
    version = "1.0-SNAPSHOT"

    val javaVersion = JavaVersion.VERSION_21

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
            }

            targets.withType<KotlinJvmTarget>().configureEach {
                compilerOptions {
                    jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
                    jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
                }
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register<Sync>("site") {
    into(project.layout.buildDirectory.dir("_site"))
    from(project(":arcade-app").tasks.named("wasmJsBrowserDistribution"))
}
