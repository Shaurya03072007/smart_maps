package com.smartmaps.app.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.smartmaps.app.ui.theme.*
import com.smartmaps.app.ui.viewmodel.HomeViewModel

/**
 * Wraps the app content with a location permission gate.
 * When permission is granted, it delegates to the HomeViewModel (Hilt singleton)
 * to start real location updates â€” ensuring the same LocationRepository instance
 * used by all ViewModels receives the coordinates.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionWrapper(
    content: @Composable () -> Unit
) {
    // Use the Hilt-injected HomeViewModel so location updates flow to the same
    // singleton LocationRepository that all other ViewModels depend on.
    val homeViewModel: HomeViewModel = hiltViewModel()

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            homeViewModel.onLocationPermissionGranted()
        }
    }

    if (locationPermissionState.allPermissionsGranted) {
        content()
    } else {
        LocationPermissionRequest(
            isPermanentlyDenied = locationPermissionState.permissions.any { perm ->
                val status = perm.status
                status is PermissionStatus.Denied && !status.shouldShowRationale
            },
            onRequestPermission = {
                locationPermissionState.launchMultiplePermissionRequest()
            }
        )
    }
}

@Composable
private fun LocationPermissionRequest(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    null,
                    tint = Primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Enable Location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Smart Maps needs your location to find nearby services and places. Your location is used only to show relevant results.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text("Allow Location Access")
            }

            if (isPermanentlyDenied) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { /* TODO: open settings */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Settings")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "You can also browse manually without GPS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

