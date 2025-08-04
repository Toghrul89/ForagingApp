// Root-level build.gradle.kts
plugins {
    // This plugin lets you use Kotlin in the Gradle build script
    kotlin("jvm") version "1.9.0" apply false
    id("com.android.application") version "8.3.0" apply false
    id("com.android.library") version "8.3.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
