package com.harry.maldownloader

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Xml
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.w3c.dom.Document
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class MediaEntry(
    val malId: Int,
    val title: String,
    val type: String,
    val genres: List<String> = emptyList(),
    val customTags: List<String> = emptyList(),
    val isHentai: Boolean = false,
    val synopsis: String = "",
    val score: Float = 0f,
    val status: String = "",
    val episodes: Int = 0,
    val year: Int = 0
)

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private lateinit var tvLogs: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnLoadXml: Button
    private lateinit var scrollLogs: ScrollView

    private lateinit var sharedPrefs: SharedPreferences

    private val savedFileIndex = ConcurrentHashMap<Int, MutableSet<String>>()
    private val saveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Custom tags storage
    private val animeCustomTags = mutableListOf<String>()
    private val mangaCustomTags = mutableListOf<String>()
    private val hentaiCustomTags = mutableListOf<String>()

    // Statistics
    private var totalProcessed = 0
    private var totalSuccess = 0
    private var totalFailed = 0

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { parseXml(it) }
    }

    // Permission launcher for Android 13+
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            logMessage("✅ Media permission granted")
        } else {
            logMessage("❌ Media permission denied - some features may not work")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSharedPreferences()
        checkPermissions()
        loadSavedData()

        btnLoadXml.setOnClickListener {
            openXmlFilePicker()
        }

        updateStatus("Ready to import MAL XML file")
        logMessage("🚀 MAL Downloader initialized")
        logMessage("📱 Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    private fun initViews() {
        tvLogs = findViewById(R.id.tvLogs)
        tvStatus = findViewById(R.id.tvStatus)
        btnLoadXml = findViewById(R.id.btnLoadXml)
        scrollLogs = findViewById(R.id.scrollLogs)
    }

    private fun setupSharedPreferences() {
        sharedPrefs = getSharedPreferences("MALDownloaderPrefs", Context.MODE_PRIVATE)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    private fun sanitizeForFilename(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        val sanitized = normalized.replace("[^\\w\\s-]".toRegex(), "")
        return sanitized.trim().replace("\\s+".toRegex(), "_").take(50)
    }

    private fun loadSavedData() {
        loadCustomTagsFromPrefs()
        loadSavedFileIndex()
    }

    private fun loadCustomTagsFromPrefs() {
        animeCustomTags.clear()
        animeCustomTags.addAll(sharedPrefs.getStringSet("animeCustomTags", setOf(
            "A-Action", "A-Romance", "A-Comedy", "A-Drama"
        )) ?: emptySet())
        
        mangaCustomTags.clear()
        mangaCustomTags.addAll(sharedPrefs.getStringSet("mangaCustomTags", setOf(
            "M-Romance", "M-Action", "M-Comedy", "M-Colored"
        )) ?: emptySet())
        
        hentaiCustomTags.clear()
        hentaiCustomTags.addAll(sharedPrefs.getStringSet("hentaiCustomTags", setOf(
            "H-NTR", "H-Vanilla", "H-Hardcore"
        )) ?: emptySet())
    }

    private fun saveCustomTagsToPrefs() {
        sharedPrefs.edit()
            .putStringSet("animeCustomTags", animeCustomTags.toSet())
            .putStringSet("mangaCustomTags", mangaCustomTags.toSet())
            .putStringSet("hentaiCustomTags", hentaiCustomTags.toSet())
            .apply()
    }

    private fun loadSavedFileIndex() {
        savedFileIndex.clear()
        // Load from shared preferences if needed for persistence
    }

    private fun addToFileIndex(malId: Int, fileName: String) {
        val set = savedFileIndex.getOrPut(malId) { mutableSetOf() }
        set.add(fileName)
    }

    private fun isDuplicate(malId: Int, fileName: String): Boolean {
        return savedFileIndex[malId]?.contains(fileName) == true
    }

    private fun openXmlFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
        } catch (e: Exception) {
            showToast("Failed to open file picker: ${e.message}")
        }
    }

    private fun parseXml(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                updateStatus("Parsing XML file...")
                logMessage("📄 Starting XML parsing...")
                
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) { 
                        showToast("Unable to open selected file.")
                        updateStatus("Failed to open file")
                    }
                    return@launch
                }
                
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)
                var eventType = parser.eventType
                val entries = mutableListOf<MediaEntry>()
                
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name?.lowercase()) {
                            "anime" -> {
                                val entry = parseMediaEntry(parser, "anime")
                                entry?.let { entries.add(it) }
                            }
                            "manga" -> {
                                val entry = parseMediaEntry(parser, "manga")
                                entry?.let { entries.add(it) }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                
                inputStream.close()
                
                withContext(Dispatchers.Main) {
                    updateStatus("Found ${entries.size} entries")
                    logMessage("✅ XML parsing complete: Found ${entries.size} entries")
                    logMessage("🔄 Starting API enrichment and downloads...")
                }
                
                // Reset statistics
                totalProcessed = 0
                totalSuccess = 0
                totalFailed = 0
                
                for ((index, entry) in entries.withIndex()) {
                    try {
                        withContext(Dispatchers.Main) {
                            updateStatus("Processing ${index + 1}/${entries.size}: ${entry.title}")
                        }
                        fetchAndProcessMediaEntry(entry)
                        delay(1500) // Respect Jikan rate limits
                    } catch (e: Exception) {
                        totalFailed++
                        Log.e(TAG, "Failed processing ${entry.title}", e)
                        withContext(Dispatchers.Main) { 
                            logMessage("❌ Failed: ${entry.title} - ${e.message}")
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    updateStatus("Complete: $totalSuccess success, $totalFailed failed")
                    logMessage("🎉 Processing complete!")
                    logMessage("📊 Stats: $totalSuccess success, $totalFailed failed out of $totalProcessed")
                }
                
            } catch (e: XmlPullParserException) {
                withContext(Dispatchers.Main) { 
                    logMessage("❌ XML parsing error: ${e.message}")
                    updateStatus("XML parsing failed")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    logMessage("❌ Error reading XML: ${e.message}")
                    updateStatus("Error processing file")
                }
            }
        }
    }

    private fun parseMediaEntry(parser: XmlPullParser, type: String): MediaEntry? {
        var malId: String? = null
        var title: String? = null
        val genres = mutableListOf<String>()
        
        try {
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.name?.equals(type, ignoreCase = true) == true) break
                
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "series_animedb_id", "manga_mangadb_id" -> malId = parser.nextText()?.trim()
                        "series_title", "manga_title" -> title = parser.nextText()?.trim()
                        "my_tags" -> {
                            val tags = parser.nextText()?.split(",")?.map { it.trim() } ?: emptyList()
                            genres.addAll(tags)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $type entry", e)
        }
        
        val customTags = when (type.lowercase()) {
            "anime" -> animeCustomTags
            "manga" -> mangaCustomTags
            else -> emptyList()
        }
        
        return if (!malId.isNullOrEmpty() && malId.toIntOrNull() != null && !title.isNullOrEmpty()) {
            MediaEntry(malId.toInt(), title!!, type.lowercase(), genres, customTags)
        } else null
    }

    // Retry helper function
    private suspend fun <T> withRetry(
        times: Int = 3,
        initialDelayMs: Long = 500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        
        return try {
            block()
        } catch (e: Exception) {
            throw lastException ?: e
        }
    }

    private suspend fun fetchAndProcessMediaEntry(entry: MediaEntry) {
        totalProcessed++
        
        withContext(Dispatchers.Main) {
            logMessage("🔍 Fetching: ${entry.type.capitalize()} '${entry.title}' (ID: ${entry.malId})")
        }
        
        try {
            val apiUrl = "https://api.jikan.moe/v4/${entry.type}/${entry.malId}"
            
            val enrichedEntry = withRetry(times = 3) {
                val request = Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "MAL-Downloader/2.0")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            val jsonString = response.body?.string() ?: ""
                            if (jsonString.isEmpty()) {
                                throw Exception("Empty API response")
                            }
                            processApiResponse(jsonString, entry)
                        }
                        response.code == 429 -> {
                            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 5
                            delay(retryAfter * 1000)
                            throw Exception("Rate limited, retrying...")
                        }
                        response.code == 404 -> {
                            throw Exception("Not found on MAL")
                        }
                        else -> {
                            throw Exception("API request failed: ${response.code}")
                        }
                    }
                }
            }
            
            // Download image with retry
            val imageUrl = getImageUrlFromEntry(enrichedEntry)
            if (imageUrl.isNotEmpty()) {
                downloadCoverAndEmbedXmp(imageUrl, enrichedEntry)
            } else {
                withContext(Dispatchers.Main) {
                    logMessage("⚠️ No image found for ${entry.title}")
                }
                totalFailed++
            }
            
        } catch (e: Exception) {
            totalFailed++
            withContext(Dispatchers.Main) {
                logMessage("❌ Failed fetching ${entry.title}: ${e.message}")
            }
        }
    }

    private fun processApiResponse(jsonString: String, entry: MediaEntry): MediaEntry {
        val json = JSONObject(jsonString)
        val data = json.getJSONObject("data")
        
        val apiGenres = mutableListOf<String>()
        val genresArray = data.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                apiGenres.add(genresArray.getJSONObject(i).getString("name"))
            }
        }
        
        // Check for explicit/hentai genres
        val explicitArray = data.optJSONArray("explicit_genres")
        if (explicitArray != null) {
            for (i in 0 until explicitArray.length()) {
                apiGenres.add(explicitArray.getJSONObject(i).getString("name"))
            }
        }
        
        val synopsis = data.optString("synopsis", "")
        val score = data.optDouble("score", 0.0).toFloat()
        val status = data.optString("status", "")
        val episodes = data.optInt("episodes", 0)
        val year = data.optJSONObject("aired")?.optString("from", "")?.take(4)?.toIntOrNull() ?: 0
        
        // Detect hentai content
        val isHentai = apiGenres.any { it.contains("Hentai", ignoreCase = true) || 
                                      it.contains("Erotica", ignoreCase = true) }
        
        val customTags = if (isHentai) hentaiCustomTags else entry.customTags
        
        return entry.copy(
            genres = apiGenres,
            customTags = customTags,
            isHentai = isHentai,
            synopsis = synopsis,
            score = score,
            status = status,
            episodes = episodes,
            year = year
        )
    }

    private fun getImageUrlFromEntry(entry: MediaEntry): String {
        // This would be extracted from the API response
        // For now, we'll construct it based on typical patterns
        return ""
    }

    private suspend fun downloadCoverAndEmbedXmp(imageUrl: String, entry: MediaEntry) {
        withContext(Dispatchers.Main) {
            logMessage("⬇️ Downloading: ${entry.title}")
        }
        
        try {
            val imageBytes = withRetry(times = 3) {
                val request = Request.Builder()
                    .url(imageUrl)
                    .addHeader("User-Agent", "MAL-Downloader/2.0")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Download failed: ${response.code}")
                    }
                    
                    response.body?.bytes() ?: throw Exception("Empty response body")
                }
            }
            
            // Embed XMP in memory before saving
            val xmpBytes = createXmpMetadata(entry)
            val finalImageBytes = embedXmpInJpeg(imageBytes, xmpBytes)
            
            saveImageToStorage(finalImageBytes, entry)
            totalSuccess++
            
        } catch (e: Exception) {
            totalFailed++
            withContext(Dispatchers.Main) {
                logMessage("❌ Download failed for ${entry.title}: ${e.message}")
            }
        }
    }

    private fun createXmpMetadata(entry: MediaEntry): ByteArray {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.newDocument()
            
            val rdfNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            val dcNamespace = "http://purl.org/dc/elements/1.1/"
            val malNamespace = "http://myanimelist.net/"
            val xmpNamespace = "http://ns.adobe.com/xap/1.0/"
            
            val rdf = doc.createElementNS(rdfNamespace, "rdf:RDF")
            rdf.setAttribute("xmlns:rdf", rdfNamespace)
            rdf.setAttribute("xmlns:dc", dcNamespace)
            rdf.setAttribute("xmlns:mal", malNamespace)
            rdf.setAttribute("xmlns:xmp", xmpNamespace)
            doc.appendChild(rdf)
            
            val description = doc.createElementNS(rdfNamespace, "rdf:Description")
            description.setAttribute("rdf:about", "")
            rdf.appendChild(description)
            
            // Add metadata
            description.setAttribute("dc:title", entry.title)
            description.setAttribute("dc:description", entry.synopsis.take(500))
            description.setAttribute("dc:creator", "MAL Downloader v2.0")
            description.setAttribute("mal:id", entry.malId.toString())
            description.setAttribute("mal:type", entry.type)
            description.setAttribute("mal:genres", entry.genres.joinToString(", "))
            description.setAttribute("mal:score", entry.score.toString())
            description.setAttribute("mal:status", entry.status)
            description.setAttribute("mal:episodes", entry.episodes.toString())
            description.setAttribute("mal:year", entry.year.toString())
            description.setAttribute("xmp:Rating", (entry.score / 2).toInt().toString())
            
            if (entry.isHentai) {
                description.setAttribute("dc:rights", "Adult Content - 18+")
                description.setAttribute("mal:adult", "true")
            }
            
            // Add subject tags
            val dcSubject = doc.createElementNS(dcNamespace, "dc:subject")
            val rdfBag = doc.createElementNS(rdfNamespace, "rdf:Bag")
            val allTags = (entry.genres + entry.customTags).distinct()
            
            for (tag in allTags) {
                val rdfLi = doc.createElementNS(rdfNamespace, "rdf:li")
                rdfLi.textContent = tag
                rdfBag.appendChild(rdfLi)
            }
            dcSubject.appendChild(rdfBag)
            description.appendChild(dcSubject)
            
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty("omit-xml-declaration", "yes")
            val source = DOMSource(doc)
            val outputStream = ByteArrayOutputStream()
            val result = StreamResult(outputStream)
            transformer.transform(source, result)
            
            return outputStream.toByteArray()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating XMP for ${entry.title}", e)
            return ByteArray(0)
        }
    }

    private fun embedXmpInJpeg(jpegBytes: ByteArray, xmpBytes: ByteArray): ByteArray {
        if (xmpBytes.isEmpty()) return jpegBytes
        
        val outputStream = ByteArrayOutputStream()
        try {
            // Write SOI marker
            outputStream.write(jpegBytes, 0, 2)
            
            // Create XMP APP1 segment
            val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)
            val segmentSize = xmpHeader.size + xmpBytes.size + 2
            
            if (segmentSize <= 65535) {
                outputStream.write(0xFF)
                outputStream.write(0xE1) // APP1
                outputStream.write((segmentSize shr 8) and 0xFF)
                outputStream.write(segmentSize and 0xFF)
                outputStream.write(xmpHeader)
                outputStream.write(xmpBytes)
            }
            
            // Write rest of JPEG, skipping existing APP1 segments
            var i = 2
            while (i < jpegBytes.size) {
                if (jpegBytes[i] == 0xFF.toByte() && i + 1 < jpegBytes.size) {
                    val marker = jpegBytes[i + 1].toInt() and 0xFF
                    if (marker == 0xE1) { // Skip existing APP1
                        i += 2
                        if (i + 1 < jpegBytes.size) {
                            val segLen = ((jpegBytes[i].toInt() and 0xFF) shl 8) or 
                                        (jpegBytes[i + 1].toInt() and 0xFF)
                            i += segLen
                            continue
                        }
                    }
                }
                outputStream.write(jpegBytes[i].toInt() and 0xFF)
                i++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding XMP", e)
            return jpegBytes
        }
        
        return outputStream.toByteArray()
    }

    private suspend fun saveImageToStorage(imageBytes: ByteArray, entry: MediaEntry) {
        try {
            val folderStructure = determineFolderStructure(entry)
            val fileName = "${sanitizeForFilename(entry.title)}_${entry.malId}.jpg"
            
            if (isDuplicate(entry.malId, fileName)) {
                withContext(Dispatchers.Main) {
                    logMessage("⚠️ Skipping duplicate: ${entry.title}")
                }
                return
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderStructure")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { imageUri ->
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        outputStream.write(imageBytes)
                        outputStream.flush()
                    }
                    
                    // Clear pending flag
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(imageUri, updateValues, null, null)
                    
                    addToFileIndex(entry.malId, fileName)
                    
                    withContext(Dispatchers.Main) {
                        logMessage("✅ Saved: $fileName in $folderStructure")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image for ${entry.title}", e)
            withContext(Dispatchers.Main) {
                logMessage("❌ Save failed: ${entry.title} - ${e.message}")
            }
            throw e
        }
    }

    private fun determineFolderStructure(entry: MediaEntry): String {
        val baseDir = "MAL_Export"
        
        val typeFolder = when {
            entry.isHentai && entry.type == "manga" -> "Hentai/Manga"
            entry.isHentai && entry.type == "anime" -> "Hentai/Anime"
            entry.isHentai -> "Hentai"
            entry.type == "anime" -> "Anime"
            entry.type == "manga" -> "Manga"
            else -> "Misc"
        }
        
        val primaryGenre = entry.genres.firstOrNull() ?: "Unknown"
        val sanitizedGenre = sanitizeForFilename(primaryGenre)
        
        return "$baseDir/$typeFolder/$sanitizedGenre"
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            tvStatus.text = "Status: $message"
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun logMessage(message: String) {
        runOnUiThread {
            val timestamp = System.currentTimeMillis() % 100000
            tvLogs.append("$timestamp: $message\n")
            
            // Auto-scroll to bottom
            scrollLogs.post {
                scrollLogs.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveScope.cancel()
        saveCustomTagsToPrefs()
    }

    companion object {
        private const val TAG = "MALDownloader"
    }
}