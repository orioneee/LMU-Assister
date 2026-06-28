package com.orioooneee.lmuasister.data.steam

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.kSteam
import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import com.orioooneee.lmuasister.analytics.installPerformanceMonitoring
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okio.Path.Companion.toPath
import steam.enums.EAuthTokenPlatformType

internal actual fun createKSteamClient(): SteamClient {
    val context = AndroidAppContext.value
        ?: error("initSteamStorage(context) must be called before Steam sign-in.")

    ensureBouncyCastleProvider()

    return kSteam {
        rootFolder = context.filesDir.resolve("ksteam").absolutePath.toPath(normalize = true)
        persistenceDriver = MemoryPersistenceDriver
        deviceInfo = DeviceInformation(
            osType = EOSType.k_eAndroidUnknown,
            gamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_Phone,
            deviceName = "LMU Assister Android",
            platformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
        )
        ktor {
            HttpClient(OkHttp) {
                installPerformanceMonitoring()
            }
        }
    }
}
