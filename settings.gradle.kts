pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "synapsys"
include("synapsys-bom")
include("synapsys-core")
include("synapsys-examples")
include("synapsys-benchmarks")