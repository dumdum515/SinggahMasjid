package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LocationHelper
import com.example.data.OverpassRepository
import com.example.model.FavoriteMosque
import com.example.model.MosqueReview
import com.example.model.MosqueSortOption
import com.example.model.MosqueUiItem
import com.example.ui.components.AddMosqueSheet
import com.example.ui.components.FacilityFilterBar
import com.example.ui.components.LocationPickerDialog
import com.example.ui.components.MosqueCard
import com.example.ui.components.MosqueDetailSheet
import com.example.ui.components.OsmMosqueMapView
import com.example.ui.components.RateMosqueSheet
import com.example.ui.components.RatingSummaryRow
import com.example.ui.components.openExternalMapsRoute
import kotlinx.coroutines.launch

enum class MainTab(val label: String) {
    EXPLORE("Jelajah"),
    MAP("Peta"),
    FAVORITES("Favorit"),
    PROFILE("Profil")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinggahMasjidApp(
    viewModel: SinggahMasjidViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationHelper = remember(context) { LocationHelper(context) }

    val searchParams by viewModel.searchParams.collectAsStateWithLifecycle()
    val mosquesState by viewModel.mosquesUiState.collectAsStateWithLifecycle()
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val currentUserName by viewModel.currentUserNameState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.EXPLORE) }
    var showInlineMap by rememberSaveable { mutableStateOf(true) }
    var selectedMosqueId by rememberSaveable { mutableStateOf<String?>(null) }

    // Modal state
    var ratingTargetMosque by remember { mutableStateOf<MosqueUiItem?>(null) }
    var editingReview by remember { mutableStateOf<MosqueReview?>(null) }
    var detailTargetMosqueId by remember { mutableStateOf<String?>(null) }
    var showAddMosqueSheet by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    fun fetchDeviceGps() {
        scope.launch {
            val coord = locationHelper.getCurrentLocation()
            if (coord != null) {
                viewModel.updateLocation(coord.lat, coord.lon, coord.label)
            } else {
                snackbarHostState.showSnackbar("Sinyal GPS tidak tersedia. Ketuk peta atau pilih kota.")
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceGps()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin lokasi ditolak. Ketuk peta untuk memilih area pencarian.")
            }
        }
    }

    fun requestOrFetchLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchDeviceGps()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    // Attempt foreground GPS automatically if already permitted on launch
    LaunchedEffect(Unit) {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse || hasFine) {
            fetchDeviceGps()
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    val mosqueList = (mosquesState as? UiState.Success)?.data.orEmpty()
    val detailTargetMosque = detailTargetMosqueId?.let { id -> mosqueList.firstOrNull { it.id == id } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mosque,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SinggahMasjid",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1
                                )
                                Text(
                                    text = searchParams.locationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showLocationDialog = true },
                            modifier = Modifier.testTag("pick_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "Pilih lokasi GPS atau kota"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshNearbyMosques() },
                            modifier = Modifier.testTag("refresh_mosques_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Muat ulang masjid sekitar"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                MainTab.EXPLORE -> Icons.Rounded.Explore
                                MainTab.MAP -> Icons.Rounded.Map
                                MainTab.FAVORITES -> Icons.Rounded.Bookmark
                                MainTab.PROFILE -> Icons.Rounded.Person
                            }
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == MainTab.EXPLORE || currentTab == MainTab.MAP) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddMosqueSheet = true },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        icon = { Icon(Icons.Rounded.AddLocationAlt, contentDescription = null) },
                        text = { Text("Tambah Masjid") },
                        modifier = Modifier.testTag("add_mosque_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                MainTab.EXPLORE -> Icons.Rounded.Explore
                                MainTab.MAP -> Icons.Rounded.Map
                                MainTab.FAVORITES -> Icons.Rounded.Bookmark
                                MainTab.PROFILE -> Icons.Rounded.Person
                            }
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                when (currentTab) {
                    MainTab.EXPLORE -> ExploreScreen(
                        searchParams = searchParams,
                        mosquesState = mosquesState,
                        showInlineMap = showInlineMap,
                        selectedMosqueId = selectedMosqueId,
                        isWideScreen = useRail,
                        onToggleInlineMap = { showInlineMap = !showInlineMap },
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onRadiusChange = viewModel::setRadius,
                        onSortChange = viewModel::setSortOption,
                        onToggleFacility = viewModel::toggleFacilityFilter,
                        onClearFilters = viewModel::clearFilters,
                        onSelectMosqueFromMap = { mosque ->
                            selectedMosqueId = mosque.id
                            detailTargetMosqueId = mosque.id
                        },
                        onMapTapCoordinate = { lat, lon ->
                            viewModel.updateLocation(
                                lat, lon,
                                "Titik Peta (${String.format("%.4f, %.4f", lat, lon)})"
                            )
                        },
                        onRecenter = { requestOrFetchLocation() },
                        onRateClick = { mosque ->
                            editingReview = null
                            ratingTargetMosque = mosque
                        },
                        onDetailClick = { mosque ->
                            selectedMosqueId = mosque.id
                            detailTargetMosqueId = mosque.id
                        },
                        onFavoriteToggle = viewModel::toggleFavorite,
                        onRetry = viewModel::refreshNearbyMosques
                    )

                    MainTab.MAP -> FullMapScreen(
                        searchParams = searchParams,
                        mosques = mosqueList,
                        selectedMosqueId = selectedMosqueId,
                        onSelectMosque = { selectedMosqueId = it.id },
                        onOpenDetail = { detailTargetMosqueId = it.id },
                        onRateClick = {
                            editingReview = null
                            ratingTargetMosque = it
                        },
                        onMapTapCoordinate = { lat, lon ->
                            viewModel.updateLocation(
                                lat, lon,
                                "Titik Peta (${String.format("%.4f, %.4f", lat, lon)})"
                            )
                        },
                        onRecenter = { requestOrFetchLocation() },
                        onRadiusChange = viewModel::setRadius
                    )

                    MainTab.FAVORITES -> FavoritesScreen(
                        favoritesState = favoritesState,
                        allMosques = mosqueList,
                        onOpenDetail = { detailTargetMosqueId = it.id },
                        onRateClick = {
                            editingReview = null
                            ratingTargetMosque = it
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onExploreClick = { currentTab = MainTab.EXPLORE }
                    )

                    MainTab.PROFILE -> ProfileScreen(
                        userName = currentUserName,
                        favoritesCount = (favoritesState as? UiState.Success)?.data?.size ?: 0,
                        nearbyCount = mosqueList.size,
                        onRequestLocation = { requestOrFetchLocation() },
                        onUpdateUserName = viewModel::updateUserName
                    )
                }
            }
        }
    }

    // Sheets & Dialogs
    ratingTargetMosque?.let { target ->
        RateMosqueSheet(
            mosque = target,
            defaultAuthorName = currentUserName,
            editingReview = editingReview,
            isSubmitting = isMutating,
            onDismiss = {
                ratingTargetMosque = null
                editingReview = null
            },
            onSubmit = { authorName, rating, facilities, comment ->
                viewModel.submitOrUpdateReview(
                    mosque = target,
                    editingReviewId = editingReview?.id,
                    authorName = authorName,
                    rating = rating,
                    facilities = facilities,
                    comment = comment
                ) {
                    ratingTargetMosque = null
                    editingReview = null
                }
            }
        )
    }

    detailTargetMosque?.let { target ->
        MosqueDetailSheet(
            mosque = target,
            currentUserId = viewModel.currentUserId,
            onDismiss = { detailTargetMosqueId = null },
            onRateClick = {
                detailTargetMosqueId = null
                editingReview = null
                ratingTargetMosque = target
            },
            onEditReview = { review ->
                detailTargetMosqueId = null
                editingReview = review
                ratingTargetMosque = target
            },
            onDeleteReview = { reviewId ->
                viewModel.deleteReview(reviewId)
            },
            onDeleteCustomMosque = { mosqueId ->
                viewModel.deleteCustomMosque(mosqueId) {
                    detailTargetMosqueId = null
                }
            },
            onFavoriteToggle = { viewModel.toggleFavorite(target) }
        )
    }

    if (showAddMosqueSheet) {
        AddMosqueSheet(
            lat = searchParams.lat,
            lon = searchParams.lon,
            isSubmitting = isMutating,
            onDismiss = { showAddMosqueSheet = false },
            onSubmit = { name, address, facilities ->
                viewModel.addCustomMosque(name, address, facilities) {
                    showAddMosqueSheet = false
                }
            }
        )
    }

    if (showLocationDialog) {
        LocationPickerDialog(
            onUseGpsClick = { requestOrFetchLocation() },
            onSelectPreset = { preset ->
                viewModel.updateLocation(preset.lat, preset.lon, preset.name)
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}

@Composable
private fun ExploreScreen(
    searchParams: SearchParams,
    mosquesState: UiState<List<MosqueUiItem>>,
    showInlineMap: Boolean,
    selectedMosqueId: String?,
    isWideScreen: Boolean,
    onToggleInlineMap: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onSortChange: (MosqueSortOption) -> Unit,
    onToggleFacility: (String) -> Unit,
    onClearFilters: () -> Unit,
    onSelectMosqueFromMap: (MosqueUiItem) -> Unit,
    onMapTapCoordinate: (Double, Double) -> Unit,
    onRecenter: () -> Unit,
    onRateClick: (MosqueUiItem) -> Unit,
    onDetailClick: (MosqueUiItem) -> Unit,
    onFavoriteToggle: (MosqueUiItem) -> Unit,
    onRetry: () -> Unit
) {
    val mosques = (mosquesState as? UiState.Success)?.data.orEmpty()
    var radiusMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val listContent: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(
            modifier = listModifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (searchParams.loadingOsm) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // Search & Controls Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchParams.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Cari nama masjid atau nama jalan...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchParams.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Bersihkan pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("search_mosque_input")
                    )

                    // Radius, Sort, Map Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            FilledTonalButton(
                                onClick = { radiusMenuOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Radius ${searchParams.radiusMeters / 1000.0} km")
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = radiusMenuOpen,
                                onDismissRequest = { radiusMenuOpen = false }
                            ) {
                                listOf(1500, 3000, 5000, 10000).forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("Radius ${r / 1000.0} km") },
                                        onClick = {
                                            radiusMenuOpen = false
                                            onRadiusChange(r)
                                        }
                                    )
                                }
                            }
                        }

                        Box {
                            FilledTonalButton(
                                onClick = { sortMenuOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(searchParams.sortOption.label)
                            }
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false }
                            ) {
                                MosqueSortOption.entries.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt.label) },
                                        onClick = {
                                            sortMenuOpen = false
                                            onSortChange(opt)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (!isWideScreen) {
                            OutlinedButton(
                                onClick = onToggleInlineMap,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(if (showInlineMap) "Tutup Peta" else "Peta")
                                Icon(
                                    imageVector = if (showInlineMap) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Facility Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Fasilitas Ramah Musafir",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchParams.selectedFacilities.isNotEmpty() || searchParams.searchQuery.isNotBlank()) {
                            TextButton(
                                onClick = onClearFilters,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Reset (${searchParams.selectedFacilities.size})")
                            }
                        }
                    }
                    FacilityFilterBar(
                        selectedFacilities = searchParams.selectedFacilities,
                        onToggleFacility = onToggleFacility
                    )
                }
            }

            // Inline Map on Compact Screens
            if (!isWideScreen) {
                item {
                    AnimatedVisibility(visible = showInlineMap) {
                        OsmMosqueMapView(
                            searchLat = searchParams.lat,
                            searchLon = searchParams.lon,
                            radiusMeters = searchParams.radiusMeters,
                            mosques = mosques,
                            selectedMosqueId = selectedMosqueId,
                            onSelectMosque = onSelectMosqueFromMap,
                            onMapTapCoordinate = onMapTapCoordinate,
                            onRecenter = onRecenter,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .height(250.dp)
                        )
                    }
                }
            }

            // Status / Count Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Masjid Sekitar (${mosques.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        color = if (searchParams.liveOsm) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (searchParams.liveOsm) "OpenStreetMap Live" else "Mode Referensi Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (searchParams.liveOsm) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            when (mosquesState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Mencari masjid di sekitar lokasi Anda...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Gagal Memuat Data",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = mosquesState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Button(onClick = onRetry) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    if (mosques.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mosque,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Belum ada masjid yang cocok",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Coba perluas radius pencarian, kurangi filter fasilitas, atau tambahkan masjid secara manual.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(onClick = onClearFilters) {
                                        Text("Reset Semua Filter")
                                    }
                                }
                            }
                        }
                    } else {
                        items(mosques, key = { it.id }) { mosque ->
                            MosqueCard(
                                mosque = mosque,
                                isHighlighted = mosque.id == selectedMosqueId,
                                onRateClick = { onRateClick(mosque) },
                                onDetailClick = { onDetailClick(mosque) },
                                onFavoriteToggle = { onFavoriteToggle(mosque) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            OsmMosqueMapView(
                searchLat = searchParams.lat,
                searchLon = searchParams.lon,
                radiusMeters = searchParams.radiusMeters,
                mosques = mosques,
                selectedMosqueId = selectedMosqueId,
                onSelectMosque = onSelectMosqueFromMap,
                onMapTapCoordinate = onMapTapCoordinate,
                onRecenter = onRecenter,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            )
            listContent(Modifier.weight(1.1f))
        }
    } else {
        listContent(Modifier.fillMaxSize())
    }
}

@Composable
private fun FullMapScreen(
    searchParams: SearchParams,
    mosques: List<MosqueUiItem>,
    selectedMosqueId: String?,
    onSelectMosque: (MosqueUiItem) -> Unit,
    onOpenDetail: (MosqueUiItem) -> Unit,
    onRateClick: (MosqueUiItem) -> Unit,
    onMapTapCoordinate: (Double, Double) -> Unit,
    onRecenter: () -> Unit,
    onRadiusChange: (Int) -> Unit
) {
    val selected = selectedMosqueId?.let { id -> mosques.firstOrNull { it.id == id } }
        ?: mosques.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        OsmMosqueMapView(
            searchLat = searchParams.lat,
            searchLon = searchParams.lon,
            radiusMeters = searchParams.radiusMeters,
            mosques = mosques,
            selectedMosqueId = selected?.id,
            onSelectMosque = onSelectMosque,
            onMapTapCoordinate = onMapTapCoordinate,
            onRecenter = onRecenter,
            modifier = Modifier.fillMaxSize()
        )

        // Radius quick chips top-end
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(3000, 5000, 10000).forEach { r ->
                val active = searchParams.radiusMeters == r
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(999.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.clickable { onRadiusChange(r) }
                ) {
                    Text(
                        text = "${r / 1000} km",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Selected Mosque Preview Card at bottom-start
        if (selected != null) {
            val context = LocalContext.current
            Card(
                onClick = { onOpenDetail(selected) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 56.dp, start = 4.dp, end = 64.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selected.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = OverpassRepository.formatDistance(selected.distanceMeters),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    RatingSummaryRow(avgRating = selected.avgRating, reviewCount = selected.reviewCount)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRateClick(selected) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Nilai", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                openExternalMapsRoute(context, selected.lat, selected.lon, selected.name)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rute", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(
    favoritesState: UiState<List<FavoriteMosque>>,
    allMosques: List<MosqueUiItem>,
    onOpenDetail: (MosqueUiItem) -> Unit,
    onRateClick: (MosqueUiItem) -> Unit,
    onToggleFavorite: (MosqueUiItem) -> Unit,
    onExploreClick: () -> Unit
) {
    val context = LocalContext.current
    when (favoritesState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = favoritesState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        is UiState.Success -> {
            val favs = favoritesState.data
            if (favs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Belum Ada Masjid Favorit",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Simpan masjid dengan fasilitas terbaik selama perjalanan agar mudah ditemukan kembali.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onExploreClick) {
                            Text("Jelajahi Masjid Terdekat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Masjid Favorit Saya (${favs.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    items(favs, key = { it.mosqueId }) { fav ->
                        val matched = allMosques.firstOrNull { it.id == fav.mosqueId }
                        if (matched != null) {
                            MosqueCard(
                                mosque = matched,
                                isHighlighted = false,
                                onRateClick = { onRateClick(matched) },
                                onDetailClick = { onOpenDetail(matched) },
                                onFavoriteToggle = { onToggleFavorite(matched) }
                            )
                        } else {
                            // Favorite outside current search radius
                            val stub = MosqueUiItem(
                                id = fav.mosqueId,
                                name = fav.name,
                                address = fav.address,
                                lat = fav.lat,
                                lon = fav.lon,
                                distanceMeters = 0.0,
                                avgRating = 0.0,
                                reviewCount = 0,
                                facilityCounts = emptyMap(),
                                verifiedFacilities = emptyList(),
                                reviews = emptyList(),
                                favorite = true,
                                customAdded = false,
                                customOwnerId = null
                            )
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fav.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            fav.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            openExternalMapsRoute(context, fav.lat, fav.lon, fav.name)
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Directions, contentDescription = "Rute")
                                    }
                                    IconButton(onClick = { onToggleFavorite(stub) }) {
                                        Icon(
                                            Icons.Rounded.Bookmark,
                                            contentDescription = "Hapus favorit",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    userName: String,
    favoritesCount: Int,
    nearbyCount: Int,
    onRequestLocation: () -> Unit,
    onUpdateUserName: (String) -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "M",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "Musafir" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Profil Lokal Perangkat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showEditNameDialog = true },
                    modifier = Modifier.testTag("edit_profile_name_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Ubah nama profil"
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$nearbyCount",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Masjid di Area Ini",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$favoritesCount",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Masjid Favorit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        FilledTonalButton(
            onClick = onRequestLocation,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Perbarui Lokasi GPS Saya")
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Mode Penyimpanan Lokal",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Masjid tambahan, ulasan, dan daftar favorit disimpan langsung di perangkat ini (Room). Pencarian masjid sekitar tetap mengambil data peta langsung dari OpenStreetMap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showEditNameDialog) {
        var draftName by remember(userName) { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Ubah Nama Musafir") },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { if (it.length <= 60) draftName = it },
                    label = { Text("Nama Tampilan") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserName(draftName)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
