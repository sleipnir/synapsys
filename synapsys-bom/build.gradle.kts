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

        // Persistence
        api("net.openhft:chronicle-map:3.23.5")
        api("net.openhft:affinity:3.23.3")

        // Logs
        api("org.slf4j:slf4j-api:2.0.16")
        api("ch.qos.logback:logback-classic:1.5.16")

        // Serialization formats
        api("com.google.code.gson:gson:2.10.1")
        api("com.google.protobuf:protobuf-java:3.25.1")
        api("io.protostuff:protostuff-core:1.8.0")
        api("io.protostuff:protostuff-runtime:1.8.0")

        // Tests
        api("org.jetbrains.kotlin:kotlin-test:1.9.0")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        api("io.mockk:mockk:1.13.16")
        api("org.junit.jupiter:junit-jupiter:5.11.0")

        // Databases
        api("androidx.room:room-runtime:2.7.0-alpha13")
        api("androidx.room:room-ktx:2.7.0-alpha13")
        api("androidx.sqlite:sqlite-ktx:2.5.0-alpha13")
        api("org.xerial:sqlite-jdbc:3.42.0.0")
        api("mysql:mysql-connector-java:8.0.33")
        api("org.postgresql:postgresql:42.7.5")
        api("com.zaxxer:HikariCP:6.2.1")

        // android
        api("androidx.room:room-compiler:2.7.0-alpha13")
    }
}