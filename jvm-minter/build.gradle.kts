plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

val lmuVersionCode = providers.gradleProperty("lmu.versionCode").get().trim().toInt()

val generateMinterBuildConfig by tasks.registering {
    inputs.property("versionCode", lmuVersionCode)
    outputs.dir(layout.buildDirectory.dir("generated/buildconfig/kotlin"))
    doLast {
        val versionCode = inputs.properties["versionCode"]
        val pkgDir = outputs.files.singleFile.resolve("com/orioooneee/lmuasister/minter")
        pkgDir.mkdirs()
        pkgDir.resolve("MinterBuildConfig.kt").writeText(
            """
            |package com.orioooneee.lmuasister.minter
            |
            |internal object MinterBuildConfig {
            |    const val VERSION_CODE: Int = $versionCode
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets.named("main") {
        kotlin.srcDir(generateMinterBuildConfig)
    }
}

dependencies {
    implementation(libs.javasteam)
    implementation("com.google.protobuf:protobuf-java:4.31.1")
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.ktor.client.cio)
    implementation(libs.bouncycastle.prov)
}

application {
    mainClass.set("com.orioooneee.lmuasister.minter.MainKt")
}
