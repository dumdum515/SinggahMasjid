package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.CustomMosque
import com.example.model.FavoriteMosque
import com.example.model.MosqueReview
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "custom_mosques")
data class CustomMosqueEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val authorName: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val baseFacilities: List<String>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): CustomMosque = CustomMosque(
        id = id,
        userId = userId,
        authorName = authorName,
        name = name,
        address = address,
        lat = lat,
        lon = lon,
        baseFacilities = baseFacilities,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "mosque_reviews")
data class MosqueReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val authorName: String,
    val mosqueId: String,
    val mosqueName: String,
    val rating: Long,
    val facilities: List<String>,
    val comment: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): MosqueReview = MosqueReview(
        id = id,
        userId = userId,
        authorName = authorName,
        mosqueId = mosqueId,
        mosqueName = mosqueName,
        rating = rating,
        facilities = facilities,
        comment = comment,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "favorite_mosques")
data class FavoriteMosqueEntity(
    @PrimaryKey val mosqueId: String,
    val userId: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val createdAtMillis: Long
) {
    fun toDomain(): FavoriteMosque = FavoriteMosque(
        id = mosqueId,
        userId = userId,
        mosqueId = mosqueId,
        name = name,
        address = address,
        lat = lat,
        lon = lon,
        createdAtMillis = createdAtMillis
    )
}

class FacilityListConverter {
    @TypeConverter
    fun fromList(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Dao
interface SinggahMasjidDao {
    @Query("SELECT * FROM custom_mosques ORDER BY createdAtMillis DESC")
    fun observeCustomMosques(): Flow<List<CustomMosqueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMosque(entity: CustomMosqueEntity)

    @Query("DELETE FROM custom_mosques WHERE id = :id")
    suspend fun deleteCustomMosqueById(id: String)

    @Query("SELECT * FROM mosque_reviews ORDER BY createdAtMillis DESC")
    fun observeAllReviews(): Flow<List<MosqueReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(entity: MosqueReviewEntity)

    @Query(
        "UPDATE mosque_reviews SET authorName = :authorName, rating = :rating, " +
            "facilities = :facilities, comment = :comment, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :reviewId"
    )
    suspend fun updateReview(
        reviewId: String,
        authorName: String,
        rating: Long,
        facilities: List<String>,
        comment: String,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM mosque_reviews WHERE id = :reviewId")
    suspend fun deleteReviewById(reviewId: String)

    @Query("SELECT * FROM favorite_mosques WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeFavorites(userId: String): Flow<List<FavoriteMosqueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteMosqueEntity)

    @Query("DELETE FROM favorite_mosques WHERE mosqueId = :mosqueId AND userId = :userId")
    suspend fun deleteFavorite(mosqueId: String, userId: String)
}

@Database(
    entities = [
        CustomMosqueEntity::class,
        MosqueReviewEntity::class,
        FavoriteMosqueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FacilityListConverter::class)
abstract class SinggahMasjidDatabase : RoomDatabase() {
    abstract fun singgahMasjidDao(): SinggahMasjidDao

    companion object {
        @Volatile
        private var INSTANCE: SinggahMasjidDatabase? = null

        fun getInstance(context: Context): SinggahMasjidDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SinggahMasjidDatabase::class.java,
                    "singgah_masjid.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
