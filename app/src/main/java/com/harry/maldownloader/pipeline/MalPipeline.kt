package com.harry.maldownloader.pipeline

import android.content.Context
import android.net.Uri
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.parser.MALXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class MalPipeline(
    private val context: Context,
    private val onLog: (String) -> Unit
) {
    private val parser = MALXmlParser()

    suspend fun processMalFile(uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        onLog("Opening XML input stream...")
        val inputStream: InputStream = try {
            context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open URI")
        } catch (e: Exception) {
            onLog("Error opening file: ${e.message}")
            return@withContext emptyList<AnimeEntry>()
        }

        onLog("Parsing anime entries...")
        val entries = parser.parseAnimeEntries(inputStream)
        onLog("Parsed ${entries.size} valid anime entries (filtered invalid ones).")

        if (entries.isNotEmpty()) {
            val sample = entries.first()
            onLog("Sample entry: ID=${sample.seriesId}, Title='${sample.title}', Watched=${sample.episodesWatched}/${sample.episodesTotal}, Status=${sample.status}")
            // Future: Add image download, tagging here (e.g., for each entry, downloadImage(sample.seriesId))
        } else {
            onLog("No valid entries found. Check XML format or silent failures.")
        }

        entries  // Return for further processing if needed
    }
}
