package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MosqueUiItem
import com.example.ui.theme.AmberStar
import com.example.ui.theme.EmeraldPrimary
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan
import okhttp3.OkHttpClient

private const val TILE_DP = 220f

@Composable
fun OsmMosqueMapView(
    searchLat: Double,
    searchLon: Double,
    radiusMeters: Int,
    mosques: List<MosqueUiItem>,
    selectedMosqueId: String?,
    onSelectMosque: (MosqueUiItem) -> Unit,
    onMapTapCoordinate: (Double, Double) -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tileSizePx = with(density) { TILE_DP.dp.toPx() }

    val osmImageLoader = remember(context) {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "SinggahMasjidAndroid/1.0")
                    .build()
                chain.proceed(req)
            }
            .build()
        ImageLoader.Builder(context)
            .okHttpClient(client)
            .build()
    }

    var viewLat by remember { mutableStateOf(searchLat) }
    var viewLon by remember { mutableStateOf(searchLon) }
    var zoom by remember { mutableIntStateOf(if (radiusMeters > 5000) 13 else 14) }

    LaunchedEffect(searchLat, searchLon) {
        viewLat = searchLat
        viewLon = searchLon
    }

    LaunchedEffect(radiusMeters) {
        zoom = when {
            radiusMeters <= 1500 -> 15
            radiusMeters <= 3000 -> 14
            radiusMeters <= 5000 -> 13
            else -> 12
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(zoom, viewLat, viewLon, tileSizePx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val worldSizePx = tileSizePx * (1 shl zoom)
                        val centerWorldX = lonToWorldX(viewLon) * worldSizePx
                        val centerWorldY = latToWorldY(viewLat) * worldSizePx
                        val nextWorldX = (centerWorldX - dragAmount.x).coerceIn(0f, worldSizePx)
                        val nextWorldY = (centerWorldY - dragAmount.y).coerceIn(0f, worldSizePx)
                        viewLon = worldXToLon(nextWorldX / worldSizePx)
                        viewLat = worldYToLat(nextWorldY / worldSizePx)
                    }
                }
                .pointerInput(zoom, viewLat, viewLon, tileSizePx) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val worldSizePx = tileSizePx * (1 shl zoom)
                        val centerWorldX = lonToWorldX(viewLon) * worldSizePx
                        val centerWorldY = latToWorldY(viewLat) * worldSizePx
                        val tappedWorldX = (centerWorldX + (tapOffset.x - w / 2f)).coerceIn(0f, worldSizePx)
                        val tappedWorldY = (centerWorldY + (tapOffset.y - h / 2f)).coerceIn(0f, worldSizePx)
                        val tapLon = worldXToLon(tappedWorldX / worldSizePx)
                        val tapLat = worldYToLat(tappedWorldY / worldSizePx)
                        onMapTapCoordinate(tapLat, tapLon)
                    }
                }
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val numTiles = 1 shl zoom
            val worldSizePx = tileSizePx * numTiles
            val centerWorldX = lonToWorldX(viewLon) * worldSizePx
            val centerWorldY = latToWorldY(viewLat) * worldSizePx

            // Architectural fallback grid behind tiles
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color(0xFFEAF2EE))
                val step = 48.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = Color(0xFFD5E4DD),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color(0xFFD5E4DD),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }

            // Visible OpenStreetMap raster tiles
            val centerTileX = (centerWorldX / tileSizePx).toInt()
            val centerTileY = (centerWorldY / tileSizePx).toInt()
            val tilesXRadius = (widthPx / (2f * tileSizePx)).toInt() + 1
            val tilesYRadius = (heightPx / (2f * tileSizePx)).toInt() + 1

            for (tx in (centerTileX - tilesXRadius)..(centerTileX + tilesXRadius)) {
                for (ty in (centerTileY - tilesYRadius)..(centerTileY + tilesYRadius)) {
                    if (ty < 0 || ty >= numTiles) continue
                    val wrappedX = ((tx % numTiles) + numTiles) % numTiles
                    val screenX = widthPx / 2f + (tx * tileSizePx - centerWorldX)
                    val screenY = heightPx / 2f + (ty * tileSizePx - centerWorldY)
                    val tileUrl = "https://tile.openstreetmap.org/$zoom/$wrappedX/$ty.png"

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tileUrl)
                            .crossfade(true)
                            .build(),
                        imageLoader = osmImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(TILE_DP.dp)
                            .offset {
                                IntOffset(screenX.roundToInt(), screenY.roundToInt())
                            }
                    )
                }
            }

            // Search Radius Circle + Anchor Marker
            val anchorWorldX = lonToWorldX(searchLon) * worldSizePx
            val anchorWorldY = latToWorldY(searchLat) * worldSizePx
            val anchorScreenX = widthPx / 2f + (anchorWorldX - centerWorldX)
            val anchorScreenY = heightPx / 2f + (anchorWorldY - centerWorldY)

            val metersPerPixel =
                156543.03392 * cos(Math.toRadians(searchLat)) / (2.0.pow(zoom.toDouble())) * (256.0 / tileSizePx)
            val radiusPx = (radiusMeters / metersPerPixel).toFloat().coerceIn(24f, 1600f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    radius = radiusPx,
                    center = Offset(anchorScreenX, anchorScreenY)
                )
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.55f),
                    radius = radiusPx,
                    center = Offset(anchorScreenX, anchorScreenY),
                    style = Stroke(width = 3f)
                )
                // User Search Anchor Pin
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = 0.25f),
                    radius = 24f,
                    center = Offset(anchorScreenX, anchorScreenY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = Offset(anchorScreenX, anchorScreenY)
                )
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 8.5f,
                    center = Offset(anchorScreenX, anchorScreenY)
                )
            }

            // Mosque Pins
            mosques.take(30).forEach { mosque ->
                val mWorldX = lonToWorldX(mosque.lon) * worldSizePx
                val mWorldY = latToWorldY(mosque.lat) * worldSizePx
                val mScreenX = widthPx / 2f + (mWorldX - centerWorldX)
                val mScreenY = heightPx / 2f + (mWorldY - centerWorldY)

                if (mScreenX in -60f..(widthPx + 60f) && mScreenY in -60f..(heightPx + 60f)) {
                    val isSelected = mosque.id == selectedMosqueId
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (isSelected) 6.dp else 3.dp,
                        border = BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (mScreenX - 24f).roundToInt(),
                                    (mScreenY - 24f).roundToInt()
                                )
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMosque(mosque) }
                            .testTag("map_pin_${mosque.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mosque,
                                contentDescription = mosque.name,
                                modifier = Modifier.size(14.dp)
                            )
                            if (mosque.reviewCount > 0) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = AmberStar,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = String.format("%.1f", mosque.avgRating),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Top Hint Overlay
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Ketuk peta untuk pindah titik cari",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Zoom & Center Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { zoom = (zoom + 1).coerceAtMost(17) },
                    modifier = Modifier.size(40.dp).testTag("map_zoom_in")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Perbesar peta")
                }
                FilledTonalIconButton(
                    onClick = { zoom = (zoom - 1).coerceAtLeast(11) },
                    modifier = Modifier.size(40.dp).testTag("map_zoom_out")
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Perkecil peta")
                }
                FilledTonalIconButton(
                    onClick = {
                        viewLat = searchLat
                        viewLon = searchLon
                        onRecenter()
                    },
                    modifier = Modifier.size(40.dp).testTag("map_recenter")
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = "Pusatkan ke lokasi")
                }
            }

            // Attribution
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "© OpenStreetMap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun lonToWorldX(lon: Double): Float {
    return ((lon + 180.0) / 360.0).toFloat()
}

private fun latToWorldY(lat: Double): Float {
    val clamped = lat.coerceIn(-85.0511, 85.0511)
    val rad = Math.toRadians(clamped)
    return ((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0).toFloat()
}

private fun worldXToLon(normX: Float): Double {
    return normX * 360.0 - 180.0
}

private fun worldYToLat(normY: Float): Double {
    val n = PI - 2.0 * PI * normY
    return Math.toDegrees(atan(sinh(n)))
}
