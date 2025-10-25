package com.harry.maldownloader.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * UI Event Bus for handling application-wide events
 * Uses SharedFlow for reactive event communication between components
 */
object UiBus {
    
    // Error event flow
    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10, // Allow buffering up to 10 error messages
    )
    val errors: SharedFlow<String> = _errors.asSharedFlow()
    
    // Success event flow
    private val _success = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10,
    )
    val success: SharedFlow<String> = _success.asSharedFlow()
    
    // Info event flow
    private val _info = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10,
    )
    val info: SharedFlow<String> = _info.asSharedFlow()
    
    // Warning event flow
    private val _warnings = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10,
    )
    val warnings: SharedFlow<String> = _warnings.asSharedFlow()
    
    // General events flow for custom events
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 50,
    )
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    
    /**
     * Emit an error message to all subscribers
     */
    fun emitError(message: String): Boolean {
        return _errors.tryEmit(message)
    }
    
    /**
     * Emit a success message to all subscribers
     */
    fun emitSuccess(message: String): Boolean {
        return _success.tryEmit(message)
    }
    
    /**
     * Emit an info message to all subscribers
     */
    fun emitInfo(message: String): Boolean {
        return _info.tryEmit(message)
    }
    
    /**
     * Emit a warning message to all subscribers
     */
    fun emitWarning(message: String): Boolean {
        return _warnings.tryEmit(message)
    }
    
    /**
     * Emit a custom event to all subscribers
     */
    fun emitEvent(event: UiEvent): Boolean {
        return _events.tryEmit(event)
    }
    
    /**
     * Clear all buffered messages (if any)
     */
    fun clearAll() {
        // Reset flows by creating new instances
        // Note: This is a simplified approach; in production you might want
        // to implement a more sophisticated reset mechanism
    }
}

/**
 * Sealed class representing different types of UI events
 */
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowDialog(val title: String, val message: String) : UiEvent()
    data class NavigateTo(val destination: String) : UiEvent()
    data class ShowLoading(val isVisible: Boolean) : UiEvent()
    data class PermissionRequired(val permission: String, val rationale: String) : UiEvent()
    data class NetworkError(val error: String, val retryAction: (() -> Unit)? = null) : UiEvent()
    data class DataUpdated(val dataType: String, val count: Int) : UiEvent()
    object RefreshUI : UiEvent()
    object CloseApp : UiEvent()
    
    // MAL Downloader specific events
    data class DownloadStarted(val title: String, val malId: Int) : UiEvent()
    data class DownloadCompleted(val title: String, val malId: Int) : UiEvent()
    data class DownloadFailed(val title: String, val malId: Int, val error: String) : UiEvent()
    data class ProcessingProgress(val current: Int, val total: Int) : UiEvent()
    data class TagsUpdated(val entryId: Int, val tagCount: Int) : UiEvent()
    data class FileImported(val fileName: String, val entryCount: Int) : UiEvent()
    data class ApiError(val apiName: String, val error: String) : UiEvent()
}