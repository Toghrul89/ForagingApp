// Root-level build.gradle.kts

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.0")
        classpath(kotlin("gradle-plugin", version = "1.9.0"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.maplibre.org/")
    }
}
