package com.harry.maldownloader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val isProcessing: Boolean = false,
    val customTagsCsv: String = "H-NTR,M-Colored,A-Harem",
    val logs: List<String> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _ui = androidx.compose.runtime.mutableStateOf(UiState())
    val uiState get() = _ui.value

    fun setCustomTags(csv: String) {
        _ui.value = uiState.copy(customTagsCsv = csv)
    }

    fun appendLog(msg: String) {
        _ui.value = uiState.copy(logs = uiState.logs + msg)
    }

    fun clearLogs() {
        _ui.value = uiState.copy(logs = emptyList())
    }

    fun onMalFilePicked(path: String, baseDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _ui.value = uiState.copy(isProcessing = true)
                appendLog("Parsing MAL file...")
                val entries = MalPipeline.parseMalDataFile(getApplication(), path)
                val customTags = uiState.customTagsCsv.split(",")
                    .map { it.trim() }.filter { it.isNotEmpty() }

                appendLog("Found ${entries.size} entries")

                for ((idx, raw) in entries.withIndex()) {
                    appendLog("Enriching [${idx + 1}/${entries.size}]: ${raw.title}")
                    val enriched = MalPipeline.enrichFromJikanIfMissing(raw)
                    val ok = MalPipeline.processEntry(baseDir, enriched, customTags)
                    appendLog("Saved: ${enriched.title} -> ${if (ok) "OK" else "Placeholder"}")
                }
                appendLog("Done")
            } catch (e: Exception) {
                appendLog("Error: ${e.message}")
            } finally {
                _ui.value = uiState.copy(isProcessing = false)
            }
        }
    }
}