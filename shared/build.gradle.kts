import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        // kotlin.time.Instant / Clock (used since kotlinx-datetime 0.7) are still experimental on 2.4
        optIn.add("kotlin.time.ExperimentalTime")
        // We use expect/actual classes & objects (e.g. LocalCache) — opt out of the Beta warning.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // We add a custom intermediate source set (tunnelMain) with manual dependsOn,
    // which disables auto-application of the default hierarchy — re-apply it explicitly
    // so iosMain/jvmMain/etc. keep their standard link to commonMain.
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

    androidLibrary {
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
        // Shared Steam device-tunnel (Ktor raw sockets + websockets) — used by all
        // (Android/JVM/iOS) clients.
        val tunnelMain = create("tunnelMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.network)
            }
        }
        androidMain {
            dependsOn(tunnelMain)
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.ktor.client.okhttp)
                // Encrypted token storage for the persisted Steam session.
                implementation(libs.androidx.security.crypto)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)

            // Networking + DI + HTML parsing + images
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
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
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain {
            dependsOn(tunnelMain)
            dependencies {
                implementation(libs.ktor.client.cio)
                // OkHttp engine for the tunnel WebSocket (CIO's WS has Cloudflare quirks).
                implementation(libs.ktor.client.okhttp)
            }
        }
        iosMain {
            dependsOn(tunnelMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// ── Backend base URL → generated BuildConfig (read from local.properties) ──
val localPropsFile = rootProject.layout.projectDirectory.file("local.properties")
// Non-secret fallback only — the real (deployed) backend URL lives in local.properties
// (git-ignored) as `backend.url=…`, so the host is never committed.
val defaultBackendUrl = "http://localhost:8000/api/v2"

val generateBuildConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/buildconfig/kotlin")
    val lp = localPropsFile.asFile
    val fallback = defaultBackendUrl
    // Tracked so editing local.properties re-runs the task (configuration cache safe).
    inputs.file(localPropsFile).optional(true).withPropertyName("localProperties")
    outputs.dir(outDir)
    doLast {
        val props = Properties()
        if (lp.exists()) lp.inputStream().use { stream -> props.load(stream) }
        val url = (props.getProperty("backend.url") ?: fallback).trim().trimEnd('/')
        val pkgDir = outDir.get().asFile.resolve("com/orioooneee/lmuasister/config")
        pkgDir.mkdirs()
        pkgDir.resolve("BuildConfig.kt").writeText(
            """
            |package com.orioooneee.lmuasister.config
            |
            |/** Generated from local.properties (backend.url) — do not edit. */
            |internal object BuildConfig {
            |    const val BACKEND_URL: String = "$url"
            |}
            |
            """.trimMargin(),
        )
    }
}

// Make the generated file part of commonMain (visible to every target) and ensure
// it's written before any Kotlin compilation runs.
kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generateBuildConfig)
}