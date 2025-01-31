plugins {
    id("application")
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
    implementation(platform(project(":synapsys-bom")))
    implementation(project(":synapsys-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("ch.qos.logback:logback-classic")
}

application {
    mainClass.set("io.eigr.synapsys.examples.MainKt")
}
