import java.io.File
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id("app.cash.sqldelight") version libs.versions.sqldelight.get()
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.sqldelight.driver)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okio)
    implementation(libs.slf4j.api)
    implementation(libs.sqlite.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

sqldelight {
    databases {
        create("MutuGembaDatabase") {
            packageName.set("id.co.nierstyd.mutugemba.storage.db")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

tasks.withType<KtLintCheckTask>().configureEach {
    exclude { it.file.path.contains("${'$'}{project.buildDir}${'$'}{File.separator}generated") }
    setSource(files("src/main/kotlin"))
}

tasks.withType<KtLintFormatTask>().configureEach {
    exclude { it.file.path.contains("${'$'}{project.buildDir}${'$'}{File.separator}generated") }
    setSource(files("src/main/kotlin"))
}
