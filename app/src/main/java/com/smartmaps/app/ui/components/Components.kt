package com.smartmaps.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.smartmaps.app.data.local.entity.ServiceCategoryEntity
import com.smartmaps.app.data.remote.model.Place
import com.smartmaps.app.data.remote.model.ProviderInfo
import com.smartmaps.app.ui.theme.*
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
// Rating Stars
// ─────────────────────────────────────────────────────────────
@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val fullStars = rating.toInt()
        val halfStar = (rating - fullStars) >= 0.5f
        repeat(5) { index ->
            val icon = when {
                index < fullStars -> Icons.Filled.Star
                index == fullStars && halfStar -> Icons.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Tertiary,
                modifier = Modifier.size(size)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Distance Chip
// ─────────────────────────────────────────────────────────────
@Composable
fun DistanceChip(distanceMeters: Double, modifier: Modifier = Modifier) {
    val text = if (distanceMeters >= 1000)
        "${"%.1f".format(distanceMeters / 1000)} km"
    else
        "${distanceMeters.roundToInt()} m"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(Icons.Filled.NearMe, null, tint = Secondary, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Secondary)
    }
}

// ─────────────────────────────────────────────────────────────
// AI Smart-Matched Vendor / Provider Card
// ─────────────────────────────────────────────────────────────
@Composable
fun ProviderCard(
    provider: ProviderInfo,
    onClick: () -> Unit,
    onCallClick: () -> Unit = {},
    onChatClick: (() -> Unit)? = null,
    onBookClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top AI Smart Match Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Primary, modifier = Modifier.size(13.dp))
                        Text(
                            "${provider.matchScore}% AI Match",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (provider.priceRangeMin > 0) {
                        Text(
                            "₹${provider.priceRangeMin}–₹${provider.priceRangeMax}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Tertiary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (provider.isAvailable) ColorAvailable.copy(alpha = 0.15f)
                        else ColorUnavailable.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (provider.isAvailable) "ETA ~${provider.estimatedArrivalMinutes}m" else "Busy",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (provider.isAvailable) ColorAvailable else ColorUnavailable,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                val safeName = (provider.name as String?).orEmpty()
                val safeBiz = (provider.businessName as String?).orEmpty().ifBlank { safeName }
                val safeAddress = (provider.address as String?).orEmpty()
                val safeMatchReason = (provider.matchReason as String?).orEmpty()
                val safeDesc = (provider.description as String?).orEmpty()
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = safeBiz.firstOrNull()?.toString() ?: "V",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = safeBiz,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (provider.isVerified) {
                            Icon(Icons.Filled.Verified, "Verified", tint = ColorVerified, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = "$safeName · ${provider.experienceYears} yrs exp · $safeAddress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RatingStars(provider.rating)
                        Text(
                            "${provider.rating} (${provider.reviewCount}) · ${provider.completedJobs} jobs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DistanceChip(provider.distanceMeters)
                    }
                }
            }

            val safeMatchReason2 = (provider.matchReason as String?).orEmpty()
            val safeDesc2 = (provider.description as String?).orEmpty()
            if (safeMatchReason2.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "💡 $safeMatchReason2",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (safeDesc2.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    safeDesc2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(10.dp))

            // Action buttons: Call, Chat, Book
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (provider.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Secondary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Secondary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", color = Secondary, style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedButton(
                    onClick = { onChatClick?.invoke() ?: onClick() },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Primary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Chat, null, tint = Primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", color = Primary, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onBookClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Filled.BookOnline, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Book", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Place Card
// ─────────────────────────────────────────────────────────────
@Composable
fun PlaceCard(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        place.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        place.categoryLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (place.rating > 0f) {
                            RatingStars(place.rating, size = 12.dp)
                            Text(
                                "%.1f".format(place.rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DistanceChip(place.distanceMeters)
                    }
                }

                IconButton(
                    onClick = {
                        val uri = Uri.parse("geo:${place.latLng.lat},${place.latLng.lng}?q=${place.latLng.lat},${place.latLng.lng}(${place.name})")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Filled.Directions, "Directions", tint = Secondary)
                }
            }

            if (place.address.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Text(
                        place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (place.openingHours.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.AccessTime, null, tint = ColorAvailable, modifier = Modifier.size(12.dp))
                    Text(
                        place.openingHours.take(40),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorAvailable
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Category Chip & Section Header
// ─────────────────────────────────────────────────────────────
@Composable
fun CategoryGrid(
    categories: List<ServiceCategoryEntity>,
    onCategoryClick: (ServiceCategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        categories.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryCard(
    category: ServiceCategoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = CategoryColors[category.id] ?: Primary

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(category.icon, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = Primary)
                Icon(Icons.Filled.ChevronRight, null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary)
            Spacer(Modifier.height(12.dp))
            Text("Loading...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        action?.let {
            Spacer(Modifier.height(16.dp))
            it()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String = "What do you need help with?",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                Row {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Send, "Search", tint = Primary)
                    }
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}
