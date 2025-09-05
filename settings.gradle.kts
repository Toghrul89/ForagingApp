pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven") }
    }
}

rootProject.name = "ForagingApp"
include(":app")
