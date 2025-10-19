package com.harry.maldownloader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Xml
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class MediaEntry(
    val malId: Int,
    val title: String,
    val type: String, // "anime" or "manga"
    val genres: List<String> = emptyList(),
    val isHentai: Boolean = false
)

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private lateinit var tvLogs: TextView
    private lateinit var btnToggleNightMode: Button
    private var isProcessing = false

    private lateinit var prefs: SharedPreferences
    private val PREFS_NAME = "mal_downloader_prefs"
    private val PREF_NIGHT_MODE = "night_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize preferences first
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Apply saved night mode preference before setContentView
        val isNightMode = prefs.getBoolean(PREF_NIGHT_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            tvLogs = findViewById(R.id.tvLogs)
            btnToggleNightMode = findViewById(R.id.btnToggleNightMode)

            // Set initial button text
            updateNightModeButtonText(isNightMode)

            btnToggleNightMode.setOnClickListener {
                toggleNightMode()
            }

            findViewById<Button>(R.id.btnLoadXml).setOnClickListener {
                if (!isProcessing) {
                    openXmlFilePicker()
                } else {
                    showToast("Already processing a file. Please wait...")
                }
            }
            logMessage("App started successfully - Night mode: $isNightMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showToast("Failed to initialize app: ${e.message}")
        }
    }

    private fun toggleNightMode() {
        val currentNightMode = prefs.getBoolean(PREF_NIGHT_MODE, false)
        val newNightMode = !currentNightMode
        
        // Save preference
        prefs.edit().putBoolean(PREF_NIGHT_MODE, newNightMode).apply()
        
        // Apply new mode
        AppCompatDelegate.setDefaultNightMode(
            if (newNightMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        // Update button text immediately
        updateNightModeButtonText(newNightMode)
        
        showToast("Switched to ${if (newNightMode) "Night" else "Day"} Mode")
        logMessage("Night mode toggled: $newNightMode")
    }
    
    private fun updateNightModeButtonText(isNightMode: Boolean) {
        btnToggleNightMode.text = if (isNightMode) "Switch to Day Mode" else "Switch to Night Mode"
    }

    private fun openXmlFilePicker() {
        try {
            logMessage("Opening file picker...")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/xml",
                    "application/xml",
                    "text/plain",
                    "application/octet-stream"
                ))
            }
            startActivityForResult(intent, REQUEST_CODE_XML)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file picker", e)
            logMessage("Error opening file picker: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        try {
            if (requestCode == REQUEST_CODE_XML) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        data?.data?.let { uri ->
                            logMessage("File selected: $uri")
                            parseMalXml(uri)
                        } ?: logMessage("No file URI received")
                    }
                    Activity.RESULT_CANCELED -> {
                        logMessage("File selection cancelled")
                    }
                    else -> {
                        logMessage("File selection failed with result code: $resultCode")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onActivityResult", e)
            logMessage("Error handling file selection: ${e.message}")
        }
    }

    private fun parseMalXml(uri: Uri) {
        if (isProcessing) {
            logMessage("Already processing a file")
            return
        }
        
        isProcessing = true
        logMessage("Starting XML parsing...")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    logMessage("Opening input stream...")
                }
                
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    withContext(Dispatchers.Main) {
                        logMessage("Input stream opened successfully")
                    }
                    
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(inputStream, null)
                    
                    var eventType = parser.eventType
                    var totalCount = 0
                    
                    withContext(Dispatchers.Main) {
                        logMessage("Starting XML parsing loop...")
                    }
                    
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        try {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    when (parser.name?.lowercase()) {
                                        "anime" -> {
                                            totalCount++
                                            val mediaEntry = parseMediaEntry(parser, "anime")
                                            mediaEntry?.let { entry ->
                                                withContext(Dispatchers.Main) {
                                                    logMessage("Found anime: ${entry.title} (ID: ${entry.malId})")
                                                }
                                                downloadAndSaveCover(entry)
                                                kotlinx.coroutines.delay(1000)
                                            }
                                        }
                                        "manga" -> {
                                            totalCount++
                                            val mediaEntry = parseMediaEntry(parser, "manga")
                                            mediaEntry?.let { entry ->
                                                withContext(Dispatchers.Main) {
                                                    logMessage("Found manga: ${entry.title} (ID: ${entry.malId})")
                                                }
                                                downloadAndSaveCover(entry)
                                                kotlinx.coroutines.delay(1000)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (totalCount % 50 == 0 && totalCount > 0) {
                                withContext(Dispatchers.Main) {
                                    logMessage("Processed $totalCount entries...")
                                }
                            }
                            
                            eventType = parser.next()
                        } catch (e: XmlPullParserException) {
                            Log.e(TAG, "XML parsing error at position ${parser.lineNumber}:${parser.columnNumber}", e)
                            withContext(Dispatchers.Main) {
                                logMessage("XML parsing error: ${e.message}")
                            }
                            break
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        logMessage("XML parsing completed. Found $totalCount total entries.")
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        logMessage("Failed to open input stream for URI: $uri")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during XML parsing", e)
                withContext(Dispatchers.Main) {
                    logMessage("XML parsing failed: ${e.javaClass.simpleName} - ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    logMessage("XML parsing process finished.")
                }
            }
        }
    }

    private fun parseMediaEntry(parser: XmlPullParser, mediaType: String): MediaEntry? {
        var malId: String? = null
        var title: String? = null
        
        try {
            while (true) {
                val eventType = parser.next()
                
                if (eventType == XmlPullParser.END_TAG && 
                    parser.name?.lowercase() == mediaType) {
                    break
                }
                
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "series_animedb_id", "series_mangadb_id" -> {
                            malId = parser.nextText()?.trim()
                        }
                        "series_title" -> {
                            title = parser.nextText()?.trim()
                        }
                    }
                }
            }
            
            return if (!malId.isNullOrEmpty() && malId.toIntOrNull() != null) {
                val safeTitle = title?.takeIf { it.isNotEmpty() } ?: "${mediaType.capitalize()}-$malId"
                MediaEntry(
                    malId = malId.toInt(),
                    title = safeTitle,
                    type = mediaType
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $mediaType entry", e)
            return null
        }
    }

    private suspend fun downloadAndSaveCover(mediaEntry: MediaEntry) {
        try {
            withContext(Dispatchers.Main) {
                logMessage("Downloading cover for: ${mediaEntry.title} (${mediaEntry.type})")
            }
            
            // Fetch media info from Jikan API
            val url = "https://api.jikan.moe/v4/${mediaEntry.type}/${mediaEntry.malId}"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MAL-Downloader/1.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        logMessage("API request failed for ${mediaEntry.title}: ${response.code}")
                    }
                    return
                }
                
                val jsonString = response.body?.string() ?: ""
                if (jsonString.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        logMessage("Empty response for ${mediaEntry.title}")
                    }
                    return
                }
                
                val json = JSONObject(jsonString)
                val data = json.getJSONObject("data")
                
                // Extract genres
                val genres = mutableListOf<String>()
                val genresArray = data.optJSONArray("genres") ?: JSONArray()
                for (i in 0 until genresArray.length()) {
                    val genre = genresArray.getJSONObject(i)
                    genres.add(genre.getString("name"))
                }
                
                // Check if it's hentai
                val isHentai = genres.any { it.lowercase().contains("hentai") } ||
                              mediaEntry.title.lowercase().contains("hentai")
                
                val enhancedEntry = mediaEntry.copy(
                    genres = genres,
                    isHentai = isHentai
                )
                
                val images = data.getJSONObject("images")
                val jpg = images.getJSONObject("jpg")
                val imageUrl = jpg.optString("large_image_url", "")
                
                if (imageUrl.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        logMessage("No image URL found for ${mediaEntry.title}")
                    }
                    return
                }
                
                // Download the image
                val imageRequest = Request.Builder()
                    .url(imageUrl)
                    .addHeader("User-Agent", "MAL-Downloader/1.0")
                    .build()
                    
                client.newCall(imageRequest).execute().use { imageResponse ->
                    if (!imageResponse.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            logMessage("Image download failed for ${mediaEntry.title}: ${imageResponse.code}")
                        }
                        return
                    }
                    
                    val inputStream = imageResponse.body?.byteStream()
                    if (inputStream != null) {
                        saveImageWithGenreBasedFolder(inputStream, enhancedEntry)
                        withContext(Dispatchers.Main) {
                            logMessage("✓ Successfully saved cover for ${mediaEntry.title}")
                        }
                    }
                }
            }
            
        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing error for ${mediaEntry.title}", e)
            withContext(Dispatchers.Main) {
                logMessage("JSON error for ${mediaEntry.title}: ${e.message}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error for ${mediaEntry.title}", e)
            withContext(Dispatchers.Main) {
                logMessage("Network error for ${mediaEntry.title}: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error for ${mediaEntry.title}", e)
            withContext(Dispatchers.Main) {
                logMessage("Error for ${mediaEntry.title}: ${e.message}")
            }
        }
    }

    private fun saveImageWithGenreBasedFolder(inputStream: InputStream, mediaEntry: MediaEntry) {
        try {
            // Create folder structure based on content type and genres
            val folderStructure = createFolderStructure(mediaEntry)
            val sanitizedFileName = "${mediaEntry.title.replace(Regex("[^\\w\\s-]"), "_").take(30)}_${mediaEntry.malId}.jpg"

            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, sanitizedFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MAL_Export/$folderStructure")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { imageUri ->
                    // Save image data first
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                    
                    // Add EXIF metadata
                    try {
                        contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                            val exif = ExifInterface(pfd.fileDescriptor)
                            
                            // Embed comprehensive metadata
                            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, 
                                "MAL ${mediaEntry.type.uppercase()} ID: ${mediaEntry.malId}")
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, 
                                "Title: ${mediaEntry.title} | Genres: ${mediaEntry.genres.joinToString(", ")}")
                            exif.setAttribute(ExifInterface.TAG_ARTIST, "MAL Downloader")
                            
                            if (mediaEntry.isHentai) {
                                exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "Adult Content - Hentai")
                            }
                            
                            exif.saveAttributes()
                            
                            runOnUiThread {
                                logMessage("✓ EXIF metadata added to ${mediaEntry.title}")
                            }
                        }
                    } catch (exifError: Exception) {
                        Log.e(TAG, "Error adding EXIF metadata for ${mediaEntry.title}", exifError)
                        runOnUiThread {
                            logMessage("⚠ Failed to add EXIF metadata for ${mediaEntry.title}")
                        }
                    }
                }

            } else {
                // Legacy path for older Android versions
                val picturesDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES
                    ), "MAL_Export/$folderStructure"
                )
                
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }
                
                val file = File(picturesDir, sanitizedFileName)
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
                
                // Add EXIF metadata
                try {
                    val exif = ExifInterface(file.absolutePath)
                    
                    exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, 
                        "MAL ${mediaEntry.type.uppercase()} ID: ${mediaEntry.malId}")
                    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, 
                        "Title: ${mediaEntry.title} | Genres: ${mediaEntry.genres.joinToString(", ")}")
                    exif.setAttribute(ExifInterface.TAG_ARTIST, "MAL Downloader")
                    
                    if (mediaEntry.isHentai) {
                        exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "Adult Content - Hentai")
                    }
                    
                    exif.saveAttributes()
                    
                    runOnUiThread {
                        logMessage("✓ EXIF metadata added to ${mediaEntry.title}")
                    }
                } catch (exifError: Exception) {
                    Log.e(TAG, "Error adding EXIF metadata for ${mediaEntry.title}", exifError)
                    runOnUiThread {
                        logMessage("⚠ Failed to add EXIF metadata for ${mediaEntry.title}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image for ${mediaEntry.title}", e)
            runOnUiThread {
                logMessage("Failed to save image for ${mediaEntry.title}: ${e.message}")
            }
        }
    }
    
    private fun createFolderStructure(mediaEntry: MediaEntry): String {
        val pathComponents = mutableListOf<String>()
        
        when (mediaEntry.type.lowercase()) {
            "anime" -> {
                if (mediaEntry.isHentai) {
                    pathComponents.add("Hentai")
                    pathComponents.add("Anime Hentai")
                } else {
                    pathComponents.add("Anime")
                }
            }
            "manga" -> {
                if (mediaEntry.isHentai) {
                    pathComponents.add("Hentai")
                    pathComponents.add("Hentai Manga")
                } else {
                    pathComponents.add("Manga")
                    pathComponents.add("Anime Manga")
                }
            }
        }
        
        // Add primary genre if available
        if (mediaEntry.genres.isNotEmpty()) {
            val primaryGenre = mediaEntry.genres.first()
                .replace(Regex("[^\\w\\s-]"), "_")
                .replace(Regex("\\s+"), "_")
            pathComponents.add(primaryGenre)
        }
        
        return pathComponents.joinToString("/")
    }

    private fun logMessage(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            tvLogs.append("${System.currentTimeMillis() % 100000}: $message\n")
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "MALDownloader"
        private const val REQUEST_CODE_XML = 1001
    }
}