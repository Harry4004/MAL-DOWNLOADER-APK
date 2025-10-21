package com.harry.maldownloader

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Xml
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayOutputStream
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
    val year: Int = 0,
    val allTags: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val demographics: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val rating: String = "",
    val source: String = ""
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
    private lateinit var btnAddTag: Button
    private lateinit var btnViewTags: Button
    private lateinit var scrollLogs: ScrollView

    private lateinit var sharedPrefs: SharedPreferences

    private val savedFileIndex = ConcurrentHashMap<Int, MutableSet<String>>()
    private val saveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val animeCustomTags = mutableListOf<String>()
    private val mangaCustomTags = mutableListOf<String>()
    private val hentaiCustomTags = mutableListOf<String>()

    private var totalProcessed = 0
    private var totalSuccess = 0
    private var totalFailed = 0

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { parseXml(it) } }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) logMessage("✅ Media permission granted")
        else logMessage("❌ Media permission denied - some features may not work")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSharedPreferences()
        checkPermissions()
        loadSavedData()
        bindButtons()

        updateStatus("Ready to import MAL XML file")
        logMessage("🚀 MAL Downloader initialized")
        logMessage("📱 Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    private fun initViews() {
        tvLogs = findViewById(R.id.tvLogs)
        tvStatus = findViewById(R.id.tvStatus)
        btnLoadXml = findViewById(R.id.btnLoadXml)
        btnAddTag = findViewById(R.id.btnAddTag)
        btnViewTags = findViewById(R.id.btnViewTags)
        scrollLogs = findViewById(R.id.scrollLogs)
        btnLoadXml.setOnClickListener { openXmlFilePicker() }
    }

    private fun bindButtons() {
        btnAddTag.setOnClickListener { showAddTagDialog() }
        btnViewTags.setOnClickListener { showViewTagsDialog() }
    }

    private fun setupSharedPreferences() {
        sharedPrefs = getSharedPreferences("MALDownloaderPrefs", Context.MODE_PRIVATE)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
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
        animeCustomTags.clear()
        animeCustomTags.addAll(sharedPrefs.getStringSet("animeCustomTags", emptySet()) ?: emptySet())
        mangaCustomTags.clear()
        mangaCustomTags.addAll(sharedPrefs.getStringSet("mangaCustomTags", emptySet()) ?: emptySet())
        hentaiCustomTags.clear()
        hentaiCustomTags.addAll(sharedPrefs.getStringSet("hentaiCustomTags", emptySet()) ?: emptySet())
        savedFileIndex.clear()
    }

    private fun saveCustomTagsToPrefs() {
        sharedPrefs.edit()
            .putStringSet("animeCustomTags", animeCustomTags.toSet())
            .putStringSet("mangaCustomTags", mangaCustomTags.toSet())
            .putStringSet("hentaiCustomTags", hentaiCustomTags.toSet())
            .apply()
    }

    private fun openXmlFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
        } catch (e: Exception) {
            showToast("Failed to open file picker: ${e.message}")
        }
    }

    private fun showAddTagDialog() {
        val types = arrayOf("Anime", "Manga", "Hentai")
        var selected = 0
        val input = EditText(this).apply { hint = "e.g., A-Action, M-Colored, H-NTR" }
        AlertDialog.Builder(this)
            .setTitle("Add Custom Tag")
            .setSingleChoiceItems(types, 0) { _, which -> selected = which }
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty()) {
                    when (selected) {
                        0 -> animeCustomTags.add(tag)
                        1 -> mangaCustomTags.add(tag)
                        2 -> hentaiCustomTags.add(tag)
                        else -> {}
                    }
                    saveCustomTagsToPrefs()
                    showToast("Added tag: $tag")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showViewTagsDialog() {
        val all = mutableListOf<String>()
        all += animeCustomTags.map { "Anime: $it" }
        all += mangaCustomTags.map { "Manga: $it" }
        all += hentaiCustomTags.map { "Hentai: $it" }
        
        if (all.isEmpty()) {
            showToast("No custom tags yet")
            return
        }
        
        val tagDialog = AlertDialog.Builder(this)
            .setTitle("Custom Tags")
            .setItems(all.toTypedArray(), null)
            .setNegativeButton("OK", null)
            .create()
        
        tagDialog.show()
        
        tagDialog.listView.setOnItemLongClickListener { _, _, position, _ ->
            val tag = all[position]
            tagDialog.dismiss()
            showRemoveTagDialog(tag)
            true
        }
    }

    private fun showRemoveTagDialog(tag: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Tag?")
            .setMessage("Are you sure you want to remove $tag?")
            .setPositiveButton("Remove") { dialogInterface, _ ->
                when {
                    tag.startsWith("Anime: ") -> animeCustomTags.remove(tag.removePrefix("Anime: "))
                    tag.startsWith("Manga: ") -> mangaCustomTags.remove(tag.removePrefix("Manga: "))
                    tag.startsWith("Hentai: ") -> hentaiCustomTags.remove(tag.removePrefix("Hentai: "))
                }
                saveCustomTagsToPrefs()
                showToast("Removed: $tag")
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ -> 
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun parseXml(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                updateStatus("Parsing XML file...")
                logMessage("📄 Starting XML parsing...")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(inputStream, null)
                    var eventType = parser.eventType
                    val entries = mutableListOf<MediaEntry>()
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (parser.name?.lowercase()) {
                                "anime" -> parseMediaEntry(parser, "anime")?.let(entries::add)
                                "manga" -> parseMediaEntry(parser, "manga")?.let(entries::add)
                                else -> {}
                            }
                        }
                        eventType = parser.next()
                    }
                    withContext(Dispatchers.Main) {
                        updateStatus("Found ${entries.size} entries")
                        logMessage("✅ XML parsing complete: Found ${entries.size} entries")
                        logMessage("🔄 Starting API enrichment and downloads...")
                    }
                    totalProcessed = 0
                    totalSuccess = 0
                    totalFailed = 0
                    for ((index, entry) in entries.withIndex()) {
                        try {
                            withContext(Dispatchers.Main) { 
                                updateStatus("Processing ${index + 1}/${entries.size}: ${entry.title}") 
                            }
                            fetchAndProcessMediaEntry(entry)
                            delay(1500)
                        } catch (e: Exception) {
                            totalFailed++
                            withContext(Dispatchers.Main) { 
                                logMessage("❌ Failed: ${entry.title} - ${e.message ?: "Unknown error"}") 
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        updateStatus("Complete: $totalSuccess success, $totalFailed failed")
                        logMessage("🎉 Processing complete!")
                        logMessage("📊 Stats: $totalSuccess success, $totalFailed failed out of $totalProcessed")
                    }
                } ?: withContext(Dispatchers.Main) { 
                    showToast("Unable to open selected file.")
                    updateStatus("Failed to open file") 
                }
            } catch (e: XmlPullParserException) {
                withContext(Dispatchers.Main) { 
                    logMessage("❌ XML parsing error: ${e.message ?: "Unknown error"}")
                    updateStatus("XML parsing failed") 
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    logMessage("❌ Error reading XML: ${e.message ?: "Unknown error"}")
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
                if (eventType == XmlPullParser.END_TAG && parser.name?.equals(type, true) == true) break
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "series_animedb_id", "manga_mangadb_id" -> malId = parser.nextText()?.trim()
                        "series_title", "manga_title" -> title = parser.nextText()?.trim()
                        "my_tags" -> genres.addAll(parser.nextText()?.split(',')?.map { it.trim() } ?: emptyList())
                        else -> {}
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
            MediaEntry(malId.toInt(), title, type.lowercase(), genres, customTags)
        } else {
            null
        }
    }

    private suspend fun <T> withRetry(times: Int = 3, initialDelayMs: Long = 500, factor: Double = 2.0, block: suspend () -> T): T {
        var current = initialDelayMs
        var last: Exception? = null
        repeat(times - 1) { 
            try { 
                return block() 
            } catch (e: Exception) { 
                last = e
                delay(current)
                current = (current * factor).toLong() 
            } 
        }
        return try { 
            block() 
        } catch (e: Exception) { 
            throw last ?: e 
        }
    }

    private suspend fun fetchAndProcessMediaEntry(entry: MediaEntry) {
        totalProcessed++
        logMessage("🔍 Fetching: ${entry.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} '${entry.title}' (ID: ${entry.malId})")
        try {
            val apiUrl = "https://api.jikan.moe/v4/${entry.type}/${entry.malId}"
            val (enrichedEntry, imageUrl) = withRetry(times = 3) {
                val request = Request.Builder().url(apiUrl).addHeader("User-Agent", "MAL-Downloader/2.0").build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            val jsonString = response.body?.string() ?: throw Exception("Empty API response")
                            processApiResponse(jsonString, entry)
                        }
                        response.code == 429 -> { 
                            delay((response.header("Retry-After")?.toLongOrNull() ?: 5) * 1000)
                            throw Exception("Rate limited, retrying...") 
                        }
                        response.code == 404 -> throw Exception("Not found on MAL")
                        else -> throw Exception("API request failed: ${response.code}")
                    }
                }
            }
            if (imageUrl.isNotEmpty()) { 
                downloadCoverAndEmbedXmp(imageUrl, enrichedEntry) 
            } else { 
                logMessage("⚠️ No image found for ${entry.title}")
                totalFailed++ 
            }
        } catch (e: Exception) { 
            totalFailed++
            logMessage("❌ Failed fetching ${entry.title}: ${e.message ?: "Unknown error"}") 
        }
    }

    private fun processApiResponse(jsonString: String, entry: MediaEntry): Pair<MediaEntry, String> {
        val json = JSONObject(jsonString)
        val data = json.getJSONObject("data")
        
        // Extract ALL possible tags from Jikan API - COMPREHENSIVE TAG EXTRACTION
        val allTags = mutableSetOf<String>()
        val studios = mutableListOf<String>()
        val demographics = mutableListOf<String>()
        val themes = mutableListOf<String>()
        
        // 1. Basic genres
        data.optJSONArray("genres")?.let { arr -> 
            for (i in 0 until arr.length()) {
                allTags.add(arr.getJSONObject(i).getString("name"))
            }
        }
        
        // 2. Explicit genres (critical for hentai content)
        data.optJSONArray("explicit_genres")?.let { arr -> 
            for (i in 0 until arr.length()) {
                allTags.add(arr.getJSONObject(i).getString("name"))
            }
        }
        
        // 3. Themes (story themes like School, Supernatural, etc.)
        data.optJSONArray("themes")?.let { arr -> 
            for (i in 0 until arr.length()) {
                val theme = arr.getJSONObject(i).getString("name")
                themes.add(theme)
                allTags.add(theme)
            }
        }
        
        // 4. Demographics (target audience)
        data.optJSONArray("demographics")?.let { arr -> 
            for (i in 0 until arr.length()) {
                val demo = arr.getJSONObject(i).getString("name")
                demographics.add(demo)
                allTags.add(demo)
            }
        }
        
        // 5. Studios (for anime)
        if (entry.type == "anime") {
            data.optJSONArray("studios")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val studio = arr.getJSONObject(i).getString("name")
                    studios.add(studio)
                    allTags.add("Studio: $studio")
                }
            }
        }
        
        // 6. Serializations/Publishers (for manga)
        if (entry.type == "manga") {
            data.optJSONArray("serializations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val serialization = arr.getJSONObject(i).getString("name")
                    allTags.add("Published: $serialization")
                }
            }
        }
        
        // 7. Status and type information
        val status = data.optString("status", "")
        if (status.isNotEmpty()) {
            allTags.add(status.replace("_", " "))
        }
        
        val type = data.optString("type", "")
        if (type.isNotEmpty()) allTags.add(type)
        
        // 8. Rating information
        val rating = data.optString("rating", "")
        if (rating.isNotEmpty()) allTags.add(rating)
        
        // 9. Source material
        val source = data.optString("source", "")
        if (source.isNotEmpty()) allTags.add("Source: $source")
        
        // 10. Season and year
        val aired = data.optJSONObject("aired")
        aired?.optJSONObject("prop")?.let { prop ->
            prop.optJSONObject("from")?.let { from ->
                val year = from.optInt("year", 0)
                val month = from.optInt("month", 0)
                if (year > 0) {
                    allTags.add(year.toString())
                    val season = when (month) {
                        in 1..3 -> "Winter"
                        in 4..6 -> "Spring"
                        in 7..9 -> "Summer"
                        in 10..12 -> "Fall"
                        else -> null
                    }
                    if (season != null) {
                        allTags.add("$season $year")
                        allTags.add("$season Season")
                    }
                }
            }
        }
        
        // 11. Add content type and platform tags
        allTags.add(entry.type.replaceFirstChar { it.uppercase() }) // "Anime" or "Manga"
        allTags.add("MyAnimeList")
        allTags.add("MAL")
        
        // 12. Score-based tags
        val score = data.optDouble("score", 0.0).toFloat()
        if (score > 0) {
            when {
                score >= 9.0 -> allTags.add("Masterpiece")
                score >= 8.0 -> allTags.add("Great")
                score >= 7.0 -> allTags.add("Good")
                score >= 6.0 -> allTags.add("Fine")
                else -> allTags.add("Poor")
            }
        }
        
        // 13. Custom prefix tags based on type
        val isHentai = allTags.any { it.contains("Hentai", true) || it.contains("Erotica", true) }
        val customTags = if (isHentai) {
            hentaiCustomTags.map { "H-$it" }
        } else {
            when (entry.type) {
                "anime" -> animeCustomTags.map { "A-$it" }
                "manga" -> mangaCustomTags.map { "M-$it" }
                else -> emptyList()
            }
        }
        allTags.addAll(customTags)
        
        // 14. Add searchable ID tags
        allTags.add("MAL-${entry.malId}")
        allTags.add("ID-${entry.malId}")
        
        // Clean and finalize tags
        val finalTags = allTags.toList().distinct().filter { it.isNotBlank() && it.length > 1 }
        
        logMessage("📊 Extracted ${finalTags.size} tags for ${entry.title}")
        
        val synopsis = data.optString("synopsis", "")
        val episodes = data.optInt("episodes", 0)
        val year = aired?.optString("from", "")?.take(4)?.toIntOrNull() ?: 0
        
        val updated = entry.copy(
            genres = finalTags.filter { !it.startsWith("A-") && !it.startsWith("M-") && !it.startsWith("H-") }.take(10),
            customTags = customTags,
            isHentai = isHentai,
            synopsis = synopsis,
            score = score,
            status = status,
            episodes = episodes,
            year = year,
            allTags = finalTags,
            studios = studios,
            demographics = demographics,
            themes = themes,
            rating = rating,
            source = source
        )
        
        val jpg = data.optJSONObject("images")?.optJSONObject("jpg")
        val imageUrl = jpg?.optString("large_image_url")?.ifEmpty { 
            jpg.optString("image_url") 
        } ?: ""
        
        return Pair(updated, imageUrl)
    }

    private suspend fun downloadCoverAndEmbedXmp(imageUrl: String, entry: MediaEntry) {
        logMessage("⬇️ Downloading: ${entry.title}")
        try {
            val imageBytes = withRetry(times = 3) {
                val request = Request.Builder().url(imageUrl).addHeader("User-Agent", "MAL-Downloader/2.0").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")
                    response.body?.bytes() ?: throw Exception("Empty response body")
                }
            }
            val xmpBytes = createXmpMetadata(entry)
            val finalImageBytes = embedXmpInJpeg(imageBytes, xmpBytes)
            saveImageToStorage(finalImageBytes, entry)
            totalSuccess++
        } catch (e: Exception) { 
            totalFailed++
            logMessage("❌ Download failed for ${entry.title}: ${e.message ?: "Unknown error"}") 
        }
    }

    private fun createXmpMetadata(entry: MediaEntry): ByteArray {
        return try {
            val docBuilder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            val doc = docBuilder.newDocument()
            val rdfNs = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            val dcNs = "http://purl.org/dc/elements/1.1/"
            val malNs = "http://myanimelist.net/"
            val xmpNs = "http://ns.adobe.com/xap/1.0/"
            
            val rdf = doc.createElementNS(rdfNs, "rdf:RDF")
            rdf.setAttribute("xmlns:rdf", rdfNs)
            rdf.setAttribute("xmlns:dc", dcNs)
            rdf.setAttribute("xmlns:mal", malNs)
            rdf.setAttribute("xmlns:xmp", xmpNs)
            doc.appendChild(rdf)
            
            val desc = doc.createElementNS(rdfNs, "rdf:Description")
            desc.setAttribute("rdf:about", "")
            rdf.appendChild(desc)
            
            // Enhanced metadata with ALL available fields
            desc.setAttribute("dc:title", entry.title)
            desc.setAttribute("dc:description", entry.synopsis.take(2000))
            desc.setAttribute("dc:creator", "MAL Downloader v2.0")
            desc.setAttribute("dc:source", "https://myanimelist.net/${entry.type}/${entry.malId}")
            desc.setAttribute("dc:rights", if (entry.isHentai) "Adult Content - 18+" else "© MyAnimeList")
            
            // MAL specific metadata
            desc.setAttribute("mal:id", entry.malId.toString())
            desc.setAttribute("mal:type", entry.type)
            desc.setAttribute("mal:score", entry.score.toString())
            desc.setAttribute("mal:status", entry.status)
            desc.setAttribute("mal:episodes", entry.episodes.toString())
            desc.setAttribute("mal:year", entry.year.toString())
            desc.setAttribute("mal:rating", entry.rating)
            desc.setAttribute("mal:source", entry.source)
            desc.setAttribute("xmp:Rating", (entry.score / 2).toInt().toString())
            
            if (entry.isHentai) {
                desc.setAttribute("mal:adult", "true")
            }
            
            // Studios/Publishers
            if (entry.studios.isNotEmpty()) {
                desc.setAttribute("mal:studios", entry.studios.joinToString(", "))
            }
            
            // COMPREHENSIVE SUBJECT TAGS - ALL 25+ TAGS EMBEDDED HERE
            val dcSubject = doc.createElementNS(dcNs, "dc:subject")
            val rdfBag = doc.createElementNS(rdfNs, "rdf:Bag")
            
            // Use ALL extracted tags (this is where the 25+ unique tags get embedded)
            entry.allTags.forEach { tag ->
                val li = doc.createElementNS(rdfNs, "rdf:li")
                li.textContent = tag
                rdfBag.appendChild(li)
            }
            
            dcSubject.appendChild(rdfBag)
            desc.appendChild(dcSubject)
            
            // Windows XP Keywords (semicolon-separated for AVES compatibility)
            desc.setAttribute("Iptc4xmpCore:Keywords", entry.allTags.joinToString(","))
            desc.setAttribute("xmp:Label", if (entry.isHentai) "Adult" else "General")
            
            val transformer = TransformerFactory.newInstance().newTransformer().apply { 
                setOutputProperty("omit-xml-declaration", "yes") 
            }
            val out = ByteArrayOutputStream()
            transformer.transform(DOMSource(doc), StreamResult(out))
            out.toByteArray()
        } catch (e: Exception) { 
            Log.e(TAG, "Failed creating XMP for ${entry.title}", e)
            ByteArray(0) 
        }
    }

    private fun embedXmpInJpeg(jpegBytes: ByteArray, xmpBytes: ByteArray): ByteArray {
        if (xmpBytes.isEmpty()) return jpegBytes
        val out = ByteArrayOutputStream()
        return try {
            out.write(jpegBytes, 0, 2)
            val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)
            val segmentSize = xmpHeader.size + xmpBytes.size + 2
            if (segmentSize <= 65535) {
                out.write(0xFF)
                out.write(0xE1)
                out.write((segmentSize shr 8) and 0xFF)
                out.write(segmentSize and 0xFF)
                out.write(xmpHeader)
                out.write(xmpBytes)
            }
            var i = 2
            while (i < jpegBytes.size) {
                if (jpegBytes[i] == 0xFF.toByte() && i + 1 < jpegBytes.size) {
                    val marker = jpegBytes[i + 1].toInt() and 0xFF
                    if (marker == 0xE1) { 
                        i += 2
                        if (i + 1 < jpegBytes.size) { 
                            val segLen = ((jpegBytes[i].toInt() and 0xFF) shl 8) or (jpegBytes[i + 1].toInt() and 0xFF)
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
            Log.e(TAG, "Error embedding XMP", e)
            jpegBytes 
        }
    }

    private suspend fun saveImageToStorage(imageBytes: ByteArray, entry: MediaEntry) {
        try {
            val folder = determineFolderStructure(entry)
            val fileName = "${sanitizeForFilename(entry.title)}_${entry.malId}.jpg"
            if (isDuplicate(entry.malId, fileName)) { 
                logMessage("⚠️ Skipping duplicate: ${entry.title}")
                return 
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply { 
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folder")
                    put(MediaStore.Images.Media.IS_PENDING, 1) 
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { imageUri -> 
                    contentResolver.openOutputStream(imageUri)?.use { 
                        it.write(imageBytes)
                        it.flush() 
                    }
                    contentResolver.update(imageUri, ContentValues().apply { 
                        put(MediaStore.Images.Media.IS_PENDING, 0) 
                    }, null, null)
                    addToFileIndex(entry.malId, fileName)
                    logMessage("✅ Saved: $fileName in $folder (${entry.allTags.size} tags embedded)") 
                }
            } else {
                logMessage("❌ Scoped storage not available on this Android version")
            }
        } catch (e: Exception) { 
            logMessage("❌ Save failed: ${entry.title} - ${e.message ?: "Unknown error"}")
            throw e 
        }
    }

    private fun determineFolderStructure(entry: MediaEntry): String {
        val base = "MAL_Export"
        val typeFolder = when {
            entry.isHentai && entry.type == "manga" -> "Hentai/Manga"
            entry.isHentai && entry.type == "anime" -> "Hentai/Anime"
            entry.isHentai -> "Hentai"
            entry.type == "anime" -> "Anime"
            entry.type == "manga" -> "Manga"
            else -> "Misc"
        }
        val primaryGenre = entry.genres.firstOrNull() ?: "Unknown"
        return "$base/$typeFolder/${sanitizeForFilename(primaryGenre)}"
    }

    private fun addToFileIndex(malId: Int, fileName: String) { 
        savedFileIndex.getOrPut(malId) { mutableSetOf() }.add(fileName) 
    }
    
    private fun isDuplicate(malId: Int, fileName: String) = savedFileIndex[malId]?.contains(fileName) == true

    private fun updateStatus(message: String) { 
        runOnUiThread { tvStatus.text = "Status: $message" } 
    }
    
    private fun showToast(message: String) { 
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() } 
    }
    
    private fun logMessage(message: String) { 
        runOnUiThread { 
            tvLogs.append("${System.currentTimeMillis() % 100000}: $message\n")
            scrollLogs.post { scrollLogs.fullScroll(View.FOCUS_DOWN) } 
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