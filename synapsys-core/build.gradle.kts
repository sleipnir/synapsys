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

tasks.test {
    useJUnitPlatform()
}

dependencies {
    // Standard libraries
    implementation(platform(project(":synapsys-bom")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Transport
    implementation("org.zeromq:jeromq")

    // Persistence
    implementation("net.openhft:chronicle-map")
    implementation("net.openhft:affinity")

    // Serialization formats
    implementation("com.google.code.gson:gson")
    implementation("com.google.protobuf:protobuf-java")
    implementation("io.protostuff:protostuff-core")
    implementation("io.protostuff:protostuff-runtime")

    // Logs
    implementation("org.slf4j:slf4j-api")

    // Tests
    api("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.mockk:mockk")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.qos.logback:logback-classic")
}