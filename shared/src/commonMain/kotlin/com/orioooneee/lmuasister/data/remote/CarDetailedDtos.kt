package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** GET /api/v3/cars/detailed — full public car catalogue with artwork and liveries. */
@Serializable
data class CarsDetailedResponse(
    val count: Int = 0,
    val cars: List<CarDetailedDto> = emptyList(),
)

@Serializable
data class CarDetailedDto(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val manufacturer: String = "",
    @SerialName("manufacturer_logo_url") val manufacturerLogoUrl: String = "",
    val description: String = "",
    val category: String = "",
    val hero: CarImageDto = CarImageDto(),
    @SerialName("hero_image_url") val heroImageUrl: String = "",
    val gallery: List<CarGalleryImageDto> = emptyList(),
    @SerialName("gallery_images") val galleryImages: List<String> = emptyList(),
    val specs: CarSpecsDto = CarSpecsDto(),
    @SerialName("tech_specs") val techSpecs: Map<String, String> = emptyMap(),
    val raw: JsonObject = JsonObject(emptyMap()),
    @SerialName("livery_urls") val liveryUrls: List<String> = emptyList(),
    val liveries: List<CarLiveryItemDto> = emptyList(),
    @SerialName("livery_count") val liveryCount: Int = 0,
)

@Serializable
data class CarImageDto(
    val url: String = "",
)

@Serializable
data class CarGalleryImageDto(
    val url: String = "",
    val index: Int = 0,
)

@Serializable
data class CarSpecsDto(
    val engine: String = "",
    val power: String = "",
    val weight: String = "",
    val length: String = "",
    val width: String = "",
    val height: String = "",
    val transmission: String = "",
    @SerialName("best_result") val bestResult: String = "",
)

@Serializable
data class CarLiveryItemDto(
    val id: String = "",
    val name: String = "",
    val series: String = "",
    @SerialName("vehicle_code") val vehicleCode: String = "",
    @SerialName("vehicle_version") val vehicleVersion: String = "",
    @SerialName("veh_file") val vehFile: String = "",
    @SerialName("dlc_app_id") val dlcAppId: Long? = null,
    val owned: Boolean = false,
    @SerialName("mapped_asset_name") val mappedAssetName: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val url: String = "",
)

fun CarDetailedDto.heroUrl(): String? =
    hero.url.takeIf { it.isNotBlank() } ?: heroImageUrl.takeIf { it.isNotBlank() }

fun CarDetailedDto.galleryUrls(): List<String> =
    (gallery.sortedBy { it.index }.mapNotNull { it.url.takeIf(String::isNotBlank) } + galleryImages)
        .distinct()

fun CarDetailedDto.displayId(): String = slug.ifBlank { id }

fun CarLiveryItemDto.image(): String? =
    imageUrl.takeIf { it.isNotBlank() } ?: url.takeIf { it.isNotBlank() }
