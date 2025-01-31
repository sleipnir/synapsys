plugins {
    id("java-platform")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        // Standard libraries
        api("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

        // Transport
        api("org.zeromq:jeromq:0.6.0")

        // Logs
        api("org.slf4j:slf4j-api:2.0.16")
        api("ch.qos.logback:logback-classic:1.5.16")

        // Serialization formats
        api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
        api("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.3")
        api("com.google.code.gson:gson:2.10.1")
        api("com.google.protobuf:protobuf-java:3.25.1")
        api("io.protostuff:protostuff-core:1.8.0")
        api("io.protostuff:protostuff-runtime:1.8.0")
    }
}
