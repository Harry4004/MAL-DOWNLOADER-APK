package com.harry.maldownloader.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.work.*
import com.harry.maldownloader.downloader.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class DownloadRepository(val context: Context, private val database: DownloadDatabase) {

    private val downloadDao = database.downloadDao()
    private val logDao = database.logDao()
    private val duplicateDao = database.duplicateDao()
    private val workManager = WorkManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getDatabase(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "download_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun queueDownload(
        url: String,
        fileName: String,
        malId: String? = null,
        title: String? = null,
        imageType: String? = null,
        priority: Int = 0,
        networkType: String = "any"
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val downloadItem = DownloadItem(
            id = downloadId,
            url = url,
            fileName = fileName,
            malId = malId,
            title = title,
            imageType = imageType,
            etag = null,
            lastModified = null,
            partialPath = null,
            priority = priority,
            networkType = networkType,
            errorMessage = null
        )

        downloadDao.insertDownload(downloadItem)
        logInfo(downloadId, "Download queued: $fileName")

        scheduleDownloadWork(downloadId, networkType, priority)

        return downloadId
    }

    // NEW METHOD: Queue download with comprehensive metadata
    suspend fun queueDownloadWithMetadata(
        url: String,
        fileName: String,
        entry: AnimeEntry
    ): String {
        val downloadId = UUID.randomUUID().toString()
        
        // Store metadata in download item
        val downloadItem = DownloadItem(
            id = downloadId,
            url = url,
            fileName = fileName,
            malId = entry.malId.toString(),
            title = entry.title,
            imageType = entry.type,
            etag = null,
            lastModified = null,
            partialPath = null,
            priority = if (entry.isHentai) 1 else 0,
            networkType = "any",
            errorMessage = null,
            // Store metadata as JSON string for worker
            metadata = serializeEntryMetadata(entry)
        )

        downloadDao.insertDownload(downloadItem)
        logInfo(downloadId, "🎯 Download queued with ${entry.allTags.size} tags: $fileName")

        scheduleDownloadWork(downloadId, "any", downloadItem.priority)

        return downloadId
    }

    private fun serializeEntryMetadata(entry: AnimeEntry): String {
        // Simple JSON serialization of metadata
        return """
        {
            "malId": ${entry.malId},
            "title": "${entry.title.replace("\"", "\\\"")}",
            "englishTitle": "${entry.englishTitle?.replace("\"", "\\\"") ?: ""}",
            "japaneseTitle": "${entry.japaneseTitle?.replace("\"", "\\\"") ?: ""}",
            "type": "${entry.type}",
            "synopsis": "${entry.synopsis?.take(500)?.replace("\"", "\\\"") ?: ""}",
            "score": ${entry.score ?: 0.0},
            "status": "${entry.status ?: ""}",
            "episodes": ${entry.episodes ?: 0},
            "chapters": ${entry.chapters ?: 0},
            "volumes": ${entry.volumes ?: 0},
            "isHentai": ${entry.isHentai},
            "allTags": [${entry.allTags.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]
        }
        """.trimIndent()
    }

    // XMP METADATA CREATION AND EMBEDDING
    suspend fun createXmpMetadata(entry: AnimeEntry): ByteArray = withContext(Dispatchers.IO) {
        try {
            val docBuilder = DocumentBuilderFactory.newInstance().apply { 
                isNamespaceAware = true 
            }.newDocumentBuilder()
            val doc = docBuilder.newDocument()
            
            // Namespaces
            val rdfNs = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            val dcNs = "http://purl.org/dc/elements/1.1/"
            val xmpNs = "http://ns.adobe.com/xap/1.0/"
            val malNs = "http://myanimelist.net/"
            val iptcNs = "http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"
            
            // Root RDF element
            val rdf = doc.createElementNS(rdfNs, "rdf:RDF")
            rdf.setAttribute("xmlns:rdf", rdfNs)
            rdf.setAttribute("xmlns:dc", dcNs)
            rdf.setAttribute("xmlns:xmp", xmpNs)
            rdf.setAttribute("xmlns:mal", malNs)
            rdf.setAttribute("xmlns:Iptc4xmpCore", iptcNs)
            doc.appendChild(rdf)
            
            // Description element
            val desc = doc.createElementNS(rdfNs, "rdf:Description")
            desc.setAttribute("rdf:about", "")
            rdf.appendChild(desc)
            
            // Basic Dublin Core metadata
            desc.setAttribute("dc:title", entry.title)
            desc.setAttribute("dc:description", entry.synopsis?.take(2000) ?: "")
            desc.setAttribute("dc:creator", "MAL Downloader v3.0")
            desc.setAttribute("dc:source", "https://myanimelist.net/${entry.type}/${entry.malId}")
            desc.setAttribute("dc:identifier", "MAL-${entry.malId}")
            
            // XMP metadata
            desc.setAttribute("xmp:CreatorTool", "MAL Downloader v3.0")
            desc.setAttribute("xmp:Rating", ((entry.score ?: 0f) / 2).toInt().toString())
            
            // MAL specific metadata
            desc.setAttribute("mal:id", entry.malId.toString())
            desc.setAttribute("mal:type", entry.type)
            desc.setAttribute("mal:status", entry.status ?: "")
            desc.setAttribute("mal:score", entry.score?.toString() ?: "0")
            entry.episodes?.let { desc.setAttribute("mal:episodes", it.toString()) }
            entry.chapters?.let { desc.setAttribute("mal:chapters", it.toString()) }
            entry.volumes?.let { desc.setAttribute("mal:volumes", it.toString()) }
            
            // Titles in multiple languages
            entry.englishTitle?.let { desc.setAttribute("mal:titleEnglish", it) }
            entry.japaneseTitle?.let { desc.setAttribute("mal:titleJapanese", it) }
            
            // Adult content flag
            if (entry.isHentai) {
                desc.setAttribute("dc:rights", "Adult Content - 18+")
                desc.setAttribute("mal:adult", "true")
            }
            
            // IPTC Keywords (for AVES gallery compatibility)
            if (entry.allTags.isNotEmpty()) {
                val keywordsElement = doc.createElementNS(iptcNs, "Iptc4xmpCore:Keywords")
                val keywordsBag = doc.createElementNS(rdfNs, "rdf:Bag")
                
                entry.allTags.forEach { tag ->
                    val li = doc.createElementNS(rdfNs, "rdf:li")
                    li.textContent = tag
                    keywordsBag.appendChild(li)
                }
                
                keywordsElement.appendChild(keywordsBag)
                desc.appendChild(keywordsElement)
                
                // Also add as Windows XP Keywords (semicolon-separated)
                desc.setAttribute("dc:subject", entry.allTags.joinToString(";"))
            }
            
            // Subject/Tags as Dublin Core subject
            if (entry.allTags.isNotEmpty()) {
                val dcSubject = doc.createElementNS(dcNs, "dc:subject")
                val rdfBag = doc.createElementNS(rdfNs, "rdf:Bag")
                
                entry.allTags.forEach { tag ->
                    val li = doc.createElementNS(rdfNs, "rdf:li")
                    li.textContent = tag
                    rdfBag.appendChild(li)
                }
                
                dcSubject.appendChild(rdfBag)
                desc.appendChild(dcSubject)
            }
            
            // Transform to bytes
            val transformer = TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty("omit-xml-declaration", "yes")
                setOutputProperty("indent", "yes")
            }
            
            val out = ByteArrayOutputStream()
            transformer.transform(DOMSource(doc), StreamResult(out))
            out.toByteArray()
            
        } catch (e: Exception) {
            logError("xmp", "Failed to create XMP metadata: ${e.message}")
            ByteArray(0)
        }
    }

    suspend fun embedXmpInJpeg(jpegBytes: ByteArray, xmpBytes: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        if (xmpBytes.isEmpty()) return@withContext jpegBytes
        
        try {
            val out = ByteArrayOutputStream()
            
            // Write JPEG header (SOI)
            out.write(jpegBytes, 0, 2)
            
            // Create XMP segment
            val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)
            val segmentSize = xmpHeader.size + xmpBytes.size + 2
            
            if (segmentSize <= 65535) {
                // Write APP1 marker for XMP
                out.write(0xFF)
                out.write(0xE1)
                
                // Write segment length
                out.write((segmentSize shr 8) and 0xFF)
                out.write(segmentSize and 0xFF)
                
                // Write XMP header and data
                out.write(xmpHeader)
                out.write(xmpBytes)
            }
            
            // Write rest of JPEG, skipping existing XMP segments
            var i = 2
            while (i < jpegBytes.size) {
                if (jpegBytes[i] == 0xFF.toByte() && i + 1 < jpegBytes.size) {
                    val marker = jpegBytes[i + 1].toInt() and 0xFF
                    
                    // Skip existing APP1 (XMP) segments
                    if (marker == 0xE1) {
                        i += 2
                        if (i + 1 < jpegBytes.size) {
                            val segLen = ((jpegBytes[i].toInt() and 0xFF) shl 8) or 
                                        (jpegBytes[i + 1].toInt() and 0xFF)
                            i += segLen
                            continue
                        }
                    }
                }
                
                out.write(jpegBytes[i].toInt() and 0xFF)
                i++
            }
            
            out.toByteArray()
            
        } catch (e: Exception) {
            logError("xmp", "Failed to embed XMP: ${e.message}")
            jpegBytes
        }
    }

    suspend fun saveImageWithMetadata(imageBytes: ByteArray, entry: AnimeEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            // Create XMP metadata
            val xmpBytes = createXmpMetadata(entry)
            
            // Embed XMP into image
            val finalImageBytes = embedXmpInJpeg(imageBytes, xmpBytes)
            
            // Determine folder structure
            val folder = determineFolderStructure(entry)
            val fileName = "${sanitizeForFilename(entry.title)}_${entry.malId}.jpg"
            
            // Save to MediaStore (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folder")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                    values
                )
                
                uri?.let { imageUri ->
                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        outputStream.write(finalImageBytes)
                        outputStream.flush()
                    }
                    
                    // Mark as not pending
                    context.contentResolver.update(
                        imageUri, 
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, 
                        null, 
                        null
                    )
                    
                    logInfo("save", "✅ Saved with ${entry.allTags.size} tags: $fileName")
                    return@withContext true
                }
            }
            
            false
        } catch (e: Exception) {
            logError("save", "Failed to save image with metadata: ${e.message}")
            false
        }
    }

    private fun determineFolderStructure(entry: AnimeEntry): String {
        val base = "MAL_Export"
        val typeFolder = when {
            entry.isHentai && entry.type == "manga" -> "Hentai/Manga"
            entry.isHentai && entry.type == "anime" -> "Hentai/Anime"
            entry.isHentai -> "Hentai"
            entry.type == "anime" -> "Anime"
            entry.type == "manga" -> "Manga"
            else -> "Misc"
        }
        
        // Use first genre as subfolder
        val primaryGenre = entry.allTags.firstOrNull { tag ->
            !tag.startsWith("A-") && !tag.startsWith("M-") && !tag.startsWith("H-") &&
            !listOf("Anime", "Manga", "MyAnimeList", "MAL").contains(tag)
        } ?: "Unknown"
        
        return "$base/$typeFolder/${sanitizeForFilename(primaryGenre)}"
    }

    private fun sanitizeForFilename(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return normalized.replace("[^\\w\\s-]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "_")
            .take(50)
    }

    private fun scheduleDownloadWork(downloadId: String, networkType: String, priority: Int) {
        val constraints = Constraints.Builder().apply {
            when (networkType) {
                "wifi" -> setRequiredNetworkType(NetworkType.UNMETERED)
                "cellular" -> setRequiredNetworkType(NetworkType.METERED)
                else -> setRequiredNetworkType(NetworkType.CONNECTED)
            }
            setRequiresBatteryNotLow(false)
        }.build()

        val inputData = workDataOf("downloadId" to downloadId)

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("download_$downloadId")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15000,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_$downloadId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun pauseDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "downloading") {
            downloadDao.updateDownload(download.copy(status = "paused"))
            workManager.cancelUniqueWork("download_$downloadId")
            logInfo(downloadId, "Download paused")
        }
    }

    suspend fun resumeDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "paused") {
            downloadDao.updateDownload(download.copy(status = "pending"))
            scheduleDownloadWork(downloadId, download.networkType ?: "any", download.priority)
            logInfo(downloadId, "Download resumed")
        }
    }

    suspend fun retryDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "failed") {
            downloadDao.updateDownload(
                download.copy(
                    status = "pending",
                    retryCount = download.retryCount + 1,
                    errorMessage = null
                )
            )
            scheduleDownloadWork(downloadId, download.networkType ?: "any", download.priority)
            logInfo(downloadId, "Download retry scheduled (attempt ${download.retryCount + 1})")
        }
    }

    suspend fun cancelDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "downloading") {
            downloadDao.updateDownload(download.copy(status = "cancelled"))
            workManager.cancelUniqueWork("download_$downloadId")
            logInfo(downloadId, "Download cancelled")
        }
    }

    suspend fun checkDuplicate(filePath: String): DuplicateHash? {
        val file = File(filePath)
        if (!file.exists()) return null

        val hash = calculateFileHash(file)
        return duplicateDao.findDuplicate(hash)
    }

    suspend fun markAsCompleted(downloadId: String, filePath: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null) {
            val file = File(filePath)
            if (file.exists()) {
                val hash = calculateFileHash(file)
                duplicateDao.insertHash(
                    DuplicateHash(
                        hash = hash,
                        filePath = filePath,
                        downloadId = downloadId
                    )
                )
            }

            downloadDao.updateDownload(
                download.copy(
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
            )
            logInfo(downloadId, "Download completed: ${download.fileName}")
        }
    }

    suspend fun markAsFailed(downloadId: String, error: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null) {
            downloadDao.updateDownload(
                download.copy(
                    status = "failed",
                    errorMessage = error
                )
            )
            logError(downloadId, "Download failed: $error")
        }
    }

    private fun calculateFileHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun getAllDownloads() = downloadDao.getAllDownloads()
    suspend fun getDownloadsByStatus(status: String) = downloadDao.getDownloadsByStatus(status)
    suspend fun getDownloadById(id: String) = downloadDao.getDownloadById(id)

    suspend fun logInfo(downloadId: String, message: String) {
        logDao.insertLog(DownloadLog(downloadId = downloadId, level = "INFO", message = message))
    }

    suspend fun logWarning(downloadId: String, message: String) {
        logDao.insertLog(DownloadLog(downloadId = downloadId, level = "WARN", message = message))
    }

    suspend fun logError(downloadId: String, message: String, exception: String? = null) {
        logDao.insertLog(DownloadLog(downloadId = downloadId, level = "ERROR", message = message, exception = exception))
    }

    suspend fun getRecentLogs(limit: Int = 1000) = logDao.getRecentLogs(limit)

    suspend fun cleanup() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        logDao.cleanupOldLogs(oneWeekAgo)
    }
}
