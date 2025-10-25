package com.harry.maldownloader.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun showErrorSnack(scope: CoroutineScope, host: SnackbarHostState, message: String) {
    scope.launch {
        host.showSnackbar(message = message, withDismissAction = true, duration = SnackbarDuration.Short)
    }
}
