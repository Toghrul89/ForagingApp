// Top-level Gradle file
plugins {
    kotlin("jvm") version "1.9.10" apply false
    id("com.android.application") version "8.1.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
