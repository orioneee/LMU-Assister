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
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
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
val defaultBackendUrl = "http://localhost:8000/api/v1"

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