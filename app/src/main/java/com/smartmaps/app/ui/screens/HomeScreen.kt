package com.smartmaps.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmaps.app.data.local.entity.ServiceCategoryEntity
import com.smartmaps.app.ui.components.*
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToProvider: (String) -> Unit,
    onNavigateToPlace: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToAiSearch: () -> Unit,
    onNavigateToProviderRegistration: () -> Unit,
    onNavigateToBooking: (String, String) -> Unit = { pId, _ -> onNavigateToProvider(pId) },
    onNavigateToMessages: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
                ) {
                    Column {
                        // Location
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                null,
                                tint = Tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = uiState.locationAddress.ifBlank { "Locating..." },
                                style = MaterialTheme.typography.labelMedium,
                                color = Tertiary
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                null,
                                tint = Tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Title
                        Text(
                            "SmartMaps Vendor Hub",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Google ADK AI Vendor Match · Instant Beast Search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        // Search bar — tapping opens Beast Hybrid Search Screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .border(
                                    1.dp,
                                    Primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable { onNavigateToAiSearch() }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Bolt,
                                    null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Search vendors, AC repair, plumber, places...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Quick actions ────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip(
                        icon = Icons.Filled.SmartToy,
                        label = "ADK Concierge",
                        onClick = onNavigateToChat,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        icon = Icons.Filled.Engineering,
                        label = "All Vendors",
                        onClick = onNavigateToExplore,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        icon = Icons.Filled.Map,
                        label = "Live Map",
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Service Categories ───────────────────────────────────
            if (uiState.categories.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Service Categories",
                        actionLabel = "All Services",
                        onAction = onNavigateToExplore,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    ServiceCategoriesRow(
                        categories = uiState.categories,
                        onCategoryClick = { _ ->
                            onNavigateToExplore()
                        }
                    )
                }
            }

            // ── AI Smart-Matched Nearby Vendors ──────────────────────
            if (uiState.nearbyProviders.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "AI Smart-Matched Vendors (${uiState.nearbyProviders.size})",
                        actionLabel = "See all",
                        onAction = onNavigateToExplore,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.nearbyProviders.take(8), key = { it.id }) { provider ->
                            ProviderCard(
                                provider = provider,
                                onClick = { onNavigateToProvider(provider.id) },
                                onBookClick = {
                                    val serviceId = provider.services.firstOrNull() ?: "general"
                                    onNavigateToBooking(provider.id, serviceId)
                                },
                                onChatClick = {
                                    viewModel.startVendorChat(provider.id) { bookingId ->
                                        onNavigateToMessages(bookingId)
                                    }
                                },
                                modifier = Modifier.width(320.dp)
                            )
                        }
                    }
                }
            } else if (uiState.currentLocation != null && uiState.nearbyProviders.isEmpty() && !uiState.isLoading) {
                item {
                    SectionHeader(
                        title = "Nearby Vendors",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔧", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No vendors registered in this zone yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onNavigateToProviderRegistration() }) {
                                Text("Register as a Vendor", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // ── Nearby Places ─────────────────────────────────────────
            if (uiState.nearbyPlaces.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Nearby Map Places",
                        actionLabel = "Explore map",
                        onAction = onNavigateToMap,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.nearbyPlaces.take(5), key = { it.id }) { place ->
                            PlaceCard(
                                place = place,
                                onClick = { onNavigateToPlace(place.id) },
                                modifier = Modifier.width(290.dp)
                            )
                        }
                    }
                }
            }

            // ── Loading ───────────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    LoadingContent(modifier = Modifier.padding(32.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ServiceCategoriesRow(
    categories: List<ServiceCategoryEntity>,
    onCategoryClick: (ServiceCategoryEntity) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val color = CategoryColors[category.id] ?: Primary
            Card(
                onClick = { onCategoryClick(category) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(category.icon, fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        category.name.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
