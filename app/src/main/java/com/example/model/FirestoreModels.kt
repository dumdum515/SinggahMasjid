package com.example.model

data class CustomMosque(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val baseFacilities: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class MosqueReview(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val mosqueId: String = "",
    val mosqueName: String = "",
    val rating: Long = 5L,
    val facilities: List<String> = emptyList(),
    val comment: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class FavoriteMosque(
    val id: String = "",
    val userId: String = "",
    val mosqueId: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class MosqueSource(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val baseFacilities: List<String>,
    val customOwnerId: String? = null
)

data class MosqueUiItem(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double,
    val avgRating: Double,
    val reviewCount: Int,
    val facilityCounts: Map<String, Int>,
    val verifiedFacilities: List<String>,
    val reviews: List<MosqueReview>,
    val favorite: Boolean,
    val customAdded: Boolean,
    val customOwnerId: String?
)

enum class MosqueSortOption(val label: String) {
    DISTANCE("Terdekat"),
    RATING("Rating Tertinggi"),
    FACILITIES("Fasilitas Terlengkap")
}
