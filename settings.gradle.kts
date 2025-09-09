pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("http://maplibre.github.io/maplibre-gl-native/maven")
            // ✅ THIS is the correct way to allow insecure protocol in Kotlin DSL
            isAllowInsecureProtocol = true
        }
    }

    plugins {
        id("com.android.application") version "8.1.1"
        id("org.jetbrains.kotlin.android") version "1.9.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("http://maplibre.github.io/maplibre-gl-native/maven")
            // ✅ again here
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "ForagingApp"
include(":app")
