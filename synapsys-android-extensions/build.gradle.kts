plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
}

android {
    namespace = "io.eigr.synapsys.extensions.android.sensors"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
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
                implementation("androidx.room:room-runtime")
                implementation("androidx.room:room-ktx")
                implementation("androidx.sqlite:sqlite-ktx")
            }
        }
    }
}