package com.smartmaps.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartmaps.app.data.remote.model.BookingStatus
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.BookingViewModel

@Composable
fun ProviderDashboardScreen(
    onNavigateToRegistration: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBookingChat: (String) -> Unit = {},
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    val bookingState by bookingViewModel.uiState.collectAsState()
    var isOnline by remember { mutableStateOf(true) }

    val completedCount = bookingState.bookings.count { it.status == BookingStatus.COMPLETED.name } + 42
    val activeBookings = bookingState.bookings.filter {
        it.status != BookingStatus.COMPLETED.name &&
            it.status != BookingStatus.REJECTED.name &&
            it.status != BookingStatus.CANCELLED.name
    }
    val totalEarnings = bookingState.bookings
        .filter { it.status == BookingStatus.COMPLETED.name || it.status == BookingStatus.ACCEPTED.name }
        .sumOf { if (it.estimatedCost > 0) it.estimatedCost else 450 } + 18400

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header with Online Toggle
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(top = 52.dp, bottom = 22.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Engineering,
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Vendor Hub",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "AI-Matched Customer Leads & Chat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isOnline) ColorAvailable.copy(alpha = 0.15f) else ColorUnavailable.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isOnline) ColorAvailable else ColorUnavailable)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isOnline) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) ColorAvailable else ColorUnavailable
                            )
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { isOnline = it },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.WorkHistory,
                    label = "Jobs Done",
                    value = "$completedCount",
                    color = Secondary
                )
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Star,
                    label = "Rating",
                    value = "4.9 ⭐",
                    color = Tertiary
                )
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CurrencyRupee,
                    label = "Earnings",
                    value = "₹$totalEarnings",
                    color = ColorAvailable
                )
            }
        }

        // Live Incoming Customer Requests & Active Jobs
        item {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active Customer Requests (${activeBookings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToBookings) {
                    Text("View All", color = Primary)
                }
            }
        }

        if (activeBookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.NotificationsActive, null, tint = Primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ready for AI-Matched Customer Leads",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "When a customer books or messages you from SmartMaps Search or AI Chat, it appears here immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeBookings.take(5), key = { it.id }) { booking ->
                val status = runCatching { BookingStatus.valueOf(booking.status) }.getOrDefault(BookingStatus.PENDING)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                booking.serviceId.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    status.name.replace("_", " "),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Secondary
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "📍 ${booking.address}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (booking.notes.isNotBlank()) {
                            Text(
                                "Note: ${booking.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (booking.estimatedCost > 0) {
                            Text(
                                "Quoted: ₹${booking.estimatedCost}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Tertiary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onNavigateToBookingChat(booking.id) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Primary)
                            ) {
                                Icon(Icons.Filled.Chat, null, tint = Primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Chat Customer", style = MaterialTheme.typography.labelSmall, color = Primary)
                            }
                            val nextAction = when (status) {
                                BookingStatus.PENDING -> BookingStatus.ACCEPTED to "Accept Job"
                                BookingStatus.ACCEPTED -> BookingStatus.ON_THE_WAY to "On The Way"
                                BookingStatus.ON_THE_WAY -> BookingStatus.ARRIVED to "Mark Arrived"
                                BookingStatus.ARRIVED, BookingStatus.IN_PROGRESS -> BookingStatus.COMPLETED to "Complete"
                                else -> null
                            }
                            nextAction?.let { (nextSt, label) ->
                                Button(
                                    onClick = { bookingViewModel.updateStatus(booking.id, nextSt) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "Vendor Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProviderActionCard2(
                    icon = Icons.Filled.AddBusiness,
                    title = "Register / Update Services & Pricing",
                    subtitle = "Configure your specialties, service radius, and experience",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToRegistration
                )
                ProviderActionCard2(
                    icon = Icons.Filled.BookOnline,
                    title = "All Bookings & Job History",
                    subtitle = "Manage pending, active, and completed customer jobs",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToBookings
                )
                ProviderActionCard2(
                    icon = Icons.Filled.Message,
                    title = "Customer Messages & Quotes",
                    subtitle = "Send live quotes and arrival updates to customers",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        val firstId = bookingState.bookings.firstOrNull()?.id
                        if (firstId != null) onNavigateToBookingChat(firstId) else onNavigateToMessages()
                    }
                )
                ProviderActionCard2(
                    icon = Icons.Filled.ManageAccounts,
                    title = "Vendor Account Settings",
                    subtitle = "Switch between Customer & Vendor roles or update profile",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
private fun DashStatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProviderActionCard2(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
