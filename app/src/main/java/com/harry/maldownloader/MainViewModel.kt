package com.harry.maldownloader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    
    private val _ui = MutableLiveData(UiState())
    val uiState: LiveData<UiState> get() = _ui

    private fun updateUi(update: UiState.() -> UiState) {
        val current = _ui.value ?: UiState()
        _ui.postValue(current.update())
    }

    fun setCustomTags(csv: String) {
        updateUi {
            copy(customTagsCsv = csv)
        }
    }

    fun appendLog(msg: String) {
        updateUi {
            copy(logs = logs + msg)
        }
    }

    fun clearLogs() {
        updateUi {
            copy(logs = emptyList())
        }
    }

    fun onMalFilePicked(path: String, baseDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateUi { copy(isProcessing = true) }
                appendLog("Opening MAL file: $path")

                val file = File(path)
                appendLog("File size: ${file.length()} bytes")

                if (!file.exists() || file.length() == 0L) {
                    appendLog("Error: The selected MAL file is empty or could not be accessed.")
                    return@launch
                }

                appendLog("Reading MAL XML contents...")
                val preview = file.bufferedReader().use { it.readLines().take(20).joinToString("\n") }
                appendLog("File preview (first 20 lines):\n$preview")

                appendLog("Parsing MAL XML file...")
                val entries = MalPipeline.parseMalDataFile(getApplication(), path) { appendLog(it) }
                appendLog("Parsed entries count: ${entries.size}")

                if (entries.isEmpty()) {
                    appendLog("Warning: No entries were detected. The file may use an unsupported format or malformed tags.")
                    return@launch
                }

                val customTags = _ui.value?.customTagsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                    ?: emptyList()

                for ((idx, raw) in entries.withIndex()) {
                    appendLog("Enriching entry [${idx + 1}/${entries.size}]: ${raw.title}")
                    val enriched = MalPipeline.enrichFromJikanIfMissing(raw) { appendLog(it) }
                    val ok = MalPipeline.processEntry(baseDir, enriched, customTags) { appendLog(it) }
                    appendLog("Saved ${enriched.title}: ${if (ok) "OK" else "Fallback poster"}")
                }

                appendLog("MAL XML successfully processed 2${entries.size}2 series generated.")
            } catch (e: Exception) {
                appendLog("Critical Error: ${e.localizedMessage}")
            } finally {
                updateUi { copy(isProcessing = false) }
            }
        }
    }
}
