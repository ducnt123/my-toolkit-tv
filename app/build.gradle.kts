plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tk.toolkit.tv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tk.toolkit.tv"
        minSdk = 28
        // Target Android 12L (API 32) so sideloaded APK installs on Android 13/14 Google TV
        // are not blocked from enabling the Accessibility Service by Restricted Settings.
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 32
        versionCode = 12
        versionName = "1.4.7"

    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore/toolkit-signing.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("keystore/toolkit-signing.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Dedicated project-pinned keystore so updates always install cleanly over existing builds
            // across any machine or CI environment.
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}