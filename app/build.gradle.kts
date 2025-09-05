plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.foragingapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.foragingapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("org.maplibre.gl:android-sdk:9.6.2")
}
