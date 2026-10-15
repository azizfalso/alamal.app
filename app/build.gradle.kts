import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ---- Auto-download Arabic fonts before every build ----
// Google Fonts "Cairo" is a variable font (wght 200-1000), so a single
// .ttf covers all weights we use in Type.kt (Normal/Medium/SemiBold/Bold).
// This task runs before preBuild, so both local builds and CI (GitHub Actions)
// get the font without any manual step.
val downloadArabicFonts by tasks.registering {
    val fontDir = File(projectDir, "src/main/res/font")
    fontDir.mkdirs()

    val fonts = mapOf(
        "cairo.ttf" to
            "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf",
    )

    fonts.forEach { (name, url) ->
        val target = File(fontDir, name)
        if (!target.exists() || target.length() < 100_000L) {
            logger.lifecycle("→ Downloading Arabic font: $name")
            URI(url).toURL().openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            logger.lifecycle("  saved $name (${target.length() / 1024} KB)")
        }
    }
}

tasks.configureEach {
    if (name == "preBuild") {
        dependsOn(downloadArabicFonts)
    }
}

val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val hasKeystore = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasKeystore) keystorePropertiesFile.inputStream().use { load(it) }
}

val versionCodeOverride: Int =
    (project.findProperty("versionCodeOverride") as String?)?.toIntOrNull() ?: 1
val versionNameOverride: String = "2.0.$versionCodeOverride"

android {
    namespace = "com.alamal.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alamal.app"
        minSdk = 32
        targetSdk = 35
        versionCode = versionCodeOverride
        versionName = versionNameOverride

        resourceConfigurations += listOf("ar", "en")

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // NOTE: androidx.lifecycle.process was removed here. It was declared
    // but never used (no ProcessLifecycleOwner references anywhere), so
    // dropping it saves ~50 KB from the APK.
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.ui.tooling)
}
