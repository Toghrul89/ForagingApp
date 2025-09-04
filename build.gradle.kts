
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.1")
        classpath(kotlin("gradle-plugin", version = "1.9.10"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
