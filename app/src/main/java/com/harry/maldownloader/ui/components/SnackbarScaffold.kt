package com.harry.maldownloader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun SnackbarScaffold(content: @Composable () -> Unit) {
    val host = remember { SnackbarHostState() }
    rememberGlobalSnackbar { /* register host if needed */ }
    Scaffold(snackbarHost = { SnackbarHost(hostState = host) }) {
        content()
    }
}
