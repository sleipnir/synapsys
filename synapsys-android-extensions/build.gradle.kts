import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.Companion.instrumentedTest
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree.Companion.instrumentedTest

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
}

android {
    namespace = "io.eigr.synapsys.extensions.android.sensors"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

        /*instrumentedTest {
            dependencies {
                implementation("androidx.test:core:1.5.0")
                implementation("org.robolectric:robolectric:4.9")
            }
        }*/
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
                implementation("org.tinylog:slf4j-tinylog:2.6.2")
                implementation("org.tinylog:tinylog-impl:2.6.2")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.11.1")
                implementation("androidx.test:core:1.6.1")
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("androidx.test:runner:1.6.2")
                implementation("androidx.test:rules:1.6.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
                implementation("org.slf4j:slf4j-api:2.0.16")
                implementation("org.tinylog:slf4j-tinylog:2.6.2")
                implementation("org.tinylog:tinylog-impl:2.6.2")
            }
        }
    }
}