package com.orioooneee.lmuasister.featureflags

object RemoteFeatureFlags {
    var remoteSource: FeatureFlagRemoteSource = NoopFeatureFlagRemoteSource()
}

actual fun platformFeatureFlagRemoteSource(): FeatureFlagRemoteSource = RemoteFeatureFlags.remoteSource
