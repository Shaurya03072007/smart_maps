package com.smartmaps.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmaps.app.ui.components.*
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToProvider: (String) -> Unit,
    onNavigateToPlace: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAiSearch: () -> Unit,
    onNavigateToProviderRegistration: () -> Unit,
    onNavigateToMessages: (String) -> Unit = {},
    onNavigateToBooking: (String, String) -> Unit = { pId, _ -> onNavigateToProvider(pId) },
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ã¢â€â‚¬Ã¢â€â‚¬ Top Bar Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Explore",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 32.dp, bottom = 12.dp)
            )

            // Modern animated TabRow
            TabRow(
                selectedTabIndex = if (uiState.tab == ExploreTab.SERVICES) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(
                                tabPositions[if (uiState.tab == ExploreTab.SERVICES) 0 else 1]
                            ),
                            color = Primary
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = uiState.tab == ExploreTab.SERVICES,
                    onClick = { viewModel.onTabChange(ExploreTab.SERVICES) },
                    text = { Text("🛠️ Services", fontWeight = FontWeight.Medium) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = uiState.tab == ExploreTab.PLACES,
                    onClick = { viewModel.onTabChange(ExploreTab.PLACES) },
                    text = { Text("📍 Places", fontWeight = FontWeight.Medium) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            // Search bar
            SmartSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = { viewModel.search() },
                placeholder = when (uiState.tab) {
                    ExploreTab.SERVICES -> "Search services or describe your problem..."
                    ExploreTab.PLACES -> "Restaurants, hospitals, ATMs..."
                }
            )
        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Filters Row Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Radius chips
            item {
                FilterChip(
                    selected = uiState.radiusKm == 2.0,
                    onClick = { viewModel.onRadiusChange(2.0); viewModel.search() },
                    label = { Text("2 km") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.radiusKm == 5.0,
                    onClick = { viewModel.onRadiusChange(5.0); viewModel.search() },
                    label = { Text("5 km") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.radiusKm == 10.0,
                    onClick = { viewModel.onRadiusChange(10.0); viewModel.search() },
                    label = { Text("10 km") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Sort chip
            item {
                FilterChip(
                    selected = uiState.sortByDistance,
                    onClick = { viewModel.toggleSortByDistance(); viewModel.search() },
                    label = { Text("Nearest first") },
                    leadingIcon = if (uiState.sortByDistance) {
                        { Icon(Icons.Filled.NearMe, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Place category chips (only for places tab)
            if (uiState.tab == ExploreTab.PLACES) {
                items(uiState.placeCategories.take(10)) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = {
                            viewModel.onCategorySelect(if (uiState.selectedCategory == category) null else category)
                            viewModel.search()
                        },
                        label = { Text(category.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Service category chips
            if (uiState.tab == ExploreTab.SERVICES) {
                items(uiState.serviceCategories) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat.id,
                        onClick = {
                            viewModel.onCategorySelect(if (uiState.selectedCategory == cat.id) null else cat.id)
                            viewModel.searchServices(cat.name)
                        },
                        label = { Text("${cat.icon} ${cat.name.split(" ").first()}") }
                    )
                }
            }
        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Results Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> LoadingContent(modifier = Modifier.padding(32.dp))

                uiState.tab == ExploreTab.PLACES -> {
                    if (uiState.places.isEmpty() && uiState.searchQuery.isNotBlank()) {
                        EmptyState(
                            icon = { Text("📍", fontSize = 48.sp) },
                            title = "No places found",
                            subtitle = "Try a different category or increase the search radius",
                            action = {
                                Button(onClick = onNavigateToAiSearch) {
                                    Icon(Icons.Filled.AutoAwesome, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ask AI to find it")
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.places.isNotEmpty()) {
                                item {
                                    Text(
                                        "${uiState.places.size} places found",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            items(uiState.places, key = { it.id }) { place ->
                                PlaceCard(
                                    place = place,
                                    onClick = { onNavigateToPlace(place.id) }
                                )
                            }

                            // Default state Ã¢â‚¬â€ show category grid to prompt user
                            if (uiState.places.isEmpty()) {
                                item {
                                    PlaceCategoryGrid(
                                        onCategoryClick = { category ->
                                            viewModel.onCategorySelect(category)
                                            viewModel.search()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.tab == ExploreTab.SERVICES -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.providers.isNotEmpty()) {
                            item {
                                Text(
                                    "${uiState.providers.size} providers nearby",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(uiState.providers, key = { it.id }) { provider ->
                                ProviderCard(
                                    provider = provider,
                                    onClick = { onNavigateToProvider(provider.id) },
                                    onChatClick = {
                                        viewModel.openVendorDirectChat(provider.id) { bookingId ->
                                            onNavigateToMessages(bookingId)
                                        }
                                    },
                                    onBookClick = {
                                        val sId = provider.services.firstOrNull() ?: "ac_repair"
                                        onNavigateToBooking(provider.id, sId)
                                    }
                                )
                            }
                        }

                        if (uiState.services.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Matching Services",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(uiState.services) { service ->
                                ServiceListItem(service = service, onClick = {
                                    viewModel.onSearchQueryChange(service.name)
                                    viewModel.search()
                                })
                            }
                        }

                        if (uiState.providers.isEmpty() && uiState.services.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = { Text("📍", fontSize = 48.sp) },
                                    title = "No providers found",
                                    subtitle = "No service providers are registered in your area yet. Be the first!",
                                    action = {
                                        Button(onClick = onNavigateToProviderRegistration) {
                                            Text("Register as Provider")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }
                ) { Text(error) }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = if (selected) Primary else Color.Transparent
    val textColor = if (selected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PlaceCategoryGrid(onCategoryClick: (String) -> Unit) {
    val categories = listOf(
        "🍔" to "food", "☕" to "cafe", "🏥" to "hospital",
        "💊" to "pharmacy", "🏧" to "atm", "🏦" to "bank",
        "🛒" to "supermarket", "⛽" to "fuel", "🏨" to "hotel",
        "🏋️" to "gym", "🌳" to "park", "🍿" to "cinema",
        "💇" to "salon", "🐾" to "vet", "🚏" to "bus_stop"
    )
    Column {
        SectionHeader(title = "Browse Categories", modifier = Modifier.padding(bottom = 16.dp))
        categories.chunked(5).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowItems.forEach { (emoji, key) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onCategoryClick(key) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ServiceListItem(
    service: com.smartmaps.app.data.local.entity.ServiceEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(service.icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    "Ã¢â€šÂ¹${service.estimatedLabourMin}Ã¢â‚¬â€œÃ¢â€šÂ¹${service.estimatedLabourMax}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Tertiary
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}




