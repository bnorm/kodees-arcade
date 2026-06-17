import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.reload.gradle.forAllJvmTargets
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.java

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    kotlin("plugin.compose") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    group = "dev.bnorm.arcade"
    version = "1.0-SNAPSHOT"

    val javaVersion = JavaVersion.VERSION_25

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
            }

            targets.withType(KotlinJvmTarget::class.java) {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                    freeCompilerArgs.add("-Xjvm-default=all")
                }
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
