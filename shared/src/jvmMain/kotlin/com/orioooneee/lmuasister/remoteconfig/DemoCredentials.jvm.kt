package com.orioooneee.lmuasister.remoteconfig

actual fun platformDemoCredentialsRemoteSource(): DemoCredentialsRemoteSource =
    NoopDemoCredentialsRemoteSource()
