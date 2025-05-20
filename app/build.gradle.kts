import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms.google.services)
    alias (libs.plugins.crashlytics)
}

val localProperties = Properties().apply{
    load(File(rootDir, "local.properties").inputStream())
}

val googleMapsApiKey: String = localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""

android {
    namespace = "com.example.inpath"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.inpath"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        resValue("string", "google_maps_key", "\"$googleMapsApiKey\"")
    }

    signingConfigs {
        create("custom"){
            storeFile = file("C:/Users/gabri/Desktop/APPS2/PRACTICA_1/InPath/app/keystore/InPath.jks")
            storePassword = "mypass"
            keyAlias = "myalias"
            keyPassword = "mypass"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("custom")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("custom")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.maps) //Maps SDK for Android
    implementation(libs.maps.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata)
    //Authentication:
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.googleid)

    implementation(libs.androidx.appcompat)
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.realtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}