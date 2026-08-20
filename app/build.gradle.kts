import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Signier-Konfiguration aus keystore.properties (nicht im Git). Fehlt die Datei,
// bleibt der Release-Build unsigniert (z. B. auf fremden Rechnern).
val keystorePropsFile = rootProject.file("keystore.properties")
val hasKeystore = keystorePropsFile.exists()
val keystoreProps = Properties().apply {
    if (hasKeystore) load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "de.kewl.fullscreendy"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.kewl.fullscreendy"
        minSdk = 28
        targetSdk = 35
        versionCode = 21
        versionName = "0.4.8"
        buildConfigField("boolean", "DEV", "false") // Default für alle Varianten; dev überschreibt
        buildConfigField("boolean", "UPDATER", "true") // Default; das fdroid-Flavor schaltet ab
    }

    // Der von AGP eingebettete, Google-signierte Abhaengigkeits-Metadatenblock ist bei
    // jedem Build anders und macht reproduzierbare Builds unmoeglich – deshalb raus.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // F-Droid baut und signiert selbst und nimmt keine Apps, die sich über einen
    // eigenen APK-Download aktualisieren. Deshalb zwei Varianten: "github" mit
    // In-App-Update, "fdroid" ohne Updater und ohne REQUEST_INSTALL_PACKAGES
    // (die Berechtigung steht in src/github/AndroidManifest.xml).
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER", "true")
        }
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER", "false")
        }
    }

    // Ohne keystore.properties (z. B. auf dem F-Droid-Buildserver) gibt es gar keine
    // Signier-Konfiguration – der Build läuft dann durch und liefert ein unsigniertes APK.
    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Keine Git-/VCS-Infos ins APK schreiben – das ist die einzige nicht
            // reproduzierbare Datei (version-control-info.textproto).
            vcsInfo {
                include = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Dev-Build: gleicher Keystore/R8, aber Version "-dev.N" und BuildConfig.DEV=true.
        // Bauen: gradle assembleDev -PdevNum=N
        create("dev") {
            initWith(getByName("release"))
            val devNum = (project.findProperty("devNum") ?: "1").toString()
            versionNameSuffix = "-dev.$devNum"
            buildConfigField("boolean", "DEV", "true")
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.paho.mqtt)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.androidx.swiperefreshlayout)
    debugImplementation(libs.androidx.ui.tooling)
}
