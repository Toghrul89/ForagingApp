pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.maplibre.org") // MapLibre repo
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.maplibre.org") // MapLibre repo
    }
}
rootProject.name = "ForagingApp"
include(":app")
