plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bambiproj.docscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bambiproj.docscanner"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        // Ship native OpenCV libs only for real-phone ABIs to keep the APK
        // smaller (arm64 covers essentially every modern device).
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Reads EXIF orientation so captured pages aren't rotated in the PDF.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // On-device document edge detection + perspective crop. The native libs
    // are bundled in the APK (loaded via OpenCVLoader.initLocal), so this
    // adds no network access — everything runs locally on the phone.
    implementation("org.opencv:opencv:4.11.0")
}
