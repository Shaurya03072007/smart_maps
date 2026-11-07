package com.smartmaps.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmaps.app.data.remote.model.BookingStatus
import com.smartmaps.app.data.remote.model.LatLng
import com.smartmaps.app.data.remote.model.Place
import com.smartmaps.app.data.remote.model.ProviderInfo
import com.smartmaps.app.ui.components.*
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.BookingViewModel
import com.smartmaps.app.ui.viewmodel.HomeViewModel
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────
// Custom Map Pin Bitmap Generators (Live GPS + Vendor Pointers)
// ─────────────────────────────────────────────────────────────

private fun createUserLiveGpsBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = (140 * density).toInt()
    val height = (68 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0984E3")
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 11.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val pillRect = RectF(6f * density, 4f * density, width - 6f * density, 28f * density)
    canvas.drawRoundRect(pillRect, 12f * density, 12f * density, pillPaint)
    canvas.drawRoundRect(pillRect, 12f * density, 12f * density, borderPaint)
    canvas.drawText("📍 YOU (Live GPS)", width / 2f, 20f * density, textPaint)

    // Pulsing outer halo + inner dot at bottom center
    val cx = width / 2f
    val cy = height - 16f * density
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#550984E3")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 15f * density, haloPaint)
    canvas.drawCircle(cx, cy, 8f * density, pillPaint)
    canvas.drawCircle(cx, cy, 8f * density, borderPaint)
    return bitmap
}

private fun createVendorPointerBitmap(
    context: Context,
    label: String,
    rating: Float,
    isSelected: Boolean,
    isPlace: Boolean = false
): Bitmap {
    val density = context.resources.displayMetrics.density
    val shortName = if (label.length > 15) label.take(14) + "…" else label
    val display = if (isPlace) "📌 $shortName" else "🛠️ $shortName ★${"%.1f".format(rating)}"

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (if (isSelected) 12.5f else 11f) * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(display)
    val padH = 12f * density
    val pillW = (textWidth + padH * 2).coerceAtLeast(80f * density)
    val pillH = (if (isSelected) 32f else 28f) * density
    val pointerH = 10f * density

    val width = (pillW + 8f * density).toInt()
    val height = (pillH + pointerH + 8f * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = when {
        isSelected -> android.graphics.Color.parseColor("#00B894") // Bright Emerald when pointed out
        isPlace -> android.graphics.Color.parseColor("#E17055")    // Warm Orange for places
        else -> android.graphics.Color.parseColor("#6C5CE7")       // Deep Purple for vendors
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.parseColor("#FFEAA7") else android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = (if (isSelected) 2.5f else 1.5f) * density
    }

    val left = 4f * density
    val top = 3f * density
    val right = width - 4f * density
    val bottom = top + pillH
    val rect = RectF(left, top, right, bottom)

    canvas.drawRoundRect(rect, 12f * density, 12f * density, bgPaint)
    canvas.drawRoundRect(rect, 12f * density, 12f * density, strokePaint)

    // Pointer triangle at bottom center pointing to exact map coordinate
    val cx = width / 2f
    val path = Path().apply {
        moveTo(cx - 7f * density, bottom - 1f)
        lineTo(cx + 7f * density, bottom - 1f)
        lineTo(cx, bottom + pointerH)
        close()
    }
    canvas.drawPath(path, bgPaint)

    val textY = top + (pillH / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(display, left + padH, textY, textPaint)
    return bitmap
}

// ─────────────────────────────────────────────────────────────
// Interactive Live Vendor & GPS Radar Map Screen
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToProvider: (String) -> Unit,
    onNavigateToPlace: (String) -> Unit,
    onNavigateToBooking: (String, String) -> Unit = { pId, _ -> onNavigateToProvider(pId) },
    onNavigateToMessages: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var mapSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedProvider by remember { mutableStateOf<ProviderInfo?>(null) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleReady by remember { mutableStateOf(false) }

    // Request live GPS immediately when Map opens
    LaunchedEffect(Unit) {
        viewModel.refreshLiveLocation { liveLoc ->
            mapInstance?.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(org.maplibre.android.geometry.LatLng(liveLoc.lat, liveLoc.lng))
                        .zoom(13.8)
                        .build()
                ),
                900
            )
        }
    }

    // Helper function to fly camera to a specific vendor and point them out on the map
    fun pointOutVendorOnMap(vendor: ProviderInfo) {
        selectedProvider = vendor
        selectedPlace = null
        val lat = vendor.latLng.lat
        val lng = vendor.latLng.lng
        mapInstance?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(org.maplibre.android.geometry.LatLng(lat, lng))
                    .zoom(15.5)
                    .build()
            ),
            850
        )
    }

    // Reactively redraw custom markers whenever style finishes loading OR vendors/location/selection changes
    LaunchedEffect(
        isStyleReady,
        uiState.currentLocation,
        uiState.nearbyProviders,
        uiState.nearbyPlaces,
        selectedProvider,
        selectedPlace
    ) {
        val map = mapInstance ?: return@LaunchedEffect
        if (!isStyleReady) return@LaunchedEffect

        map.clear()
        val iconFactory = IconFactory.getInstance(context)

        // 1. User Live GPS Marker
        val userLoc = uiState.currentLocation ?: LatLng(17.3850, 78.4867)
        val userIcon = iconFactory.fromBitmap(createUserLiveGpsBitmap(context))
        map.addMarker(
            MarkerOptions()
                .position(org.maplibre.android.geometry.LatLng(userLoc.lat, userLoc.lng))
                .title("📍 Your Live GPS Location")
                .snippet("user_gps")
                .icon(userIcon)
        )

        // 2. Vendor Pointer Markers
        uiState.nearbyProviders.forEach { provider ->
            val safeName = (provider.businessName as String?).orEmpty().ifBlank {
                (provider.name as String?).orEmpty().ifBlank { "Vendor" }
            }
            val isSel = selectedProvider?.id == provider.id
            val vendorBmp = createVendorPointerBitmap(
                context = context,
                label = safeName,
                rating = provider.rating,
                isSelected = isSel,
                isPlace = false
            )
            val marker = map.addMarker(
                MarkerOptions()
                    .position(org.maplibre.android.geometry.LatLng(provider.latLng.lat, provider.latLng.lng))
                    .title("🛠️ $safeName (${provider.matchScore}% AI Match)")
                    .snippet("provider_${provider.id}")
                    .icon(iconFactory.fromBitmap(vendorBmp))
            )
            if (isSel) {
                marker.showInfoWindow(map, MapView(context))
            }
        }

        // 3. Place Markers
        uiState.nearbyPlaces.take(8).forEach { place ->
            val placeBmp = createVendorPointerBitmap(
                context = context,
                label = place.name,
                rating = place.rating,
                isSelected = selectedPlace?.id == place.id,
                isPlace = true
            )
            map.addMarker(
                MarkerOptions()
                    .position(org.maplibre.android.geometry.LatLng(place.latLng.lat, place.latLng.lng))
                    .title("📌 ${place.name}")
                    .snippet("place_${place.id}")
                    .icon(iconFactory.fromBitmap(placeBmp))
            )
        }

        map.setOnMarkerClickListener { marker ->
            val snippet = marker.snippet ?: return@setOnMarkerClickListener false
            when {
                snippet.startsWith("provider_") -> {
                    val pId = snippet.removePrefix("provider_")
                    uiState.nearbyProviders.find { it.id == pId }?.let { v ->
                        pointOutVendorOnMap(v)
                    }
                    true
                }
                snippet.startsWith("place_") -> {
                    val pId = snippet.removePrefix("place_")
                    selectedPlace = uiState.nearbyPlaces.find { it.id == pId }
                    selectedProvider = null
                    true
                }
                else -> false
            }
        }
    }

    val mapFilterChips = listOf(
        null to "🛠️ All Vendors",
        "ac_repair" to "❄️ AC Repair",
        "plumbing_tap_repair" to "🚿 Plumber",
        "electrical_wiring" to "⚡ Electrician",
        "car_breakdown" to "🚗 Mechanic",
        "home_deep_clean" to "🧽 Cleaning"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. MapLibre Native Vector Map
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mapView ->
                    mapView.onCreate(null)
                    mapView.getMapAsync { map ->
                        mapInstance = map
                        val styleUrl = ctx.getString(com.smartmaps.app.R.string.map_style_url)
                        map.setStyle(styleUrl) {
                            isStyleReady = true
                            val targetLatLng = uiState.currentLocation ?: LatLng(17.3850, 78.4867)
                            val cameraPos = CameraPosition.Builder()
                                .target(org.maplibre.android.geometry.LatLng(targetLatLng.lat, targetLatLng.lng))
                                .zoom(13.6)
                                .build()
                            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos))
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Search Bar + Category Filter Chips Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // In-Map Vendor & Place Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = mapSearchQuery,
                        onValueChange = { q ->
                            mapSearchQuery = q
                            viewModel.searchOnMap(q, selectedCategory)
                        },
                        placeholder = {
                            Text(
                                "Search vendors on map (AC, Plumber, Mechanic...)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.searchOnMap(mapSearchQuery, selectedCategory)
                                uiState.nearbyProviders.firstOrNull()?.let { pointOutVendorOnMap(it) }
                            }
                        )
                    )
                    if (mapSearchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                mapSearchQuery = ""
                                selectedProvider = null
                                viewModel.searchOnMap("", selectedCategory)
                            }
                        ) {
                            Icon(Icons.Filled.Clear, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Quick Service Filter Chips on Map
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mapFilterChips) { (serviceId, label) ->
                    val isSel = selectedCategory == serviceId
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            selectedCategory = serviceId
                            selectedProvider = null
                            viewModel.searchOnMap(mapSearchQuery, serviceId)
                        },
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Right Side: Live GPS Recenter FAB + Fit All Vendors FAB
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    viewModel.refreshLiveLocation { loc ->
                        selectedProvider = null
                        mapInstance?.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(org.maplibre.android.geometry.LatLng(loc.lat, loc.lng))
                                    .zoom(14.5)
                                    .build()
                            ),
                            800
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Filled.MyLocation, "My Live GPS Location")
            }

            if (uiState.nearbyProviders.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val nextIdx = if (selectedProvider == null) 0 else {
                            (uiState.nearbyProviders.indexOfFirst { it.id == selectedProvider?.id } + 1) % uiState.nearbyProviders.size
                        }
                        pointOutVendorOnMap(uiState.nearbyProviders[nextIdx])
                    },
                    containerColor = Primary,
                    contentColor = Color.White,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Filled.Engineering, "Point Out Next Vendor")
                }
            }
        }

        // 4. Bottom Interactive Vendor Map Carousel ("Point Out Vendors on Map")
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // Selected Vendor / Place Callout Banner
            selectedProvider?.let { sel ->
                val safeBiz = (sel.businessName as String?).orEmpty().ifBlank { (sel.name as String?).orEmpty() }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, ColorAvailable)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🎯 Pointing Out: $safeBiz",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorAvailable
                            )
                            Text(
                                "${sel.matchScore}% AI Match · ★${sel.rating} · ₹${sel.priceRangeMin}–₹${sel.priceRangeMax} · ETA ~${sel.estimatedArrivalMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.startVendorChat(sel.id) { bookingId ->
                                        onNavigateToMessages(bookingId)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Chat", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = {
                                    val sId = (sel.services as List<String>?).orEmpty().firstOrNull() ?: "ac_repair"
                                    onNavigateToBooking(sel.id, sId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Book", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Horizontal Scrollable Vendor Strip at Bottom of Map
            if (uiState.nearbyProviders.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.nearbyProviders, key = { it.id }) { vendor ->
                        val isSel = selectedProvider?.id == vendor.id
                        val safeBiz = (vendor.businessName as String?).orEmpty().ifBlank { (vendor.name as String?).orEmpty() }
                        Card(
                            onClick = { pointOutVendorOnMap(vendor) },
                            modifier = Modifier.width(265.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            ),
                            border = BorderStroke(
                                if (isSel) 2.dp else 1.dp,
                                if (isSel) Primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🛠️ $safeBiz",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${vendor.matchScore}%",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "★ ${vendor.rating} (${vendor.completedJobs} jobs) · ₹${vendor.priceRangeMin}–₹${vendor.priceRangeMax}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Tertiary
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { pointOutVendorOnMap(vendor) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Locate", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.startVendorChat(vendor.id) { bookingId ->
                                                onNavigateToMessages(bookingId)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Chat", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = { onNavigateToProvider(vendor.id) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("View", style = MaterialTheme.typography.labelSmall)
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

// ─────────────────────────────────────────────────────────────
// Bookings Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun BookingsScreen(
    onNavigateToBookingDetail: (String) -> Unit,
    onNavigateToMessages: (String) -> Unit,
    onNavigateToReview: (String, String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            "My Bookings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp)
        )

        if (uiState.bookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No bookings yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Book a service to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.bookings, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        onClick = { onNavigateToBookingDetail(booking.id) },
                        onMessageClick = { onNavigateToMessages(booking.id) },
                        onReviewClick = { onNavigateToReview(booking.id, booking.providerId) },
                        onStatusUpdate = { status -> viewModel.updateStatus(booking.id, status) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: com.smartmaps.app.data.local.entity.BookingEntity,
    onClick: () -> Unit,
    onMessageClick: () -> Unit,
    onReviewClick: () -> Unit,
    onStatusUpdate: (BookingStatus) -> Unit
) {
    val status = runCatching { BookingStatus.valueOf(booking.status) }.getOrDefault(BookingStatus.PENDING)
    val statusColor = when (status) {
        BookingStatus.PENDING -> ColorWarning
        BookingStatus.ACCEPTED, BookingStatus.ON_THE_WAY, BookingStatus.ARRIVED, BookingStatus.IN_PROGRESS -> ColorVerified
        BookingStatus.COMPLETED -> ColorAvailable
        BookingStatus.REJECTED, BookingStatus.CANCELLED, BookingStatus.DISPUTED -> ColorUnavailable
    }
    val dateStr = remember(booking.scheduledAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(booking.scheduledAt))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Booking #${booking.id.take(8)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(status.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(booking.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (booking.estimatedCost > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CurrencyRupee, null, tint = Tertiary, modifier = Modifier.size(14.dp))
                    Text("${booking.estimatedCost} estimated", style = MaterialTheme.typography.bodySmall, color = Tertiary)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMessageClick,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", style = MaterialTheme.typography.labelMedium)
                }

                if (status == BookingStatus.COMPLETED) {
                    Button(
                        onClick = onReviewClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Review", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (status == BookingStatus.PENDING) {
                    OutlinedButton(
                        onClick = { onStatusUpdate(BookingStatus.CANCELLED) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ColorUnavailable),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium, color = ColorUnavailable)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Profile Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(
    onNavigateToProviderRegistration: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background))
                    )
                    .padding(top = 48.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Smart Maps User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("AI Vendor & Local Discovery", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))

                ProfileSection(title = "Account") {
                    ProfileItem(icon = Icons.Filled.Person, label = "Edit Profile", onClick = {})
                    ProfileItem(icon = Icons.Filled.LocationOn, label = "Saved Addresses", onClick = {})
                    ProfileItem(icon = Icons.Filled.Favorite, label = "Saved Places", onClick = {})
                    ProfileItem(icon = Icons.Filled.BookOnline, label = "My Bookings", onClick = {})
                }

                Spacer(Modifier.height(16.dp))

                ProfileSection(title = "Become a Provider") {
                    ProfileItem(
                        icon = Icons.Filled.Engineering,
                        label = "Register as Service Provider",
                        sublabel = "Earn money by offering your skills",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToProviderRegistration
                    )
                }

                Spacer(Modifier.height(16.dp))

                ProfileSection(title = "Settings & Support") {
                    ProfileItem(icon = Icons.Filled.Notifications, label = "Notifications", onClick = {})
                    ProfileItem(icon = Icons.Filled.Security, label = "Privacy", onClick = {})
                    ProfileItem(icon = Icons.Filled.Help, label = "Help & Support", onClick = {})
                    ProfileItem(icon = Icons.Filled.Info, label = "About Smart Maps", onClick = {})
                }

                Spacer(Modifier.height(16.dp))

                // Logout button
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLogout)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        Text("Logout", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Smart Maps v2.0 · Powered by Google ADK & OpenStreetMap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(content = content)
    }
}

@Composable
private fun ProfileItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = tint)
            sublabel?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
