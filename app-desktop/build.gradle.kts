plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
    id("org.jetbrains.compose") version libs.versions.compose.get()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(project(":core-usecase"))
    implementation(project(":core-data"))
    implementation(project(":core-domain"))
    implementation(project(":core-analytics"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.api)
    implementation(libs.pdfbox)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

compose.desktop {
    application {
        mainClass = "id.co.nierstyd.mutugemba.desktop.MainKt"
        nativeDistributions {
            packageName = "MutuGemba"
            packageVersion = "1.2.0"
            description = "MutuGemba - Platform QC TPS Offline"
            vendor = "PT. Primaraya Graha Nusantara"
            windows {
                menuGroup = "MutuGemba"
                shortcut = true
                iconFile.set(project.file("src/main/resources/branding/app_icon.ico"))
            }
        }
    }
}

tasks.register("packageDistribution") {
    group = "distribution"
    dependsOn("packageDistributionForCurrentOS")
}
