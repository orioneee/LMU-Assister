package com.orioooneee.lmuasister.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

actual fun platformDemoCredentialsRemoteSource(): DemoCredentialsRemoteSource =
    FirebaseDemoCredentialsRemoteSource()

private class FirebaseDemoCredentialsRemoteSource : DemoCredentialsRemoteSource {
    private val remoteConfig = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build(),
        )
    }

    override fun fetch(keys: List<DemoCredentialKey>, onComplete: (Map<String, String>) -> Unit) {
        remoteConfig.setDefaultsAsync(keys.associate { it.remoteName to "" })
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            // Read the last activated values even when this network refresh fails.
            onComplete(keys.associate { key -> key.remoteName to remoteConfig.getString(key.remoteName) })
        }
    }
}
