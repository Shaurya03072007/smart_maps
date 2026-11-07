package com.smartmaps.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmaps.app.ui.components.PlaceCard
import com.smartmaps.app.ui.components.ProviderCard
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.AiSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlace: (String) -> Unit,
    onNavigateToProvider: (String) -> Unit = {},
    onNavigateToMessages: (String) -> Unit = {},
    onNavigateToBooking: (String, String) -> Unit = { pId, _ -> onNavigateToProvider(pId) },
    initialQuery: String? = null,
    viewModel: AiSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.onQueryChange(initialQuery)
        }
    }

    val quickTags = listOf(
        "❄️ AC Repair",
        "🚿 Plumber",
        "⚡ Electrician",
        "🚗 Mechanic",
        "🫧 Washing Machine",
        "🧽 Deep Clean",
        "💻 Laptop Repair",
        "🏥 Hospital",
        "🍽️ Restaurant"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header / Instant Beast Search Bar ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
                    )
                )
                .padding(top = 48.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search vendors, problems or places...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Filled.Bolt, null, tint = Primary) },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search() })
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Instant Quick Tag Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickTags) { tag ->
                        val cleanQuery = tag.substringAfter(" ")
                        FilterChip(
                            selected = uiState.query.equals(cleanQuery, ignoreCase = true),
                            onClick = { viewModel.onQueryChange(cleanQuery) },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // ── Results Area ───────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Speed & AI Summary Header
                if (uiState.analysisText.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Primary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Speed, null, tint = Primary, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.analysisText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    uiState.intent?.let { intent ->
                                        if ((intent.estimatedLabourMin ?: 0) > 0 && uiState.query.isNotBlank()) {
                                            Text(
                                                text = "Estimated Service Charge: ₹${intent.estimatedLabourMin} – ₹${intent.estimatedLabourMax} · Duration: ${intent.estimatedDurationMin}–${intent.estimatedDurationMax} mins",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. AI Smart-Matched Vendors Section
                if (uiState.vendors.isNotEmpty()) {
                    item {
                        Text(
                            text = "🛠️ AI Smart-Matched Vendors (${uiState.vendors.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(uiState.vendors, key = { "v_${it.id}" }) { vendor ->
                        ProviderCard(
                            provider = vendor,
                            onClick = { onNavigateToProvider(vendor.id) },
                            onChatClick = {
                                viewModel.openVendorDirectChat(vendor.id) { bookingId ->
                                    onNavigateToMessages(bookingId)
                                }
                            },
                            onBookClick = {
                                val sId = uiState.intent?.service ?: vendor.services.firstOrNull() ?: "ac_repair"
                                onNavigateToBooking(vendor.id, sId)
                            }
                        )
                    }
                }

                // 2. Nearby Map Places Section
                if (uiState.places.isNotEmpty()) {
                    item {
                        Text(
                            text = "📍 Nearby Map Places (${uiState.places.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(uiState.places, key = { "p_${it.id}" }) { place ->
                        PlaceCard(
                            place = place,
                            onClick = { onNavigateToPlace(place.id) }
                        )
                    }
                }
            }

            uiState.error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }
                ) { Text(errorMsg) }
            }
        }
    }
}
