plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("org.apache.pdfbox:pdfbox:2.0.29")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    implementation("org.telegram:telegrambots:6.8.0")
 }

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}