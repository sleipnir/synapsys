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
include("synapsys-store-room")
include("synapsys-store-mysql")
include("synapsys-store-postgres")
include("synapsys-benchmarks")