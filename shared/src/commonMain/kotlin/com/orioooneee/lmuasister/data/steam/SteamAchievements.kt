package com.orioooneee.lmuasister.data.steam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SteamAchievements(
    val appId: Int = LMU_APP_ID,
    val total: Int = 0,
    val unlocked: Int = 0,
    val achievements: List<SteamAchievement> = emptyList(),
) {
    val progress: Float
        get() = if (total <= 0) 0f else unlocked.toFloat() / total.toFloat()
}

@Serializable
data class SteamAchievement(
    val name: String,
    val description: String? = null,
    @SerialName("achieved_image_url") val achievedImageUrl: String? = null,
    @SerialName("locked_image_url") val lockedImageUrl: String? = null,
    val achieved: Boolean = false,
    @SerialName("unlock_time") val unlockTime: Long = 0L,
) {
    val imageUrl: String?
        get() = if (achieved) achievedImageUrl ?: lockedImageUrl else lockedImageUrl ?: achievedImageUrl
}

interface SteamAchievementsClient {
    suspend fun achievements(appId: Int = LMU_APP_ID): SteamAchievements
}

object UnsupportedSteamAchievementsClient : SteamAchievementsClient {
    override suspend fun achievements(appId: Int): SteamAchievements =
        throw UnsupportedOperationException("Steam achievements are not available on this platform.")
}
