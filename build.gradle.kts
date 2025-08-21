buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven/") } // ✅ Needed for MapLibre
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath(kotlin("gradle-plugin", version = "1.9.10"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maplibre.org/maven/") } // ✅ Needed here too
    }
}
