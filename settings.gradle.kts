pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }
    }
}

rootProject.name = "ForagingApp"
include(":app")
