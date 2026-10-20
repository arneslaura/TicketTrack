
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

fun localProperty(name: String): String {
    return rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("$name=") }
        ?.substringAfter("=")
        ?.trim()
        .orEmpty()
}

fun buildConfigString(name: String): String {
    val value = localProperty(name)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$value\""
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        properties.load(it)
    }
}

val mapsApiKey = localProperty("MAPS_API_KEY")
val googleWebClientId = localProperty("GOOGLE_WEB_CLIENT_ID")

android {
    namespace = "com.example.tickettrack"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.tickettrack"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField(
            "boolean",
            "MAPS_ENABLED",
            mapsApiKey.isNotBlank().toString()
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${googleWebClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )

        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            buildConfigString("CLOUDINARY_CLOUD_NAME")
        )
        buildConfigField(
            "String",
            "CLOUDINARY_API_KEY",
            buildConfigString("CLOUDINARY_API_KEY")
        )
        buildConfigField(
            "String",
            "CLOUDINARY_API_SECRET",
            buildConfigString("CLOUDINARY_API_SECRET")
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.cloudinary:cloudinary-android:2.5.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
}
