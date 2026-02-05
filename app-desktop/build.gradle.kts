plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
    id("org.jetbrains.compose") version libs.versions.compose.get()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":core-usecase"))
    implementation(project(":core-data"))
    implementation(project(":core-domain"))
    runtimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

compose.desktop {
    application {
        mainClass = "id.co.nierstyd.mutugemba.desktop.MainKt"
    }
}

tasks.register("packageDistribution") {
    group = "distribution"
    dependsOn("packageDistributionForCurrentOS")
}
