package com.example.data

import android.util.Log
import com.example.model.MosqueSource
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val type: String = "",
    val id: Long = 0L,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

interface OverpassApiService {
    @FormUrlEncoded
    @POST
    suspend fun queryOverpass(
        @Url url: String,
        @Field("data") query: String
    ): OverpassResponse
}

data class LocationPreset(
    val name: String,
    val lat: Double,
    val lon: Double
)

data class OverpassFetchResult(
    val mosques: List<MosqueSource>,
    val isLiveOsm: Boolean
)

class OverpassRepository {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "SinggahMasjidAndroid/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: OverpassApiService = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OverpassApiService::class.java)

    private val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    suspend fun fetchNearbyMosques(
        lat: Double,
        lon: Double,
        radiusMeters: Int
    ): OverpassFetchResult {
        val query = """
            [out:json][timeout:15];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
              node["building"="mosque"](around:$radiusMeters,$lat,$lon);
              way["building"="mosque"](around:$radiusMeters,$lat,$lon);
            );
            out center;
        """.trimIndent()

        for (endpoint in endpoints) {
            try {
                val response = api.queryOverpass(endpoint, query)
                val parsed = parseOverpassElements(response.elements)
                if (parsed.isNotEmpty()) {
                    return OverpassFetchResult(mosques = parsed, isLiveOsm = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("OverpassRepo", "Overpass endpoint $endpoint failed: ${e.message}")
            }
        }

        // Graceful fallback when OpenStreetMap Overpass is unreachable or returns empty
        return OverpassFetchResult(
            mosques = buildNearbyFallbackMosques(lat, lon),
            isLiveOsm = false
        )
    }

    private fun parseOverpassElements(elements: List<OverpassElement>): List<MosqueSource> {
        val seen = HashSet<String>()
        val results = mutableListOf<MosqueSource>()

        for (el in elements) {
            val elLat = el.lat ?: el.center?.lat ?: continue
            val elLon = el.lon ?: el.center?.lon ?: continue
            val tags = el.tags.orEmpty()
            val name = tags["name"] ?: tags["name:id"] ?: "Masjid / Musholla"
            val dedupKey = "${name.lowercase()}_${"%.4f".format(elLat)}_${"%.4f".format(elLon)}"
            if (!seen.add(dedupKey)) continue

            val addressParts = listOfNotNull(
                tags["addr:street"],
                tags["addr:suburb"],
                tags["addr:city"]
            ).filter { it.isNotBlank() }

            val baseFacilities = buildList {
                if (tags["internet_access"] == "wlan" || tags["internet_access"] == "yes") add("wifi")
                if (tags["wheelchair"] == "yes") add("disabilitas")
                if (tags["toilets"] == "yes") add("toilet")
                if (tags["air_conditioning"] == "yes") add("ac")
            }

            results.add(
                MosqueSource(
                    id = "osm_${el.type}_${el.id}",
                    name = name,
                    address = if (addressParts.isNotEmpty()) {
                        addressParts.joinToString(", ")
                    } else {
                        "Terdata di OpenStreetMap"
                    },
                    lat = elLat,
                    lon = elLon,
                    baseFacilities = baseFacilities,
                    customOwnerId = null
                )
            )
        }
        return results
    }

    private fun buildNearbyFallbackMosques(lat: Double, lon: Double): List<MosqueSource> {
        // Only used when Overpass API is offline/rate-limited so user can still browse & rate nearby reference points
        return listOf(
            MosqueSource(
                id = "osm_ref_al_ikhlas",
                name = "Masjid Raya Al-Ikhlas",
                address = "Area Pencarian Terdekat (Mode Offline)",
                lat = lat + 0.0028,
                lon = lon + 0.0022,
                baseFacilities = listOf("wifi", "minuman", "ac", "toilet", "parkir", "shower")
            ),
            MosqueSource(
                id = "osm_ref_nurul_huda",
                name = "Masjid Jami Nurul Huda",
                address = "Area Pencarian Terdekat (Mode Offline)",
                lat = lat - 0.0032,
                lon = lon + 0.0019,
                baseFacilities = listOf("istirahat", "menginap", "wudhu", "charger", "toilet")
            ),
            MosqueSource(
                id = "osm_ref_al_muhajirin",
                name = "Masjid Singgah Al-Muhajirin",
                address = "Area Pencarian Terdekat (Mode Offline)",
                lat = lat + 0.0014,
                lon = lon - 0.0035,
                baseFacilities = listOf("wifi", "minuman", "istirahat", "menginap", "shower", "keamanan")
            )
        )
    }

    companion object {
        val PRESET_LOCATIONS = listOf(
            LocationPreset("Jakarta Pusat (Istiqlal)", -6.1702, 106.8314),
            LocationPreset("Bandung (Salman ITB)", -6.8938, 107.6114),
            LocationPreset("Yogyakarta (Jogokariyan)", -7.8235, 110.3645),
            LocationPreset("Surabaya (Al-Akbar)", -7.3365, 112.7153)
        )

        fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        fun formatDistance(meters: Double): String {
            return if (meters < 1000.0) {
                "${meters.toInt()} m"
            } else {
                String.format("%.2f km", meters / 1000.0)
            }
        }
    }
}
