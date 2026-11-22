package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MosqueRepository
import com.example.data.OverpassFetchResult
import com.example.data.OverpassRepository
import com.example.model.CustomMosque
import com.example.model.FavoriteMosque
import com.example.model.MosqueReview
import com.example.model.MosqueSortOption
import com.example.model.MosqueSource
import com.example.model.MosqueUiItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SearchParams(
    val lat: Double = -6.1702,
    val lon: Double = 106.8314,
    val locationLabel: String = "Jakarta Pusat (Default)",
    val radiusMeters: Int = 3000,
    val searchQuery: String = "",
    val selectedFacilities: Set<String> = emptySet(),
    val sortOption: MosqueSortOption = MosqueSortOption.DISTANCE,
    val loadingOsm: Boolean = true,
    val liveOsm: Boolean = true
)

class SinggahMasjidViewModel(
    private val mosqueRepository: MosqueRepository,
    private val overpassRepository: OverpassRepository,
    val currentUserId: String = MosqueRepository.LOCAL_USER_ID,
    initialUserName: String = "Musafir",
    private val onPersistUserName: (String) -> Unit = {}
) : ViewModel() {

    private val _currentUserName = MutableStateFlow(initialUserName.ifBlank { "Musafir" })
    val currentUserNameState: StateFlow<String> = _currentUserName.asStateFlow()

    val currentUserName: String
        get() = _currentUserName.value

    private val _searchParams = MutableStateFlow(SearchParams())
    val searchParams: StateFlow<SearchParams> = _searchParams.asStateFlow()

    private val _osmMosques = MutableStateFlow<List<MosqueSource>>(emptyList())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val customMosquesState: StateFlow<UiState<List<CustomMosque>>> =
        mosqueRepository.observeCustomMosques()
            .map<List<CustomMosque>, UiState<List<CustomMosque>>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing custom mosques", error)
                emit(UiState.Error(error.message ?: "Gagal memuat data masjid lokal"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val reviewsState: StateFlow<UiState<List<MosqueReview>>> =
        mosqueRepository.observeAllReviews()
            .map<List<MosqueReview>, UiState<List<MosqueReview>>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing reviews", error)
                emit(UiState.Error(error.message ?: "Gagal memuat ulasan masjid"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoritesState: StateFlow<UiState<List<FavoriteMosque>>> =
        mosqueRepository.observeFavorites(currentUserId)
            .map<List<FavoriteMosque>, UiState<List<FavoriteMosque>>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing favorites", error)
                emit(UiState.Error(error.message ?: "Gagal memuat daftar favorit"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val mosquesUiState: StateFlow<UiState<List<MosqueUiItem>>> = combine(
        _searchParams,
        _osmMosques,
        customMosquesState,
        reviewsState,
        favoritesState
    ) { params, osmList, customState, reviewState, favState ->
        val dbError = (customState as? UiState.Error)?.message
            ?: (reviewState as? UiState.Error)?.message
            ?: (favState as? UiState.Error)?.message

        if (dbError != null && osmList.isEmpty()) {
            return@combine UiState.Error(dbError)
        }

        val customSources = (customState as? UiState.Success)?.data.orEmpty()
            .filter { custom ->
                OverpassRepository.haversineDistanceMeters(
                    params.lat, params.lon, custom.lat, custom.lon
                ) <= params.radiusMeters
            }
            .map { custom ->
                MosqueSource(
                    id = custom.id,
                    name = custom.name,
                    address = custom.address,
                    lat = custom.lat,
                    lon = custom.lon,
                    baseFacilities = custom.baseFacilities,
                    customOwnerId = custom.userId
                )
            }

        val allReviews = (reviewState as? UiState.Success)?.data.orEmpty()
        val reviewsByMosque = allReviews.groupBy { it.mosqueId }
        val favoriteIds = (favState as? UiState.Success)?.data.orEmpty()
            .map { it.mosqueId }
            .toSet()

        val combinedSources = customSources + osmList
        if (params.loadingOsm && combinedSources.isEmpty()) {
            return@combine UiState.Loading
        }

        val enriched = combinedSources.map { source ->
            val reviews = reviewsByMosque[source.id].orEmpty()
                .sortedByDescending { it.createdAtMillis }
            val distance = OverpassRepository.haversineDistanceMeters(
                params.lat, params.lon, source.lat, source.lon
            )
            val avgRating = if (reviews.isNotEmpty()) {
                reviews.sumOf { it.rating.toDouble() } / reviews.size
            } else {
                0.0
            }

            val facilityCounts = LinkedHashMap<String, Int>()
            source.baseFacilities.forEach { fid ->
                facilityCounts[fid] = (facilityCounts[fid] ?: 0) + 1
            }
            reviews.forEach { review ->
                review.facilities.distinct().forEach { fid ->
                    facilityCounts[fid] = (facilityCounts[fid] ?: 0) + 1
                }
            }

            MosqueUiItem(
                id = source.id,
                name = source.name,
                address = source.address,
                lat = source.lat,
                lon = source.lon,
                distanceMeters = distance,
                avgRating = avgRating,
                reviewCount = reviews.size,
                facilityCounts = facilityCounts,
                verifiedFacilities = facilityCounts.keys.toList(),
                reviews = reviews,
                favorite = source.id in favoriteIds,
                customAdded = source.customOwnerId != null,
                customOwnerId = source.customOwnerId
            )
        }

        val queryLower = params.searchQuery.trim().lowercase()
        val filtered = enriched.filter { item ->
            val matchesQuery = queryLower.isEmpty() ||
                item.name.lowercase().contains(queryLower) ||
                item.address.lowercase().contains(queryLower)
            val matchesFacilities = params.selectedFacilities.isEmpty() ||
                params.selectedFacilities.all { fid -> fid in item.verifiedFacilities }
            matchesQuery && matchesFacilities
        }

        val sorted = when (params.sortOption) {
            MosqueSortOption.DISTANCE -> filtered.sortedBy { it.distanceMeters }
            MosqueSortOption.RATING -> filtered.sortedWith(
                compareByDescending<MosqueUiItem> { it.avgRating }
                    .thenByDescending { it.reviewCount }
                    .thenBy { it.distanceMeters }
            )
            MosqueSortOption.FACILITIES -> filtered.sortedWith(
                compareByDescending<MosqueUiItem> { it.verifiedFacilities.size }
                    .thenBy { it.distanceMeters }
            )
        }

        UiState.Success(sorted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    init {
        refreshNearbyMosques()
    }

    fun updateUserName(newName: String) {
        val cleaned = newName.trim().ifBlank { "Musafir" }.take(60)
        _currentUserName.value = cleaned
        onPersistUserName(cleaned)
        _snackbarMessage.value = "Nama profil diperbarui menjadi $cleaned"
    }

    fun updateLocation(lat: Double, lon: Double, label: String) {
        _searchParams.update {
            it.copy(lat = lat, lon = lon, locationLabel = label)
        }
        refreshNearbyMosques()
    }

    fun setRadius(radiusMeters: Int) {
        if (_searchParams.value.radiusMeters == radiusMeters) return
        _searchParams.update { it.copy(radiusMeters = radiusMeters) }
        refreshNearbyMosques()
    }

    fun setSearchQuery(query: String) {
        _searchParams.update { it.copy(searchQuery = query) }
    }

    fun toggleFacilityFilter(facilityId: String) {
        _searchParams.update { current ->
            val next = current.selectedFacilities.toMutableSet()
            if (!next.add(facilityId)) {
                next.remove(facilityId)
            }
            current.copy(selectedFacilities = next)
        }
    }

    fun clearFilters() {
        _searchParams.update {
            it.copy(searchQuery = "", selectedFacilities = emptySet())
        }
    }

    fun setSortOption(option: MosqueSortOption) {
        _searchParams.update { it.copy(sortOption = option) }
    }

    fun refreshNearbyMosques() {
        val current = _searchParams.value
        viewModelScope.launch {
            _searchParams.update { it.copy(loadingOsm = true) }
            val result: OverpassFetchResult = overpassRepository.fetchNearbyMosques(
                lat = current.lat,
                lon = current.lon,
                radiusMeters = current.radiusMeters
            )
            _osmMosques.value = result.mosques
            _searchParams.update {
                it.copy(loadingOsm = false, liveOsm = result.isLiveOsm)
            }
        }
    }

    fun submitOrUpdateReview(
        mosque: MosqueUiItem,
        editingReviewId: String?,
        authorName: String,
        rating: Int,
        facilities: List<String>,
        comment: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (editingReviewId != null) {
                mosqueRepository.updateReview(
                    reviewId = editingReviewId,
                    authorName = authorName,
                    rating = rating,
                    facilities = facilities,
                    comment = comment
                )
            } else {
                mosqueRepository.submitReview(
                    mosqueId = mosque.id,
                    mosqueName = mosque.name,
                    authorName = authorName,
                    rating = rating,
                    facilities = facilities,
                    comment = comment
                )
            }
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = if (editingReviewId != null) {
                    "Ulasan berhasil diperbarui"
                } else {
                    "Terima kasih! Rating & fasilitas disimpan lokal"
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menyimpan ulasan"
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = mosqueRepository.deleteReview(reviewId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Ulasan berhasil dihapus"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menghapus ulasan"
            }
        }
    }

    fun addCustomMosque(
        name: String,
        address: String,
        facilities: List<String>,
        onComplete: () -> Unit
    ) {
        val params = _searchParams.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = mosqueRepository.addCustomMosque(
                authorName = currentUserName,
                name = name,
                address = address,
                lat = params.lat,
                lon = params.lon,
                facilities = facilities
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Masjid baru berhasil ditambahkan secara lokal"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menambah masjid"
            }
        }
    }

    fun deleteCustomMosque(mosqueId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = mosqueRepository.deleteCustomMosque(mosqueId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Masjid berhasil dihapus"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menghapus masjid"
            }
        }
    }

    fun toggleFavorite(mosque: MosqueUiItem) {
        viewModelScope.launch {
            val result = mosqueRepository.toggleFavorite(
                mosqueId = mosque.id,
                name = mosque.name,
                address = mosque.address,
                lat = mosque.lat,
                lon = mosque.lon,
                currentlyFavorite = mosque.favorite
            )
            result.onSuccess {
                _snackbarMessage.value = if (mosque.favorite) {
                    "Dihapus dari daftar favorit"
                } else {
                    "Disimpan ke masjid favorit"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal memperbarui favorit"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private companion object {
        const val TAG = "SinggahMasjidVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
