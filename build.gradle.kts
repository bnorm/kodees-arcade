import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    kotlin("plugin.compose") apply false
    id("org.jetbrains.compose") apply false
    id("io.ktor.plugin") apply false
}

allprojects {
    group = "dev.bnorm.arcade"
    version = "1.0-SNAPSHOT"

    val javaVersion = JavaVersion.VERSION_21

    fun HasConfigurableKotlinCompilerOptions<*>.configureCommonCompilerOptions() {
        compilerOptions {
            optIn.add("kotlin.io.path.ExperimentalPathApi")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
        }
    }

    fun HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>.configureJvmCompilerOption() {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        }
    }

    plugins.withType<KotlinMultiplatformPluginWrapper> {
        extensions.configure<KotlinMultiplatformExtension> {
            configureCommonCompilerOptions()
            targets.withType<KotlinJvmTarget> {
                configureJvmCompilerOption()
            }
        }
    }

    plugins.withType<KotlinPluginWrapper> {
        extensions.configure<KotlinJvmProjectExtension> {
            configureCommonCompilerOptions()
            configureJvmCompilerOption()
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
