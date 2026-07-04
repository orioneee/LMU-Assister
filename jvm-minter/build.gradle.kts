plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ksteam.core)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.ktor.client.cio)
    implementation(libs.bouncycastle.prov)
}

application {
    mainClass.set("com.orioooneee.lmuasister.minter.MainKt")
}
