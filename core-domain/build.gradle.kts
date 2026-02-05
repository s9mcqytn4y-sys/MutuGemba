plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
