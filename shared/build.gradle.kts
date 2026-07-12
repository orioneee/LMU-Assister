import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    compilerOptions {
        // kotlin.time.Instant / Clock (used since kotlinx-datetime 0.7) are still experimental on 2.4
        optIn.add("kotlin.time.ExperimentalTime")
        // We use expect/actual classes & objects (e.g. LocalCache) — opt out of the Beta warning.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Keep the standard hierarchy explicit so iosMain/jvmMain/etc. stay linked to commonMain.
    applyDefaultHierarchyTemplate()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()

    wasmJs {
        browser()
    }

    android {
       namespace = "com.orioooneee.lmuasister.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        val commonMain by getting
        val nativeSteamMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ksteam.core)
            }
        }

        androidMain {
            dependsOn(nativeSteamMain)
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.firebase.appcheck.playintegrity)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.bouncycastle.prov)
                // Encrypted storage for legacy/app-managed Steam session data.
                implementation(libs.androidx.security.crypto)
                // Firebase Analytics + Crashlytics + Performance (Android-only telemetry backend).
                // `api` so the app module's Crashlytics Gradle plugin sees the dependency
                // on its runtime classpath. The BoM pins the module versions.
                api(project.dependencies.platform(libs.firebase.bom))
                api(libs.firebase.analytics)
                api(libs.firebase.config)
                api(libs.firebase.crashlytics)
                api(libs.firebase.performance)
            }
        }
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.materialIconsCore)
                implementation(libs.compose.materialIconsExtended)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.coroutinesCore)

                // Networking + DI + HTML parsing + images
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.mock)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.composeViewmodel)
                implementation(libs.ksoup)
                implementation(libs.coil.compose)
                implementation(libs.coil.networkKtor)
                implementation(libs.coil.svg)
                implementation(libs.kotlinx.serializationJson)
                implementation(libs.kotlinx.datetime)
                implementation(libs.navigation.compose)
                implementation(libs.paging.common)
                implementation(libs.paging.compose)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain {
            dependsOn(nativeSteamMain)
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.bouncycastle.prov)
            }
        }
        iosMain {
            dependsOn(nativeSteamMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        wasmJsMain {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// ── Runtime config → generated BuildConfig (read from local.properties) ──
val localPropsFile = rootProject.layout.projectDirectory.file("local.properties")
val defaultAppSite = "https://lmu-assister.com"

val generateBuildConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/buildconfig/kotlin")
    val lp = localPropsFile.asFile
    val fallback = defaultAppSite
    // Tracked so editing local.properties re-runs the task (configuration cache safe).
    inputs.file(localPropsFile).optional(true).withPropertyName("localProperties")
    outputs.dir(outDir)
    doLast {
        val props = Properties()
        if (lp.exists()) lp.inputStream().use { stream -> props.load(stream) }
        val appSite = props.getProperty("app.site")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fallback
        // Mock data is opt-in for local UI/dev work.
        val useMock = props.getProperty("backend.mock")?.trim()?.toBooleanStrictOrNull() ?: false
        // App-store-review demo login — set in local.properties (demo.username /
        // demo.password) to match the backend's DEMO_USERNAME/DEMO_PASSWORD. No defaults:
        // when unset the creds are empty and the demo path simply never triggers.
        val demoUser = props.getProperty("demo.username")?.trim().orEmpty()
        val demoPass = props.getProperty("demo.password")?.trim().orEmpty()
        val companionUrl = props.getProperty("companion.url")?.trim()?.trimEnd('/')
            ?: "http://127.0.0.1:8787"
        val firebaseApiKey = (props.getProperty("firebase.apiKey") ?: props.getProperty("apiKey")).orEmpty().trim()
        val firebaseAuthDomain = (props.getProperty("firebase.authDomain") ?: props.getProperty("authDomain")).orEmpty().trim()
        val firebaseProjectId = (props.getProperty("firebase.projectId") ?: props.getProperty("projectId")).orEmpty().trim()
        val firebaseStorageBucket =
            (props.getProperty("firebase.storageBucket") ?: props.getProperty("storageBucket")).orEmpty().trim()
        val firebaseMessagingSenderId =
            (props.getProperty("firebase.messagingSenderId") ?: props.getProperty("messagingSenderId")).orEmpty().trim()
        val firebaseAppId = (props.getProperty("firebase.appId") ?: props.getProperty("appId")).orEmpty().trim()
        val firebaseMeasurementId =
            (props.getProperty("firebase.measurementId") ?: props.getProperty("measurementId")).orEmpty().trim()
        val firebaseWebAnalyticsEnabled =
            firebaseApiKey.isNotBlank() &&
                firebaseProjectId.isNotBlank() &&
                firebaseAppId.isNotBlank() &&
                firebaseMeasurementId.isNotBlank()
        val pkgDir = outDir.get().asFile.resolve("com/orioooneee/lmuasister/config")
        pkgDir.mkdirs()
        pkgDir.resolve("BuildConfig.kt").writeText(
            """
            |package com.orioooneee.lmuasister.config
            |
            |/** Generated from local.properties — do not edit. */
            |internal object BuildConfig {
            |    const val APP_SITE: String = "${appSite.trimEnd('/')}"
            |    const val USE_MOCK: Boolean = $useMock
            |    const val DEMO_USERNAME: String = "$demoUser"
            |    const val DEMO_PASSWORD: String = "$demoPass"
            |    const val COMPANION_URL: String = "$companionUrl"
            |    const val FIREBASE_WEB_ANALYTICS_ENABLED: Boolean = $firebaseWebAnalyticsEnabled
            |    const val FIREBASE_WEB_API_KEY: String = "$firebaseApiKey"
            |    const val FIREBASE_WEB_AUTH_DOMAIN: String = "$firebaseAuthDomain"
            |    const val FIREBASE_WEB_PROJECT_ID: String = "$firebaseProjectId"
            |    const val FIREBASE_WEB_STORAGE_BUCKET: String = "$firebaseStorageBucket"
            |    const val FIREBASE_WEB_MESSAGING_SENDER_ID: String = "$firebaseMessagingSenderId"
            |    const val FIREBASE_WEB_APP_ID: String = "$firebaseAppId"
            |    const val FIREBASE_WEB_MEASUREMENT_ID: String = "$firebaseMeasurementId"
            |}
            |
            """.trimMargin(),
        )
    }
}

val generateJvmBuildConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/jvm-buildconfig/kotlin")
    val lp = localPropsFile.asFile

    inputs.file(localPropsFile).optional(true)
    outputs.dir(outDir)

    doLast {
        val props = Properties()
        if (lp.exists()) lp.inputStream().use { props.load(it) }

        val debugAppCheck = props.getProperty("debug.app.check")
            ?.trim()
            .orEmpty()

        val pkgDir = outDir.get().asFile.resolve("com/orioooneee/lmuasister/config")
        pkgDir.mkdirs()

        pkgDir.resolve("JvmBuildConfig.kt").writeText(
            """
            |package com.orioooneee.lmuasister.config
            |
            |internal object JvmBuildConfig {
            |    const val DEBUG_APP_CHECK: String = "$debugAppCheck"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir(generateJvmBuildConfig)
}

val generateIosBuildConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/ios-buildconfig/kotlin")
    val lp = localPropsFile.asFile

    inputs.file(localPropsFile).optional(true)
    outputs.dir(outDir)

    doLast {
        val props = Properties()
        if (lp.exists()) lp.inputStream().use { props.load(it) }

        val debugAppCheck = props.getProperty("debug.app.check")
            ?.trim()

        val pkgDir = outDir.get().asFile.resolve("com/orioooneee/lmuasister/config")
        pkgDir.mkdirs()

        pkgDir.resolve("IosBuildConfig.kt").writeText(
            """
            |package com.orioooneee.lmuasister.config
            |
            |internal object IosBuildConfig {
            |    const val DEBUG_APP_CHECK: String = "$debugAppCheck"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin.sourceSets.named("iosMain") {
    kotlin.srcDir(generateIosBuildConfig)
}

// Make the generated file part of commonMain (visible to every target) and ensure
// it's written before any Kotlin compilation runs.
kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generateBuildConfig)
}
