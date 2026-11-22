package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LocationPreset
import com.example.data.OverpassRepository
import com.example.model.MosqueFacility
import com.example.model.MosqueReview
import com.example.model.MosqueUiItem
import com.example.ui.theme.AmberStar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateMosqueSheet(
    mosque: MosqueUiItem,
    defaultAuthorName: String,
    editingReview: MosqueReview? = null,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (authorName: String, rating: Int, facilities: List<String>, comment: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var authorName by remember(editingReview) {
        mutableStateOf(editingReview?.authorName ?: defaultAuthorName)
    }
    var rating by remember(editingReview) {
        mutableIntStateOf(editingReview?.rating?.toInt()?.coerceIn(1, 5) ?: 5)
    }
    var selectedFacilities by remember(editingReview, mosque.id) {
        mutableStateOf(
            editingReview?.facilities?.toSet() ?: mosque.verifiedFacilities.toSet()
        )
    }
    var comment by remember(editingReview) {
        mutableStateOf(editingReview?.comment ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (editingReview != null) "Edit Ulasan: ${mosque.name}" else "Nilai Fasilitas: ${mosque.name}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Centang fasilitas yang tersedia saat Anda singgah untuk membantu musafir lain.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = authorName,
                onValueChange = { if (it.length <= 60) authorName = it },
                label = { Text("Nama Anda (Opsional)") },
                placeholder = { Text("Contoh: Musafir / Ahmad") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reviewer_name_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Rating Kenyamanan & Keramahan Musafir",
                    style = MaterialTheme.typography.labelLarge
                )
                InteractiveStarBar(
                    rating = rating,
                    onRatingSelected = { rating = it }
                )
            }

            HorizontalDivider()

            Text(
                text = "Fasilitas yang Tersedia (${selectedFacilities.size} dipilih)",
                style = MaterialTheme.typography.titleSmall
            )
            FacilityChecklistSection(
                selectedIds = selectedFacilities,
                onToggle = { fid ->
                    selectedFacilities = if (fid in selectedFacilities) {
                        selectedFacilities - fid
                    } else {
                        selectedFacilities + fid
                    }
                }
            )

            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 600) comment = it },
                label = { Text("Catatan Singgah / Ulasan") },
                placeholder = {
                    Text("Ceritakan kebersihan shower, ketersediaan air minum, kecepatan WiFi, atau aturan menginap...")
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_comment_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        onSubmit(
                            authorName.ifBlank { "Musafir" },
                            rating,
                            selectedFacilities.toList(),
                            comment
                        )
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("submit_review_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (editingReview != null) "Simpan Perubahan" else "Simpan Rating")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MosqueDetailSheet(
    mosque: MosqueUiItem,
    currentUserId: String,
    onDismiss: () -> Unit,
    onRateClick: () -> Unit,
    onEditReview: (MosqueReview) -> Unit,
    onDeleteReview: (String) -> Unit,
    onDeleteCustomMosque: (String) -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mosque.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${mosque.address} • ${OverpassRepository.formatDistance(mosque.distanceMeters)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (mosque.favorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Favorit",
                        tint = if (mosque.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RatingSummaryRow(avgRating = mosque.avgRating, reviewCount = mosque.reviewCount)

            // Quick CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRateClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Rounded.StarRate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nilai Fasilitas")
                }
                OutlinedButton(
                    onClick = { openExternalMapsRoute(context, mosque.lat, mosque.lon, mosque.name) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rute Maps")
                }
            }

            if (mosque.customAdded && mosque.customOwnerId == currentUserId) {
                OutlinedButton(
                    onClick = { onDeleteCustomMosque(mosque.id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus Masjid Komunitas Saya")
                }
            }

            HorizontalDivider()

            // Verified facilities full list
            Text(
                text = "Fasilitas Terkonfirmasi (${mosque.verifiedFacilities.size})",
                style = MaterialTheme.typography.titleMedium
            )
            FacilityBadgeRow(
                verifiedFacilities = mosque.verifiedFacilities,
                facilityCounts = mosque.facilityCounts,
                maxVisible = 20
            )

            HorizontalDivider()

            // Community Reviews
            Text(
                text = "Ulasan Musafir (${mosque.reviews.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (mosque.reviews.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada ulasan tertulis. Bagikan pengalaman Anda saat singgah di masjid ini!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                mosque.reviews.forEach { review ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = review.authorName.ifBlank { "Musafir" },
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                repeat(review.rating.toInt().coerceIn(1, 5)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = AmberStar,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            val dateStr = dateFormatter.format(Date(review.createdAtMillis))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (review.comment.isNotBlank()) {
                                Text(
                                    text = review.comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (review.facilities.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    review.facilities.forEach { fid ->
                                        val fac = MosqueFacility.fromId(fid) ?: return@forEach
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = fac.icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = fac.shortLabel,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (review.userId == currentUserId && review.id.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onEditReview(review) }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    TextButton(
                                        onClick = { onDeleteReview(review.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Hapus")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMosqueSheet(
    lat: Double,
    lon: Double,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String, facilities: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedFacilities by remember { mutableStateOf(emptySet<String>()) }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Tambah Masjid / Musholla Baru",
                style = MaterialTheme.typography.headlineSmall
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Titik koordinat: ${String.format("%.5f, %.5f", lat, lon)} (dapat diubah dengan mengetuk peta terlebih dahulu)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 120) {
                        name = it
                        nameError = false
                    }
                },
                label = { Text("Nama Masjid / Musholla *") },
                placeholder = { Text("Contoh: Masjid Al-Muhajirin") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Nama masjid minimal 2 karakter") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_mosque_name_input")
            )

            OutlinedTextField(
                value = address,
                onValueChange = { if (it.length <= 250) address = it },
                label = { Text("Alamat / Patokan") },
                placeholder = { Text("Contoh: Jl. Sudirman No. 12, dekat SPBU") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_mosque_address_input")
            )

            Text(
                text = "Fasilitas Awal (${selectedFacilities.size} dipilih)",
                style = MaterialTheme.typography.titleSmall
            )
            FacilityChecklistSection(
                selectedIds = selectedFacilities,
                onToggle = { fid ->
                    selectedFacilities = if (fid in selectedFacilities) {
                        selectedFacilities - fid
                    } else {
                        selectedFacilities + fid
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        if (name.trim().length < 2) {
                            nameError = true
                        } else {
                            onSubmit(name.trim(), address.trim(), selectedFacilities.toList())
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("submit_add_mosque_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Tambahkan Masjid")
                    }
                }
            }
        }
    }
}

@Composable
fun LocationPickerDialog(
    onUseGpsClick: () -> Unit,
    onSelectPreset: (LocationPreset) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilih Lokasi Pencarian",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onUseGpsClick()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deteksi GPS Saya Sekarang")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Atau lompat ke area kota populer:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OverpassRepository.PRESET_LOCATIONS.forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            onSelectPreset(preset)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(preset.name, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
