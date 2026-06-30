import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.ktor) apply false
}

allprojects {
    group = "dev.bnorm.arcade"
    version = "1.0.0"

    val javaVersion = JavaVersion.VERSION_21

    fun HasConfigurableKotlinCompilerOptions<*>.configureCommonCompilerOptions() {
        compilerOptions {
            optIn.add("kotlin.io.path.ExperimentalPathApi")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")

            optIn.add("androidx.compose.ui.ExperimentalComposeUiApi")
            optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
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

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register<Sync>("site") {
    into(project.layout.buildDirectory.dir("_site"))
    from(project(":arcade-machine:arcade-app").tasks.named("wasmJsBrowserDistribution"))
}
