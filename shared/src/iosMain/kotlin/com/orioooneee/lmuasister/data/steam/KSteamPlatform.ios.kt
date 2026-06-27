package com.orioooneee.lmuasister.data.steam

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.kSteam
import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import steam.enums.EAuthTokenPlatformType

@OptIn(ExperimentalForeignApi::class)
internal actual fun createKSteamClient(): SteamClient {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("Could not resolve iOS documents directory.")

    return kSteam {
        rootFolder = "$documents/ksteam".toPath(normalize = true)
        persistenceDriver = MemoryPersistenceDriver
        deviceInfo = DeviceInformation(
            osType = EOSType.k_eIOSUnknown,
            gamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_Phone,
            deviceName = "LMU Assister iOS",
            platformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
        )
        ktor { HttpClient(Darwin) }
    }
}
