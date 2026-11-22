package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Accessible
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class FacilityCategory(val title: String) {
    MUSAFIR("Fasilitas Utama Musafir"),
    KENYAMANAN("Kenyamanan & Ibadah"),
    INKLUSIF("Layanan & Keamanan")
}

enum class MosqueFacility(
    val id: String,
    val label: String,
    val shortLabel: String,
    val category: FacilityCategory,
    val icon: ImageVector
) {
    // 5 Fasilitas utama permintaan pengguna
    WIFI(
        id = "wifi",
        label = "WiFi Gratis",
        shortLabel = "WiFi Gratis",
        category = FacilityCategory.MUSAFIR,
        icon = Icons.Rounded.Wifi
    ),
    MINUMAN(
        id = "minuman",
        label = "Minuman Kemasan Gratis",
        shortLabel = "Minuman Gratis",
        category = FacilityCategory.MUSAFIR,
        icon = Icons.Rounded.LocalDrink
    ),
    ISTIRAHAT(
        id = "istirahat",
        label = "Tempat Istirahat Khusus",
        shortLabel = "Ruang Istirahat",
        category = FacilityCategory.MUSAFIR,
        icon = Icons.Rounded.Weekend
    ),
    MENGINAP(
        id = "menginap",
        label = "Boleh Singgah Menginap",
        shortLabel = "Boleh Menginap",
        category = FacilityCategory.MUSAFIR,
        icon = Icons.Rounded.Bedtime
    ),
    SHOWER(
        id = "shower",
        label = "Shower Untuk Mandi",
        shortLabel = "Shower Mandi",
        category = FacilityCategory.MUSAFIR,
        icon = Icons.Rounded.Shower
    ),

    // Fasilitas tambahan yang biasa ada di masjid
    AC(
        id = "ac",
        label = "Ruang Sholat Ber-AC",
        shortLabel = "Ber-AC",
        category = FacilityCategory.KENYAMANAN,
        icon = Icons.Rounded.AcUnit
    ),
    PARKIR(
        id = "parkir",
        label = "Parkir Luas & Aman",
        shortLabel = "Parkir Luas",
        category = FacilityCategory.KENYAMANAN,
        icon = Icons.Rounded.LocalParking
    ),
    TOILET(
        id = "toilet",
        label = "Toilet Bersih Terpisah",
        shortLabel = "Toilet Bersih",
        category = FacilityCategory.KENYAMANAN,
        icon = Icons.Rounded.Wc
    ),
    WUDHU(
        id = "wudhu",
        label = "Tempat Wudhu Tertutup",
        shortLabel = "Wudhu Tertutup",
        category = FacilityCategory.KENYAMANAN,
        icon = Icons.Rounded.WaterDrop
    ),
    CHARGER(
        id = "charger",
        label = "Stop Kontak / Charger HP",
        shortLabel = "Charger HP",
        category = FacilityCategory.KENYAMANAN,
        icon = Icons.Rounded.ElectricalServices
    ),
    PERLENGKAPAN(
        id = "perlengkapan",
        label = "Mukena & Sarung Bersih",
        shortLabel = "Mukena/Sarung",
        category = FacilityCategory.KENYAMANAN,
        icon = Icons.Rounded.Checkroom
    ),
    DISABILITAS(
        id = "disabilitas",
        label = "Akses Kursi Roda / Lansia",
        shortLabel = "Ramah Lansia",
        category = FacilityCategory.INKLUSIF,
        icon = Icons.AutoMirrored.Rounded.Accessible
    ),
    RAMAH_ANAK(
        id = "ramah_anak",
        label = "Area Anak / Ruang Laktasi",
        shortLabel = "Ramah Anak",
        category = FacilityCategory.INKLUSIF,
        icon = Icons.Rounded.ChildCare
    ),
    MAKANAN(
        id = "makanan",
        label = "Takjil / Makanan Gratis",
        shortLabel = "Makanan Gratis",
        category = FacilityCategory.INKLUSIF,
        icon = Icons.Rounded.Restaurant
    ),
    KEAMANAN(
        id = "keamanan",
        label = "CCTV & Penjaga 24 Jam",
        shortLabel = "Jaga 24 Jam",
        category = FacilityCategory.INKLUSIF,
        icon = Icons.Rounded.Shield
    ),
    PERPUSTAKAAN(
        id = "perpustakaan",
        label = "Perpustakaan / Kajian Rutin",
        shortLabel = "Kajian/Buku",
        category = FacilityCategory.INKLUSIF,
        icon = Icons.AutoMirrored.Rounded.MenuBook
    );

    companion object {
        fun fromId(id: String): MosqueFacility? = entries.firstOrNull { it.id == id }
    }
}
