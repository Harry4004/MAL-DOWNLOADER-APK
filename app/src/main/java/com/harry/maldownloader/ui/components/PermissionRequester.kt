package com.harry.maldownloader.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun PermissionRequester(
    notificationPermissionGranted: Boolean,
    storagePermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit
) {
    if (!notificationPermissionGranted) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal */ },
            title = { Text("Notification Permission Required") },
            text = { Text("Please allow notifications to get download progress updates.") },
            confirmButton = {
                Button(onClick = onRequestNotificationPermission) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Optional: handle denial */ }) {
                    Text("Deny")
                }
            }
        )
    } else if (!storagePermissionGranted) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal */ },
            title = { Text("Storage Permission Required") },
            text = { Text("Please allow storage permission to save downloaded images.") },
            confirmButton = {
                Button(onClick = onRequestStoragePermission) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Optional: handle denial */ }) {
                    Text("Deny")
                }
            }
        )
    }
}
