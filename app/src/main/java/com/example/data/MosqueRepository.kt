package com.example.data

import android.content.Context
import com.example.data.local.CustomMosqueEntity
import com.example.data.local.FavoriteMosqueEntity
import com.example.data.local.MosqueReviewEntity
import com.example.data.local.SinggahMasjidDao
import com.example.data.local.SinggahMasjidDatabase
import com.example.model.CustomMosque
import com.example.model.FavoriteMosque
import com.example.model.MosqueReview
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MosqueRepository(
    private val dao: SinggahMasjidDao,
    private val localUserId: String = LOCAL_USER_ID
) {
    constructor(context: Context) : this(
        SinggahMasjidDatabase.getInstance(context).singgahMasjidDao()
    )

    fun observeCustomMosques(): Flow<List<CustomMosque>> =
        dao.observeCustomMosques().map { list -> list.map { it.toDomain() } }

    fun observeAllReviews(): Flow<List<MosqueReview>> =
        dao.observeAllReviews().map { list -> list.map { it.toDomain() } }

    fun observeFavorites(userId: String = localUserId): Flow<List<FavoriteMosque>> =
        dao.observeFavorites(userId.ifBlank { localUserId }).map { list -> list.map { it.toDomain() } }

    suspend fun submitReview(
        mosqueId: String,
        mosqueName: String,
        authorName: String,
        rating: Int,
        facilities: List<String>,
        comment: String
    ): Result<Unit> {
        return try {
            val cleanMosqueId = mosqueId.trim()
            val cleanMosqueName = mosqueName.trim().take(120)
            val cleanAuthor = authorName.trim().ifBlank { "Musafir" }.take(60)
            val cleanComment = comment.trim().take(600)
            val cleanFacilities = facilities.distinct().take(20)
            require(cleanMosqueId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                "ID masjid tidak valid"
            }
            require(cleanMosqueName.isNotEmpty()) { "Nama masjid wajib diisi" }
            require(rating in 1..5) { "Rating harus antara 1 sampai 5" }

            val now = System.currentTimeMillis()
            dao.insertReview(
                MosqueReviewEntity(
                    id = "rev_${UUID.randomUUID()}",
                    userId = localUserId,
                    authorName = cleanAuthor,
                    mosqueId = cleanMosqueId,
                    mosqueName = cleanMosqueName,
                    rating = rating.toLong(),
                    facilities = cleanFacilities,
                    comment = cleanComment,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReview(
        reviewId: String,
        authorName: String,
        rating: Int,
        facilities: List<String>,
        comment: String
    ): Result<Unit> {
        return try {
            require(reviewId.isNotBlank()) { "ID ulasan tidak valid" }
            require(rating in 1..5) { "Rating harus antara 1 sampai 5" }

            val cleanAuthor = authorName.trim().ifBlank { "Musafir" }.take(60)
            val cleanComment = comment.trim().take(600)
            val cleanFacilities = facilities.distinct().take(20)

            dao.updateReview(
                reviewId = reviewId,
                authorName = cleanAuthor,
                rating = rating.toLong(),
                facilities = cleanFacilities,
                comment = cleanComment,
                updatedAtMillis = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            require(reviewId.isNotBlank()) { "ID ulasan tidak valid" }
            dao.deleteReviewById(reviewId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomMosque(
        authorName: String,
        name: String,
        address: String,
        lat: Double,
        lon: Double,
        facilities: List<String>
    ): Result<Unit> {
        return try {
            val cleanName = name.trim().take(120)
            val cleanAddress = address.trim().ifBlank { "Ditambahkan oleh musafir" }.take(250)
            val cleanAuthor = authorName.trim().ifBlank { "Musafir" }.take(60)
            val cleanFacilities = facilities.distinct().take(20)
            require(cleanName.length >= 2) { "Nama masjid minimal 2 karakter" }
            require(lat in -90.0..90.0 && lon in -180.0..180.0) { "Koordinat tidak valid" }

            val now = System.currentTimeMillis()
            dao.insertCustomMosque(
                CustomMosqueEntity(
                    id = "custom_${UUID.randomUUID()}",
                    userId = localUserId,
                    authorName = cleanAuthor,
                    name = cleanName,
                    address = cleanAddress,
                    lat = lat,
                    lon = lon,
                    baseFacilities = cleanFacilities,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomMosque(mosqueId: String): Result<Unit> {
        return try {
            require(mosqueId.isNotBlank()) { "ID masjid tidak valid" }
            dao.deleteCustomMosqueById(mosqueId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(
        mosqueId: String,
        name: String,
        address: String,
        lat: Double,
        lon: Double,
        currentlyFavorite: Boolean
    ): Result<Unit> {
        return try {
            val cleanId = mosqueId.trim()
            require(cleanId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                "ID masjid tidak valid"
            }

            if (currentlyFavorite) {
                dao.deleteFavorite(mosqueId = cleanId, userId = localUserId)
            } else {
                dao.insertFavorite(
                    FavoriteMosqueEntity(
                        mosqueId = cleanId,
                        userId = localUserId,
                        name = name.trim().take(120).ifBlank { "Masjid" },
                        address = address.trim().take(250),
                        lat = lat,
                        lon = lon,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val LOCAL_USER_ID = "local_musafir"
    }
}
