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
    compileSdk = 36

    defaultConfig {
        applicationId = "nl.vanwijhe.lodestar"
        minSdk = 26
        targetSdk = 36
        versionCode = commitCount
        versionName = topoVersionName
    }

    // Vaste sleutel, zodat elke nieuwe versie over de vorige heen te installeren is (met behoud van voortgang).
    // Alleen voor eigen gebruik (de APK van GitHub).
    signingConfigs {
        getByName("debug") {
            storeFile = file("topo-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Upload-sleutel voor de Play Store. Het bestand is versleuteld met een sterk wachtwoord,
        // dat alleen als GitHub-secret bestaat (UPLOAD_KEY_PASSWORD).
        System.getenv("UPLOAD_KEY_PASSWORD")?.takeIf { it.isNotBlank() }?.let { pw ->
            create("upload") {
                storeFile = file("lodestar-upload.jks")
                storePassword = pw
                keyAlias = "upload"
                keyPassword = pw
            }
        }
    }

    buildTypes {
        debug {
            // Eigen ID, zodat de GitHub-versie en de Play Store-versie naast elkaar kunnen staan.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            // Geen code-verkleining: veiliger (geen R8-verrassingen), de app wordt er maar iets groter van.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
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
