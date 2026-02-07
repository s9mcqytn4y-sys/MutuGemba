import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import java.io.File
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.compose") version libs.versions.compose.get() apply false
    id("org.jlleitschuh.gradle.ktlint") version libs.versions.ktlintPlugin.get() apply false
    id("io.gitlab.arturbosch.detekt") version libs.versions.detekt.get() apply false
    id("app.cash.sqldelight") version libs.versions.sqldelight.get() apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    extensions.configure<KtlintExtension> {
        verbose.set(false)
        outputToConsole.set(true)
        filter {
            exclude {
                val buildDir = project.layout.buildDirectory.get().asFile
                it.file.path.contains("${buildDir}${File.separator}generated")
            }
        }
    }

    tasks.withType<KtLintCheckTask>().configureEach {
        exclude("**/build/generated/**")
    }

    tasks.withType<KtLintFormatTask>().configureEach {
        exclude("**/build/generated/**")
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files("$rootDir/detekt.yml"))
        baseline = file("$rootDir/detekt-baseline-${project.name}.xml")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register("ktlintCheck") {
    group = "verification"
    dependsOn(
        ":app-desktop:ktlintCheck",
        ":core-domain:ktlintCheck",
        ":core-usecase:ktlintCheck",
        ":core-data:ktlintCheck",
        ":core-analytics:ktlintCheck",
    )
}

tasks.register("ktlintFormat") {
    group = "formatting"
    dependsOn(
        ":app-desktop:ktlintFormat",
        ":core-domain:ktlintFormat",
        ":core-usecase:ktlintFormat",
        ":core-data:ktlintFormat",
        ":core-analytics:ktlintFormat",
    )
}

tasks.register("detektAll") {
    group = "verification"
    dependsOn(
        ":app-desktop:detekt",
        ":core-domain:detekt",
        ":core-usecase:detekt",
        ":core-data:detekt",
        ":core-analytics:detekt",
    )
}

tasks.register("formatAll") {
    dependsOn("ktlintFormat")
}

tasks.register("lintAll") {
    dependsOn("ktlintCheck", "detektAll")
}
