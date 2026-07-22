plugins {
    id("dev.bnorm.arcade.arcade-driver")
}

arcade {
    className = "Kodee"
}

// TODO include build dependency substitution doesn't seem to work automatically?
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":arcade-player"))
        }
    }
}
