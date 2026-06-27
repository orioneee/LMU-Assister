package com.orioooneee.lmuasister.data.steam

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.kSteam
import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import okio.Path.Companion.toPath
import steam.enums.EAuthTokenPlatformType

internal actual fun createKSteamClient(): SteamClient {
    val home = System.getProperty("user.home") ?: "."
    val root = "$home/.lmuassister/ksteam"

    ensureBouncyCastleProvider()

    return kSteam {
        rootFolder = root.toPath(normalize = true)
        persistenceDriver = MemoryPersistenceDriver
        deviceInfo = DeviceInformation(
            osType = EOSType.k_WinUnknown,
            gamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC,
            deviceName = "LMU Assister Desktop",
            platformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
        )
    }
}
