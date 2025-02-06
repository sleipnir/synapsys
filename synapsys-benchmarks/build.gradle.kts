plugins {
    id("me.champeau.jmh") version "0.7.3"
}

repositories {
    mavenCentral()
}

sourceSets.main {
    java.srcDirs("src")
}
sourceSets.test {
    java.srcDirs("test")
}

dependencies {
    jmhImplementation(platform(project(":synapsys-bom")))
    jmhImplementation(project(":synapsys-core"))
    jmhImplementation("org.jetbrains.kotlin:kotlin-stdlib")
    jmhImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    jmhImplementation("ch.qos.logback:logback-classic")
    jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    warmupIterations.set(3)
    iterations.set(10)
    fork.set(1)
    benchmarkMode.set(listOf("Throughput"))
    verbosity = "EXTRA"
    includes = listOf("io.eigr.synapsys.benchmarks.SynapsysBenchmark.kt")
}