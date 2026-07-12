import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val lmuVersionCode = providers.gradleProperty("lmu.versionCode").get().trim().toInt()
val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties").asFile
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun Properties.localString(name: String): String? =
    getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }

val androidSigningStoreFile =
    localProperties.localString("android.signing.storeFile") ?: "/Users/admin/StudioProjects/Untitled.jks"
val androidSigningStoreType =
    localProperties.localString("android.signing.storeType") ?: "PKCS12"
val androidSigningStorePassword =
    localProperties.localString("android.signing.storePassword").orEmpty()
val androidSigningKeyAlias =
    localProperties.localString("android.signing.keyAlias") ?: "key0"
val androidSigningKeyPassword =
    localProperties.localString("android.signing.keyPassword") ?: androidSigningStorePassword

if (androidSigningStorePassword.isBlank()) {
    logger.warn(
        "Android signing uses $androidSigningStoreFile / $androidSigningKeyAlias, " +
            "but android.signing.storePassword is missing in ${localPropertiesFile.path}.",
    )
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Reads androidApp/google-services.json and wires Firebase init + uploads the
    // R8 mapping file to Crashlytics on release builds.
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerformance)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(libs.firebase.appcheck.debug)
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    implementation(libs.firebase.appcheck.playintegrity)
}

android {
    namespace = "com.orioooneee.lmuasister"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.orioooneee.lmuasister"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = lmuVersionCode
        versionName = "1.0.0"
    }
    signingConfigs {
        create("lmu") {
            storeFile = file(androidSigningStoreFile)
            storeType = androidSigningStoreType
            storePassword = androidSigningStorePassword
            keyAlias = androidSigningKeyAlias
            keyPassword = androidSigningKeyPassword
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        configureEach {
            signingConfig = signingConfigs.getByName("lmu")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
