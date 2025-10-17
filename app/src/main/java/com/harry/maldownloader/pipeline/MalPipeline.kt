package com.harry.maldownloader.pipeline

import android.content.Context
import android.net.Uri
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.downloader.ImageDownloader
import com.harry.maldownloader.parser.MALXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream

class MalPipeline(
    private val context: Context,
    private val onLog: (String) -> Unit
) {
    private val parser = MALXmlParser()
    private val imageDownloader = ImageDownloader(context)

    suspend fun processMalFile(uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        onLog("Opening XML input stream...")
        val inputStream: InputStream = try {
            context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open URI")
        } catch (e: Exception) {
            onLog("Error opening file: ${e.message}")
            return@withContext emptyList<AnimeEntry>()
        }

        onLog("Parsing anime entries...")
        val entries = parser.parseAnimeEntries(inputStream).toMutableList()  // Make mutable for updates
        inputStream.close()  // Explicit close after parsing
        onLog("Parsed ${entries.size} valid anime entries (filtered invalid ones).")

        if (entries.isNotEmpty()) {
            val sample = entries.first()
            onLog("Sample entry: ID=${sample.seriesId}, Title='${sample.title}', Watched=${sample.episodesWatched}/${sample.episodesTotal}, Status=${sample.status}")
            
            // Image downloading with retries and fallbacks
            onLog("Starting image downloads for ${entries.size} entries...")
            var downloadedCount = 0
            var fallbackCount = 0
            entries.forEachIndexed { index, entry ->
                onLog("Downloading image ${index + 1}/${entries.size} for '${entry.title}' (ID: ${entry.seriesId})...")
                try {
                    val updatedEntry = imageDownloader.downloadImageForEntry(entry)
                    entries[index] = updatedEntry
                    if (updatedEntry.imagePath != null) {
                        if (updatedEntry.imagePath!!.contains("placeholder")) {  // Detect fallback
                            fallbackCount++
                        } else {
                            downloadedCount++
                        }
                    }
                } catch (e: Exception) {
                    onLog("Failed to download for ${entry.title}: ${e.message} - Using fallback.")
                    fallbackCount++
                }
                delay(1000L)  // Polite delay for Jikan API rate limiting (1 req/sec)
            }
            onLog("Image processing complete: ${downloadedCount} downloaded, ${fallbackCount} fallbacks generated.")
        } else {
            onLog("No valid entries found. Check XML format or silent failures.")
        }

        entries  // Return enriched list for UI/RecyclerView
    }
}
