// Root build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath(kotlin("gradle-plugin", version = "1.9.0"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }
    }
}
