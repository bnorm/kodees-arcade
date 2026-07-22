plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.plugin)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    website.set("https://github.com/bnorm/kodees-arcade")
    vcsUrl.set("https://github.com/bnorm/kodees-arcade.git")
    plugins {
        create("ArcadeDriverPlugin") {
            id = "dev.bnorm.arcade.arcade-driver"
            displayName = "Kodee's Arcade Driver Plugin"
            implementationClass = "dev.bnorm.arcade.gradle.player.ArcadeDriverPlugin"
        }
    }
}
