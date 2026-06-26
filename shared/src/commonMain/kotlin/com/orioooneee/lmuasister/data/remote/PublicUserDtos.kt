package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

@Serializable
data class UsersSummaryResponse(
    val count: Int = 0,
    val distribution: UsersDistributionDto = UsersDistributionDto(),
    @SerialName("top_safety") val topSafety: List<PublicUserDto> = emptyList(),
)

@Serializable
data class UsersDistributionDto(
    @SerialName("driver_rating") val driverRating: Map<String, RatingDistributionBucketDto> = emptyMap(),
    @SerialName("safety_rating") val safetyRating: Map<String, RatingDistributionBucketDto> = emptyMap(),
)

@Serializable
data class RatingDistributionBucketDto(
    val count: Int = 0,
    val percentage: Double = 0.0,
)

@Serializable
data class UsersSearchResponse(
    val query: String = "",
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val count: Int = 0,
    val total: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
    val users: List<PublicUserDto> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PublicUserDto(
    val uid: String,
    val name: String? = null,
    val nationality: String? = null,
    val badge: String? = null,
    @SerialName("badge_url") @JsonNames("badgeUrl") val badgeUrl: String? = null,
    val source: String? = null,
    @SerialName("source_label") val sourceLabel: String? = null,
    @SerialName("external_data") val externalData: Boolean = false,
    @SerialName("external_url") val externalUrl: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val team: String? = null,
    @SerialName("driver_rating") val driverRating: RatingDto? = null,
    @SerialName("safety_rating") val safetyRating: RatingDto? = null,
    val races: Int? = null,
    val wins: Int? = null,
    val podiums: Int? = null,
    @SerialName("pole_positions") val polePositions: Int? = null,
    @SerialName("fastest_laps") val fastestLaps: Int? = null,
    @SerialName("synced_at") val syncedAt: String? = null,
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
)
