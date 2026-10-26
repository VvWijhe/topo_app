plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase (online ranglijst): de gegevens komen uit app/google-services.json, als die er is.
// Zonder bestand blijft de app offline. Zelf inlezen i.p.v. de google-services-plugin,
// zodat ook de dev-versie (ander pakket-ID) hetzelfde Firebase-project gebruikt.
val firebaseConfig: Map<String, String> = runCatching {
    @Suppress("UNCHECKED_CAST")
    val root = groovy.json.JsonSlurper().parse(file("google-services.json")) as Map<String, Any?>
    val info = root["project_info"] as Map<String, Any?>
    val clients = root["client"] as List<Map<String, Any?>>
    val client = clients.firstOrNull {
        ((it["client_info"] as Map<*, *>)["android_client_info"] as Map<*, *>)["package_name"] == "nl.vanwijhe.lodestar"
    } ?: clients.first()
    mapOf(
        "projectId" to info["project_id"] as String,
        "appId" to (client["client_info"] as Map<*, *>)["mobilesdk_app_id"] as String,
        "apiKey" to ((client["api_key"] as List<Map<*, *>>).first()["current_key"] as String),
    )
}.getOrDefault(emptyMap())

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
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebaseConfig["projectId"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${firebaseConfig["appId"].orEmpty()}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${firebaseConfig["apiKey"].orEmpty()}\"")
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
        buildConfig = true
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register("printVersionName") {
    doLast { println(topoVersionName) }
}
