import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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
    }

    // We add a custom intermediate source set (jvmAndroidMain) with manual dependsOn,
    // which disables auto-application of the default hierarchy — re-apply it explicitly
    // so iosMain/jsMain/etc. keep their standard link to commonMain.
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
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
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
        // Shared Steam device-tunnel (Ktor raw sockets + websockets) — used by the
        // Android/JVM/iOS clients. NOT js/wasm (browsers can't do raw TCP).
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
                // On-device Steam auth (Android keeps JavaSteam for now).
                implementation(libs.javasteam)
                implementation(libs.bouncycastle.prov)
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
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// ── Backend base URL → generated BuildConfig (read from local.properties) ──
val localPropsFile = rootProject.layout.projectDirectory.file("local.properties")
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