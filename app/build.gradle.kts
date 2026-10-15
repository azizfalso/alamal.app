import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ---- Auto-download Arabic fonts before every build ----
//
// We use Tajawal (static weights), not Cairo.
// Reason: the previous Cairo variable font did not render well on some
// Android devices — glyph weights looked wrong and some Arabic characters
// appeared to fall back to a missing-glyph box. Tajawal ships as separate
// static files, so each weight loads predictably everywhere.
//
// Tajawal has no SemiBold. Type.kt maps SemiBold requests to the Bold file
// so nothing falls back to the system font.
val downloadArabicFonts by tasks.registering {
    val fontDir = File(projectDir, "src/main/res/font")
    fontDir.mkdirs()

    val fonts = mapOf(
        "tajawal_regular.ttf" to
            "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-Regular.ttf",
        "tajawal_medium.ttf" to
            "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-Medium.ttf",
        "tajawal_bold.ttf" to
            "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-Bold.ttf",
    )

    // Remove obsolete fonts from previous versions so they don't linger
    // as dead resources in the APK.
    listOf("cairo.ttf").forEach { obsolete ->
        val f = File(fontDir, obsolete)
        if (f.exists()) {
            f.delete()
            logger.lifecycle("  removed obsolete font: $obsolete")
        }
    }

    fonts.forEach { (name, url) ->
        val target = File(fontDir, name)
        if (!target.exists() || target.length() < 30_000L) {
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
