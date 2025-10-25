package com.harry.maldownloader

import androidx.compose.runtime.Composable
import com.harry.maldownloader.ui.components.ModernGlassmorphismDrawer

// Backwards shim to keep existing SafeMainScreen import working if needed
@Composable
fun ModernDrawer(
    onClose: () -> Unit,
    onCustomTagsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    storagePermissionGranted: Boolean,
    notificationPermissionGranted: Boolean
) = ModernGlassmorphismDrawer(
    onClose = onClose,
    onCustomTagsClick = onCustomTagsClick,
    onSettingsClick = onSettingsClick,
    onAboutClick = onAboutClick,
    storagePermissionGranted = storagePermissionGranted,
    notificationPermissionGranted = notificationPermissionGranted
)
