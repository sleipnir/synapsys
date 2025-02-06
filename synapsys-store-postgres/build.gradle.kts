plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(platform(project(":synapsys-bom")))
    implementation(project(":synapsys-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation("ch.qos.logback:logback-classic")
}