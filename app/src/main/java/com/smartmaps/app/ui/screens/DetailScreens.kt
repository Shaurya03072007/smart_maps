package com.smartmaps.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmaps.app.data.local.entity.ProviderEntity
import com.smartmaps.app.data.remote.model.BookingStatus
import com.smartmaps.app.data.remote.model.Place
import com.smartmaps.app.data.remote.model.ProviderInfo
import com.smartmaps.app.data.repository.BookingRepository
import com.smartmaps.app.data.repository.PlaceRepository
import com.smartmaps.app.data.repository.ProviderRepository
import com.smartmaps.app.ui.components.*
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.BookingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// Provider Detail Screen
// ─────────────────────────────────────────────────────────────

data class ProviderDetailUiState(
    val provider: ProviderInfo? = null,
    val reviews: List<com.smartmaps.app.data.local.entity.ReviewEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProviderDetailViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderDetailUiState())
    val uiState: StateFlow<ProviderDetailUiState> = _uiState.asStateFlow()

    fun loadProvider(providerId: String) {
        viewModelScope.launch {
            val provider = providerRepository.getProviderById(providerId)
            _uiState.update { it.copy(provider = provider, isLoading = false) }
        }
        viewModelScope.launch {
            bookingRepository.getProviderReviews(providerId).collect { reviews ->
                _uiState.update { it.copy(reviews = reviews) }
            }
        }
    }

    fun startChatWithVendor(providerId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val bookingId = bookingRepository.getOrCreateDirectInquiryBooking(providerId)
            onReady(bookingId)
        }
    }
}

@Composable
fun ProviderDetailScreen(
    providerId: String,
    onNavigateToBooking: (String, String) -> Unit,
    onNavigateToMessages: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ProviderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(providerId) { viewModel.loadProvider(providerId) }

    if (uiState.isLoading) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val provider = uiState.provider ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        if (provider.matchScore > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, null, tint = Primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        "${provider.matchScore}% AI Smart Match",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val safeName = (provider.name as String?).orEmpty().ifBlank { "Verified Specialist" }
                    val safeBiz = (provider.businessName as String?).orEmpty().ifBlank { safeName }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Primary, Secondary))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                safeBiz.firstOrNull()?.toString() ?: "V",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    safeBiz,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (provider.isVerified) {
                                    Icon(
                                        Icons.Filled.Verified,
                                        "Verified",
                                        tint = ColorVerified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                "Specialist: $safeName · ${provider.experienceYears} yrs exp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RatingStars(provider.rating, size = 16.dp)
                                Text(
                                    "${provider.rating} (${provider.reviewCount}) · ${provider.completedJobs} jobs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (provider.isAvailable) ColorAvailable.copy(alpha = 0.15f)
                                            else ColorUnavailable.copy(alpha = 0.15f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        if (provider.isAvailable) "Online & Ready" else "Busy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (provider.isAvailable) ColorAvailable else ColorUnavailable
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Tertiary.copy(alpha = 0.15f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "₹${provider.priceRangeMin} – ₹${provider.priceRangeMax}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action buttons: Call, Chat with Vendor, Book Service
        item {
            val safePhone = (provider.phone as String?).orEmpty()
            val safeServices = (provider.services as List<String>?).orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (safePhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$safePhone")))
                        },
                        modifier = Modifier.weight(0.9f),
                        border = BorderStroke(1.dp, Secondary)
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Secondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", color = Secondary)
                    }
                }
                OutlinedButton(
                    onClick = {
                        viewModel.startChatWithVendor(provider.id) { bookingId ->
                            onNavigateToMessages(bookingId)
                        }
                    },
                    modifier = Modifier.weight(1.1f),
                    border = BorderStroke(1.dp, Primary)
                ) {
                    Icon(Icons.Filled.Chat, null, tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chat Vendor", color = Primary)
                }
                Button(
                    onClick = {
                        val serviceId = safeServices.firstOrNull() ?: "general"
                        onNavigateToBooking(provider.id, serviceId)
                    },
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Filled.BookOnline, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Book")
                }
            }
        }

        val safeMatchReason = (provider.matchReason as String?).orEmpty()
        if (safeMatchReason.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Recommend, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Text(
                            "AI Selection Reason: $safeMatchReason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Description
        val safeDescription = (provider.description as String?).orEmpty()
        val safeAddress = (provider.address as String?).orEmpty()
        if (safeDescription.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "About Vendor & Services",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            safeDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (safeAddress.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, tint = Secondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    safeAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("⭐", "${provider.rating}", "Rating", Modifier.weight(1f))
                StatCard("✅", "${provider.completedJobs}", "Jobs Done", Modifier.weight(1f))
                StatCard("⚡", "~${provider.estimatedArrivalMinutes}m", "ETA", Modifier.weight(1f))
            }
        }

        // Reviews
        if (uiState.reviews.isNotEmpty()) {
            item {
                SectionHeader(
                    "Customer Reviews (${uiState.reviews.size})",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(uiState.reviews.take(5)) { review ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        RatingStars(review.rating)
                        if (review.comment.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                review.comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Place Detail Screen
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository
) : ViewModel() {
    private val _place = MutableStateFlow<Place?>(null)
    val place: StateFlow<Place?> = _place.asStateFlow()

    fun loadPlace(placeId: String) {
        viewModelScope.launch {
            _place.value = placeRepository.getPlaceById(placeId)
        }
    }
}

@Composable
fun PlaceDetailScreen(
    placeId: String,
    onBack: () -> Unit,
    viewModel: PlaceDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val place by viewModel.place.collectAsState()

    LaunchedEffect(placeId) {
        viewModel.loadPlace(placeId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                place?.name ?: "Place Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    place?.name ?: "Map Location ($placeId)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val p = place
                if (p != null) {
                    Text(
                        "Category: ${p.categoryLabel.ifBlank { p.categoryKey }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                    if (p.address.isNotBlank()) {
                        Text("📍 ${p.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (p.openingHours.isNotBlank()) {
                        Text("🕒 Hours: ${p.openingHours}", style = MaterialTheme.typography.bodySmall, color = ColorAvailable)
                    }
                    if (p.phone.isNotBlank()) {
                        Text("📞 ${p.phone}", style = MaterialTheme.typography.bodySmall, color = Secondary)
                    }
                    Text(
                        "Coordinates: ${String.format("%.4f", p.latLng.lat)}, ${String.format("%.4f", p.latLng.lng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val p = place
                    val gmmIntentUri = if (p != null) {
                        Uri.parse("geo:${p.latLng.lat},${p.latLng.lng}?q=${Uri.encode(p.name)}")
                    } else {
                        Uri.parse("geo:0,0?q=$placeId")
                    }
                    context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Directions, null)
                Spacer(Modifier.width(6.dp))
                Text("Navigate on Maps")
            }
            val ph = place?.phone ?: ""
            if (ph.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ph")))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Call, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Call Place")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Booking Detail Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun BookingDetailScreen(
    bookingId: String,
    onNavigateToMessages: () -> Unit,
    onNavigateToReview: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val booking = uiState.bookings.find { it.id == bookingId }
    var quoteInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Vendor Service Booking",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        booking?.let { b ->
            val status = runCatching { BookingStatus.valueOf(b.status) }.getOrDefault(BookingStatus.PENDING)
            val statusColor = when (status) {
                BookingStatus.COMPLETED -> ColorAvailable
                BookingStatus.REJECTED, BookingStatus.CANCELLED -> ColorUnavailable
                else -> ColorVerified
            }
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Booking #${b.id.take(8)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(status.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Service: ${b.serviceId.replace("_", " ").uppercase()}", style = MaterialTheme.typography.labelMedium, color = Primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Address: ${b.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (b.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Notes: ${b.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (b.estimatedCost > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("Final / Estimated Quote: ₹${b.estimatedCost}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Tertiary)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    // Vendor Workflow Actions
                    Text(
                        "Live Vendor Job Progression:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val nextStatuses = when (status) {
                            BookingStatus.PENDING -> listOf(BookingStatus.ACCEPTED, BookingStatus.REJECTED)
                            BookingStatus.ACCEPTED -> listOf(BookingStatus.ON_THE_WAY)
                            BookingStatus.ON_THE_WAY -> listOf(BookingStatus.ARRIVED)
                            BookingStatus.ARRIVED -> listOf(BookingStatus.IN_PROGRESS)
                            BookingStatus.IN_PROGRESS -> listOf(BookingStatus.COMPLETED)
                            else -> emptyList()
                        }
                        items(nextStatuses) { nextStatus ->
                            Button(
                                onClick = {
                                    val cost = quoteInput.toIntOrNull()
                                    viewModel.updateStatus(b.id, nextStatus, cost)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nextStatus == BookingStatus.REJECTED) ColorUnavailable else Primary
                                )
                            ) {
                                Text(nextStatus.name.replace("_", " "), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (status != BookingStatus.COMPLETED && status != BookingStatus.REJECTED) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quoteInput,
                                onValueChange = { quoteInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Update Quote (₹)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    quoteInput.toIntOrNull()?.let { amt ->
                                        viewModel.updateStatus(b.id, status, amt)
                                        quoteInput = ""
                                    }
                                }
                            ) {
                                Text("Set ₹")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigateToMessages,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Icon(Icons.Filled.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Live Vendor Chat")
                }
                if (status == BookingStatus.COMPLETED) {
                    Button(
                        onClick = { onNavigateToReview(b.id, b.providerId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Tertiary)
                    ) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rate Vendor")
                    }
                }
            }
        } ?: run {
            Spacer(Modifier.height(24.dp))
            Text(
                "Booking not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Real 2-Way Vendor–User Messages Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun MessagesScreen(
    bookingId: String,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val booking = uiState.bookings.find { it.id == bookingId }
    val messagesFlow = remember(bookingId) { viewModel.getMessagesForBooking(bookingId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    // Allows user to send as Customer (with automatic smart vendor reply from backend) or test sending as Vendor
    var actAsVendor by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val customerQuickChips = listOf(
        "What is the final quote? 💰",
        "How soon can you arrive? 🛵",
        "Sharing my exact location 📍",
        "Is this urgent service available now? ⚡"
    )
    val vendorQuickChips = listOf(
        "Quote: ₹450 including visit & inspection 💰",
        "I am on my way, arriving in 15 mins 🛵",
        "Reached your location landmark 📍",
        "Job completed! Thank you ✅"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Header with Vendor Info + Customer/Vendor Role Switch
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 44.dp, end = 16.dp, bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Primary, Secondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Engineering, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Vendor Direct Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(Icons.Filled.Verified, null, tint = ColorVerified, modifier = Modifier.size(15.dp))
                            }
                            Text(
                                booking?.let { "Status: ${it.status} · Quote: ${if (it.estimatedCost > 0) "₹${it.estimatedCost}" else "Pending"}" }
                                    ?: "Real-Time Vendor Connection",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorAvailable
                            )
                        }
                    }

                    // Mode toggle chip so user can test both sides of the conversation
                    FilterChip(
                        selected = actAsVendor,
                        onClick = { actAsVendor = !actAsVendor },
                        label = {
                            Text(
                                if (actAsVendor) "Mode: Vendor 🔧" else "Mode: Customer 👤",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Secondary.copy(alpha = 0.2f),
                            selectedLabelColor = Secondary
                        )
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Handshake, null, tint = Primary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Connected with Verified Vendor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ask for a price quote, share your problem details, or confirm arrival ETA. The vendor replies in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isProviderMsg = msg.senderType == "provider" || msg.senderId == "provider"
                val timeFmt = remember(msg.createdAt) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.createdAt))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isProviderMsg) Arrangement.Start else Arrangement.End
                ) {
                    if (isProviderMsg) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔧", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isProviderMsg) 4.dp else 16.dp,
                            bottomEnd = if (isProviderMsg) 16.dp else 4.dp
                        ),
                        color = if (isProviderMsg) MaterialTheme.colorScheme.surface else Primary,
                        border = if (isProviderMsg) BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant) else null,
                        modifier = Modifier.widthIn(max = 285.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                if (isProviderMsg) "Verified Vendor" else "You (Customer)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isProviderMsg) Secondary else Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isProviderMsg) MaterialTheme.colorScheme.onSurface else Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                timeFmt,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isProviderMsg) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        // Quick Interaction Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = if (actAsVendor) vendorQuickChips else customerQuickChips
            items(chips) { chip ->
                SuggestionChip(
                    onClick = {
                        viewModel.sendMessage(
                            bookingId = bookingId,
                            text = chip,
                            senderType = if (actAsVendor) "provider" else "user"
                        )
                    },
                    label = { Text(chip, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (actAsVendor) "Reply as Vendor (e.g., Quote ₹450)..."
                        else "Message vendor (e.g., Need urgent repair)...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(
                            bookingId = bookingId,
                            text = inputText.trim(),
                            senderType = if (actAsVendor) "provider" else "user"
                        )
                        inputText = ""
                    }
                },
                containerColor = if (actAsVendor) Secondary else Primary,
                modifier = Modifier.size(46.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Provider Registration Screen (Synced with Agentic Backend)
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class ProviderRegistrationViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val serviceDao: com.smartmaps.app.data.local.dao.ServiceDao,
    private val locationRepository: com.smartmaps.app.data.location.LocationRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    val availableServices: StateFlow<List<com.smartmaps.app.data.local.entity.ServiceEntity>> =
        serviceDao.getAllServices().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentAddress = MutableStateFlow("")
    val currentAddress: StateFlow<String> = _currentAddress

    private val _currentLocation = MutableStateFlow<com.smartmaps.app.data.remote.model.LatLng?>(null)
    val currentLocation: StateFlow<com.smartmaps.app.data.remote.model.LatLng?> = _currentLocation

    init {
        viewModelScope.launch {
            locationRepository.currentLocation.filterNotNull().collect { latLng ->
                _currentLocation.value = latLng
                placeRepository.reverseGeocode(latLng).onSuccess { result ->
                    val addr = result.address
                    val displayName = listOfNotNull(
                        addr?.suburb ?: addr?.road,
                        addr?.city
                    ).joinToString(", ").ifBlank { result.displayName?.split(",")?.take(2)?.joinToString(",") ?: "" }
                    _currentAddress.value = displayName
                }
            }
        }
    }

    fun saveProvider(
        name: String,
        businessName: String,
        phone: String,
        email: String,
        description: String,
        lat: Double,
        lng: Double,
        address: String,
        radius: Double,
        selectedServiceIds: List<String>,
        expYears: Int = 0
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val entity = ProviderEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                businessName = businessName.ifBlank { name },
                phone = phone,
                email = email,
                description = description,
                latitude = lat,
                longitude = lng,
                address = address,
                serviceRadius = radius,
                experienceYears = expYears,
                isVerified = true,
                isAvailable = true,
                rating = 4.8f,
                reviewCount = 12,
                completedJobs = 25
            )
            providerRepository.saveProvider(entity, selectedServiceIds)
            _isSubmitting.value = false
            _saved.value = true
        }
    }
}

@Composable
fun ProviderRegistrationScreen(
    onBack: () -> Unit,
    viewModel: ProviderRegistrationViewModel = hiltViewModel()
) {
    val saved by viewModel.saved.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val availableServices by viewModel.availableServices.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    var name by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("5.0") }
    var experienceYears by remember { mutableStateOf("4") }
    var selectedServices by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(currentAddress) {
        if (currentAddress.isNotBlank() && address.isBlank()) {
            address = currentAddress
        }
    }

    LaunchedEffect(saved) { if (saved) onBack() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "Register Business / Vendor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Vendor Indexing", tint = Primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "SmartMaps AI Vendor Network",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Your vendor profile is indexed by our Google ADK AI Concierge & Beast Search to match you with nearby customers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            @Composable
            fun fieldColors() = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
            val fieldShape = RoundedCornerShape(12.dp)
            val modifier = Modifier.fillMaxWidth()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Specialist Name *") }, modifier = modifier, shape = fieldShape, colors = fieldColors())
                OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business / Shop Name") }, modifier = modifier, shape = fieldShape, colors = fieldColors())
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    modifier = modifier,
                    shape = fieldShape,
                    colors = fieldColors(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = modifier,
                    shape = fieldShape,
                    colors = fieldColors(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Specialties, Pricing & Experience") },
                    modifier = modifier,
                    shape = fieldShape,
                    colors = fieldColors(),
                    minLines = 3,
                    maxLines = 5
                )
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Service Area Address *") }, modifier = modifier, shape = fieldShape, colors = fieldColors())

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("Radius (km)") },
                        modifier = Modifier.weight(1f),
                        shape = fieldShape,
                        colors = fieldColors(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = experienceYears,
                        onValueChange = { experienceYears = it },
                        label = { Text("Exp (Years)") },
                        modifier = Modifier.weight(1f),
                        shape = fieldShape,
                        colors = fieldColors(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Select Services Offered",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                if (availableServices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableServices.chunked(2).forEach { rowServices ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                rowServices.forEach { service ->
                                    val isSelected = selectedServices.contains(service.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedServices = if (isSelected) selectedServices - service.id else selectedServices + service.id
                                        },
                                        label = { Text("${service.icon} ${service.name}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary.copy(alpha = 0.2f),
                                            selectedLabelColor = Primary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowServices.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Loading available services...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveProvider(
                            name = name,
                            businessName = businessName.ifBlank { name },
                            phone = phone,
                            email = email,
                            description = description,
                            lat = currentLocation?.lat ?: 17.3850,
                            lng = currentLocation?.lng ?: 78.4867,
                            address = address,
                            radius = radius.toDoubleOrNull() ?: 5.0,
                            selectedServiceIds = selectedServices.toList().ifEmpty { listOf("ac_repair") },
                            expYears = experienceYears.toIntOrNull() ?: 3
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && phone.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving Vendor Profile...")
                    } else {
                        Icon(Icons.Filled.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save & Publish Vendor Profile")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Create Booking Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun CreateBookingScreen(
    providerId: String,
    serviceId: String,
    onSuccess: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var address by remember { mutableStateOf("Banjara Hills, Hyderabad") }
    var notes by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Standard (Today)") }
    var scheduledDate by remember { mutableStateOf(System.currentTimeMillis() + 3600000) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Book Verified Vendor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(20.dp))

        @Composable
        fun fieldColors() = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Urgency Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Urgent (20 min)", "Standard (Today)", "Tomorrow").forEach { opt ->
                    FilterChip(
                        selected = urgency == opt,
                        onClick = { urgency = opt },
                        label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Service Address / Landmark *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Describe the issue for the vendor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                minLines = 3
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "💡 Once booked, a live 2-way chat opens immediately with your vendor to confirm the final quote & ETA.",
                style = MaterialTheme.typography.bodySmall,
                color = Secondary
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val combinedNotes = "[$urgency] $notes".trim()
                    viewModel.createBooking(
                        providerId = providerId,
                        serviceId = serviceId,
                        address = address,
                        latitude = 17.3850,
                        longitude = 78.4867,
                        scheduledAt = scheduledDate,
                        notes = combinedNotes,
                        estimatedCost = 450,
                        onSuccess = onSuccess
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = address.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.BookOnline, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm & Start Vendor Chat")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Write Review Screen
// ─────────────────────────────────────────────────────────────
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {
    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun submitReview(providerId: String, bookingId: String, rating: Float, comment: String) {
        viewModelScope.launch {
            bookingRepository.submitReview(providerId, bookingId, rating, comment)
                .onSuccess { _submitted.value = true }
                .onFailure { _error.value = it.message }
        }
    }
}

@Composable
fun WriteReviewScreen(
    bookingId: String,
    providerId: String,
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val submitted by viewModel.submitted.collectAsState()
    val error by viewModel.error.collectAsState()
    var rating by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(submitted) { if (submitted) onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Rate & Review Vendor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(24.dp))

        Text("Rating", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { star ->
                Icon(
                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$star stars",
                    tint = if (star <= rating) Tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { rating = star.toFloat() }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Your Review (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ColorUnavailable) }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.submitReview(providerId, bookingId, rating, comment) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Filled.Star, null)
            Spacer(Modifier.width(8.dp))
            Text("Submit Vendor Review")
        }
    }
}
