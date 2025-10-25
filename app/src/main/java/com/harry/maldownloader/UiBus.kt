package com.harry.maldownloader

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest

object UiBus {
    val errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
}

@Composable
fun rememberGlobalSnackbar(onHost: (SnackbarHostState) -> Unit = {}): SnackbarHostState {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        onHost(host)
        UiBus.errors.collectLatest { msg -> host.showSnackbar(msg) }
    }
    return host
}
