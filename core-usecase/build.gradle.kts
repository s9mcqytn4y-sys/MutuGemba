plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
}

dependencies {
    implementation(project(":core-domain"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
