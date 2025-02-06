plugins {
    id("base")
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.android.library") version "8.1.4" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.1.0" apply false
}

allprojects {
    group = "io.eigr"
    version = "0.1.2"
}