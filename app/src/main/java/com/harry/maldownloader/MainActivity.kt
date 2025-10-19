package com.harry.maldownloader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private lateinit var tvLogs: TextView
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            tvLogs = findViewById(R.id.tvLogs)
            findViewById<Button>(R.id.btnLoadXml).setOnClickListener {
                if (!isProcessing) {
                    openXmlFilePicker()
                } else {
                    showToast("Already processing a file. Please wait...")
                }
            }
            logMessage("App started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showToast("Failed to initialize app: ${e.message}")
        }
    }

    private fun openXmlFilePicker() {
        try {
            logMessage("Opening file picker...")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*" // Accept all file types initially
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
                    var animeCount = 0
                    var processedCount = 0
                    
                    withContext(Dispatchers.Main) {
                        logMessage("Starting XML parsing loop...")
                    }
                    
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        try {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    if (parser.name?.equals("anime", ignoreCase = true) == true) {
                                        animeCount++
                                        val animeData = parseAnimeEntry(parser)
                                        
                                        animeData?.let { (malId, title) ->
                                            withContext(Dispatchers.Main) {
                                                logMessage("Found anime: $title (ID: $malId)")
                                            }
                                            
                                            // Process every 10th anime to avoid overwhelming the API
                                            if (processedCount < 10) {
                                                downloadAndSaveCover(malId, title)
                                                processedCount++
                                                
                                                // Add delay between requests to respect API limits
                                                kotlinx.coroutines.delay(1000)
                                            }
                                        }
                                        
                                        if (animeCount % 50 == 0) {
                                            withContext(Dispatchers.Main) {
                                                logMessage("Processed $animeCount anime entries...")
                                            }
                                        }
                                    }
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
                        logMessage("XML parsing completed. Found $animeCount anime entries, processed $processedCount covers.")
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
    
    private fun parseAnimeEntry(parser: XmlPullParser): Pair<Int, String>? {
        var malId: String? = null
        var title: String? = null
        
        try {
            while (true) {
                val eventType = parser.next()
                
                if (eventType == XmlPullParser.END_TAG && parser.name?.equals("anime", ignoreCase = true) == true) {
                    break
                }
                
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "series_animedb_id" -> {
                            malId = parser.nextText()?.trim()
                        }
                        "series_title" -> {
                            title = parser.nextText()?.trim()
                        }
                    }
                }
            }
            
            return if (!malId.isNullOrEmpty() && malId.toIntOrNull() != null) {
                val safeTitle = title?.takeIf { it.isNotEmpty() } ?: "Anime-$malId"
                malId.toInt() to safeTitle
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing anime entry", e)
            return null
        }
    }

    private suspend fun downloadAndSaveCover(animeId: Int, animeTitle: String) {
        try {
            withContext(Dispatchers.Main) {
                logMessage("Downloading cover for: $animeTitle")
            }
            
            // 1. Fetch anime info from Jikan API
            val url = "https://api.jikan.moe/v4/anime/$animeId"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MAL-Downloader/1.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        logMessage("API request failed for $animeTitle: ${response.code}")
                    }
                    return
                }
                
                val jsonString = response.body?.string() ?: ""
                if (jsonString.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        logMessage("Empty response for $animeTitle")
                    }
                    return
                }
                
                val json = JSONObject(jsonString)
                val data = json.getJSONObject("data")
                val images = data.getJSONObject("images")
                val jpg = images.getJSONObject("jpg")
                val imageUrl = jpg.optString("large_image_url", "")
                
                if (imageUrl.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        logMessage("No image URL found for $animeTitle")
                    }
                    return
                }
                
                // 2. Download the image
                val imageRequest = Request.Builder()
                    .url(imageUrl)
                    .addHeader("User-Agent", "MAL-Downloader/1.0")
                    .build()
                    
                client.newCall(imageRequest).execute().use { imageResponse ->
                    if (!imageResponse.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            logMessage("Image download failed for $animeTitle: ${imageResponse.code}")
                        }
                        return
                    }
                    
                    val inputStream = imageResponse.body?.byteStream()
                    if (inputStream != null) {
                        saveImageStream(inputStream, animeTitle)
                        withContext(Dispatchers.Main) {
                            logMessage("✓ Successfully saved cover for $animeTitle")
                        }
                    }
                }
            }
            
        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing error for $animeTitle", e)
            withContext(Dispatchers.Main) {
                logMessage("JSON error for $animeTitle: ${e.message}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error for $animeTitle", e)
            withContext(Dispatchers.Main) {
                logMessage("Network error for $animeTitle: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error for $animeTitle", e)
            withContext(Dispatchers.Main) {
                logMessage("Error for $animeTitle: ${e.message}")
            }
        }
    }

    private fun saveImageStream(inputStream: java.io.InputStream, seriesName: String) {
        try {
            // Sanitize filename to prevent issues
            val sanitizedName = seriesName.replace(Regex("[^\\w\\s-]"), "_")
                .replace(Regex("\\s+"), "_")
                .take(50) // Limit filename length
            
            val filename = "${sanitizedName}.jpg"
            
            if (Build.VERSION.SDK_INT >= 29) {
                // Use MediaStore for Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MAL_Export")
                }
                
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { imageUri ->
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                // Legacy path for older Android versions
                val picturesDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES
                    ), "MAL_Export"
                )
                
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }
                
                val file = File(picturesDir, filename)
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image for $seriesName", e)
            runOnUiThread {
                logMessage("Failed to save image for $seriesName: ${e.message}")
            }
        }
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