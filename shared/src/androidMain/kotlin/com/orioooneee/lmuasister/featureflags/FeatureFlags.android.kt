package com.orioooneee.lmuasister.featureflags

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

actual fun platformFeatureFlagRemoteSource(): FeatureFlagRemoteSource = FirebaseFeatureFlagRemoteSource()

private class FirebaseFeatureFlagRemoteSource : FeatureFlagRemoteSource {
    private val remoteConfig = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build(),
        )
    }

    override fun fetch(keys: List<FeatureFlagKey>, onComplete: (Map<String, Boolean>) -> Unit) {
        val defaults = keys.associate { it.remoteName to it.defaultValue }
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(emptyMap())
                    return@addOnCompleteListener
                }

                onComplete(keys.associate { key -> key.remoteName to remoteConfig.getBoolean(key.remoteName) })
            }
    }
}
