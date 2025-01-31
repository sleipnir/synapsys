plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Standard libraries
    implementation(platform(project(":synapsys-bom")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Transport
    implementation("org.zeromq:jeromq")

    // Serialization formats
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf")
    implementation("com.google.code.gson:gson")
    implementation("com.google.protobuf:protobuf-java")
    implementation("io.protostuff:protostuff-core")
    implementation("io.protostuff:protostuff-runtime")

    // Logs
    implementation("org.slf4j:slf4j-api")
}