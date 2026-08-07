plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jayed.nodistract"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.jayed.nodistract"
        minSdk = 24
        targetSdk = 36
        versionCode = 20260808
        versionName = "2026.8.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}