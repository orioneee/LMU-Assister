package com.orioooneee.lmuasister.remoteconfig

object RemoteDemoCredentials {
    var remoteSource: DemoCredentialsRemoteSource = NoopDemoCredentialsRemoteSource()
}

actual fun platformDemoCredentialsRemoteSource(): DemoCredentialsRemoteSource =
    RemoteDemoCredentials.remoteSource
