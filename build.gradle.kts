plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.detekt)
}

tasks.register("clean").configure {
    delete("build")
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("detekt.yml"))
        source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
    }
}
