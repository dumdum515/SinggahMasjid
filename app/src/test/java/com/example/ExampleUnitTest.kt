package com.example

import com.example.data.MosqueRepository
import com.example.data.OverpassRepository
import com.example.data.local.CustomMosqueEntity
import com.example.data.local.FavoriteMosqueEntity
import com.example.data.local.MosqueReviewEntity
import com.example.data.local.SinggahMasjidDao
import com.example.model.MosqueFacility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private class FakeSinggahMasjidDao : SinggahMasjidDao {
    private val customMosques = MutableStateFlow<List<CustomMosqueEntity>>(emptyList())
    private val reviews = MutableStateFlow<List<MosqueReviewEntity>>(emptyList())
    private val favorites = MutableStateFlow<List<FavoriteMosqueEntity>>(emptyList())

    override fun observeCustomMosques(): Flow<List<CustomMosqueEntity>> = customMosques

    override suspend fun insertCustomMosque(entity: CustomMosqueEntity) {
      customMosques.value = customMosques.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun deleteCustomMosqueById(id: String) {
      customMosques.value = customMosques.value.filterNot { it.id == id }
    }

    override fun observeAllReviews(): Flow<List<MosqueReviewEntity>> = reviews

    override suspend fun insertReview(entity: MosqueReviewEntity) {
      reviews.value = reviews.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun updateReview(
      reviewId: String,
      authorName: String,
      rating: Long,
      facilities: List<String>,
      comment: String,
      updatedAtMillis: Long
    ) {
      reviews.value = reviews.value.map {
        if (it.id == reviewId) {
          it.copy(
            authorName = authorName,
            rating = rating,
            facilities = facilities,
            comment = comment,
            updatedAtMillis = updatedAtMillis
          )
        } else {
          it
        }
      }
    }

    override suspend fun deleteReviewById(reviewId: String) {
      reviews.value = reviews.value.filterNot { it.id == reviewId }
    }

    override fun observeFavorites(userId: String): Flow<List<FavoriteMosqueEntity>> =
      favorites.map { list -> list.filter { it.userId == userId } }

    override suspend fun insertFavorite(entity: FavoriteMosqueEntity) {
      favorites.value = favorites.value.filterNot {
        it.mosqueId == entity.mosqueId && it.userId == entity.userId
      } + entity
    }

    override suspend fun deleteFavorite(mosqueId: String, userId: String) {
      favorites.value = favorites.value.filterNot {
        it.mosqueId == mosqueId && it.userId == userId
      }
    }
  }

  @Test
  fun `mosque facilities include all requested traveler amenities`() {
    val requiredIds = listOf("wifi", "minuman", "istirahat", "menginap", "shower")
    requiredIds.forEach { id ->
      assertNotNull("Expected facility $id to exist", MosqueFacility.fromId(id))
    }
    assertTrue(MosqueFacility.entries.size >= 15)
  }

  @Test
  fun `haversine distance calculates realistic distance`() {
    val meters = OverpassRepository.haversineDistanceMeters(
      -6.1702, 106.8314,
      -6.1754, 106.8272
    )
    assertTrue("Expected ~740m between Istiqlal and Monas, got $meters", meters in 500.0..1000.0)
  }

  @Test
  fun `repository supports full CRUD for custom mosques reviews and favorites`() = runTest {
    val repo = MosqueRepository(FakeSinggahMasjidDao())

    assertTrue(
      repo.addCustomMosque(
        authorName = "Ahmad",
        name = "Masjid Singgah Musafir",
        address = "Jl. Merdeka No. 10",
        lat = -6.1702,
        lon = 106.8314,
        facilities = listOf("wifi", "minuman", "shower")
      ).isSuccess
    )

    val customMosques = repo.observeCustomMosques().first()
    assertEquals(1, customMosques.size)
    val mosqueId = customMosques.first().id

    assertTrue(
      repo.submitReview(
        mosqueId = mosqueId,
        mosqueName = "Masjid Singgah Musafir",
        authorName = "Ahmad",
        rating = 5,
        facilities = listOf("wifi", "shower"),
        comment = "Bersih dan nyaman"
      ).isSuccess
    )
    val review = repo.observeAllReviews().first().first()
    assertEquals(5L, review.rating)

    assertTrue(
      repo.updateReview(
        reviewId = review.id,
        authorName = "Ahmad",
        rating = 4,
        facilities = listOf("wifi"),
        comment = "Nyaman"
      ).isSuccess
    )
    assertEquals(4L, repo.observeAllReviews().first().first().rating)

    assertTrue(
      repo.toggleFavorite(
        mosqueId = mosqueId,
        name = "Masjid Singgah Musafir",
        address = "Jl. Merdeka No. 10",
        lat = -6.1702,
        lon = 106.8314,
        currentlyFavorite = false
      ).isSuccess
    )
    assertEquals(1, repo.observeFavorites().first().size)

    assertTrue(
      repo.toggleFavorite(
        mosqueId = mosqueId,
        name = "Masjid Singgah Musafir",
        address = "Jl. Merdeka No. 10",
        lat = -6.1702,
        lon = 106.8314,
        currentlyFavorite = true
      ).isSuccess
    )
    assertTrue(repo.observeFavorites().first().isEmpty())

    assertTrue(repo.deleteReview(review.id).isSuccess)
    assertTrue(repo.observeAllReviews().first().isEmpty())

    assertTrue(repo.deleteCustomMosque(mosqueId).isSuccess)
    assertTrue(repo.observeCustomMosques().first().isEmpty())
  }
}
