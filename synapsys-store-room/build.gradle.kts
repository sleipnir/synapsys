plugins {
    id("com.android.library") version "8.1.4"
    id("org.jetbrains.kotlin.multiplatform") version "1.9.20"
}

android {
    namespace = "io.eigr.synapsys.extensions.store.backends.room"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project.dependencies.platform(project(":synapsys-bom")))
                implementation(project(":synapsys-core"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.room:room-ktx")
                implementation("androidx.sqlite:sqlite-ktx")
            }
        }
    }
}