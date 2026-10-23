plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Versie: <major>.<aantal commits sinds de basis>. Elke update krijgt zo automatisch een hoger nummer.
// Voor 1.0: zet topo.versionMajor=1 en topo.versionBase op het aantal commits van dat moment (gradle.properties).
val commitCount: Int = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)
val versionMajor = (findProperty("topo.versionMajor") as String?)?.toIntOrNull() ?: 0
val versionBase = (findProperty("topo.versionBase") as String?)?.toIntOrNull() ?: 0
val topoVersionName = "$versionMajor.${(commitCount - versionBase).coerceAtLeast(0)}"

android {
    namespace = "nl.topo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.topo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = commitCount
        versionName = topoVersionName
    }

    // Vaste sleutel, zodat elke nieuwe versie over de vorige heen te installeren is (met behoud van voortgang).
    // Alleen voor eigen gebruik; voor de Play Store komt er een echte, geheime sleutel.
    signingConfigs {
        getByName("debug") {
            storeFile = file("topo-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Tijdelijk: release ondertekenen met de debug-sleutel, zodat je hem zonder eigen sleutel kunt installeren.
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register("printVersionName") {
    doLast { println(topoVersionName) }
}
