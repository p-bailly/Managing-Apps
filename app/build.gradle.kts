plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Numéro de version = nombre de commits : le même en local et sur GitHub, et toujours croissant
// (une mise à jour doit avoir un numéro plus grand que la version installée).
val commitCount: Int = runCatching {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

android {
    namespace = "fr.perso.managingapps"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.perso.managingapps"
        minSdk = 28
        targetSdk = 35
        versionCode = commitCount
        versionName = "1.0.$commitCount"
    }

    signingConfigs {
        // Clé personnelle volontairement versionnée (comme pour NBA, Scan et Météo) : tous les APK,
        // construits sur GitHub ou en local, ont la même signature et s'installent par-dessus.
        create("release") {
            storeFile = file("managing-apps.keystore")
            storePassword = "apps-perso"
            keyAlias = "apps"
            keyPassword = "apps-perso"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Vraie implémentation de org.json pour les tests JVM (celle d'android.jar n'est qu'un bouchon).
    testImplementation("org.json:json:20240303")
}
