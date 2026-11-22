package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.MosqueRepository
import com.example.data.OverpassRepository
import com.example.data.local.SinggahMasjidDatabase
import com.example.model.MosqueFacility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: SinggahMasjidDatabase
  private lateinit var repository: MosqueRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, SinggahMasjidDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = MosqueRepository(db.singgahMasjidDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SinggahMasjid", appName)
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
  fun `room repository persists custom mosque reviews and favorites`() = runTest {
    val addMosqueResult = repository.addCustomMosque(
      authorName = "Ahmad",
      name = "Masjid Singgah Musafir",
      address = "Jl. Merdeka No. 10",
      lat = -6.1702,
      lon = 106.8314,
      facilities = listOf("wifi", "minuman", "shower")
    )
    assertTrue(addMosqueResult.isSuccess)
    val mosques = repository.observeCustomMosques().first()
    assertEquals(1, mosques.size)
    assertEquals("Masjid Singgah Musafir", mosques.first().name)

    val mosqueId = mosques.first().id
    val reviewResult = repository.submitReview(
      mosqueId = mosqueId,
      mosqueName = "Masjid Singgah Musafir",
      authorName = "Ahmad",
      rating = 5,
      facilities = listOf("wifi", "shower"),
      comment = "Bersih dan nyaman untuk istirahat"
    )
    assertTrue(reviewResult.isSuccess)
    val reviews = repository.observeAllReviews().first()
    assertEquals(1, reviews.size)
    assertEquals(5L, reviews.first().rating)

    val favResult = repository.toggleFavorite(
      mosqueId = mosqueId,
      name = "Masjid Singgah Musafir",
      address = "Jl. Merdeka No. 10",
      lat = -6.1702,
      lon = 106.8314,
      currentlyFavorite = false
    )
    assertTrue(favResult.isSuccess)
    val favs = repository.observeFavorites().first()
    assertEquals(1, favs.size)

    val unfavResult = repository.toggleFavorite(
      mosqueId = mosqueId,
      name = "Masjid Singgah Musafir",
      address = "Jl. Merdeka No. 10",
      lat = -6.1702,
      lon = 106.8314,
      currentlyFavorite = true
    )
    assertTrue(unfavResult.isSuccess)
    assertTrue(repository.observeFavorites().first().isEmpty())
  }
}
