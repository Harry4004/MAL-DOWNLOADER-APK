package com.harry.maldownloader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
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
    val isHentai: Boolean = false,
    val otherFields: Map<String, String> = emptyMap()
)

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private lateinit var tvLogs: TextView
    private lateinit var btnLoadXml: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLogs = findViewById(R.id.tvLogs)
        btnLoadXml = findViewById(R.id.btnLoadXml)

        btnLoadXml.setOnClickListener {
            openXmlFilePicker()
        }

        logMessage("App ready. Select an XML file to parse.")
    }

    private fun openXmlFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/xml", "application/xml"))
            }
            startActivityForResult(intent, REQUEST_CODE_XML)
        } catch (e: Exception) {
            showToast("Failed to open file picker: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_XML && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                parseXml(uri)
            }
        }
    }

    private fun parseXml(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        showToast("Unable to open selected file.")
                    }
                    return@launch
                }

                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)

                var eventType = parser.eventType
                var entries = mutableListOf<MediaEntry>()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name?.lowercase()) {
                            "anime", "manga" -> {
                                val entry = parseMediaEntry(parser, parser.name ?: "")
                                entry?.let { entries.add(it) }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                withContext(Dispatchers.Main) {
                    logMessage("XML parsing complete: Found ${entries.size} entries.")
                }

                // Process sequentially: add metadata fetching and saving
                for (entry in entries) {
                    try {
                        downloadAndSaveCover(entry)
                        kotlinx.coroutines.delay(1200) // Respect API rate limit
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing entry ${entry.title}", e)
                        withContext(Dispatchers.Main) {
                            logMessage("Failed to process ${entry.title}")
                        }
                    }
                }

            } catch (e: XmlPullParserException) {
                withContext(Dispatchers.Main) { logMessage("XML parsing error: ${e.message}") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { logMessage("Error reading XML: ${e.message}") }
            }
        }
    }

    private fun parseMediaEntry(parser: XmlPullParser, type: String): MediaEntry? {
        var malId: String? = null
        var title: String? = null
        var genres = mutableListOf<String>()

        try {
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.name?.equals(type, ignoreCase = true) == true) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "series_animedb_id", "series_mangadb_id" -> malId = parser.nextText()?.trim()
                        "series_title" -> title = parser.nextText()?.trim()
                        "genres" -> genres = parseGenres(parser)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $type entry", e)
        }

        return if (!malId.isNullOrEmpty() && malId.toIntOrNull() != null && !title.isNullOrEmpty()) {
            MediaEntry(
                malId.toInt(),
                title!!,
                type.lowercase(),
                genres
            )
        } else {
            null
        }
    }

    private fun parseGenres(parser: XmlPullParser): MutableList<String> {
        val genreList = mutableListOf<String>()
        try {
            var eventType = parser.eventType
            while (!(eventType == XmlPullParser.END_TAG && parser.name == "genres")) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "genre") {
                    genreList.add(parser.nextText())
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing genres", e)
        }
        return genreList
    }

    private suspend fun downloadAndSaveCover(mediaEntry: MediaEntry) {
        withContext(Dispatchers.Main) {
            logMessage("Downloading cover: ${mediaEntry.title} (ID: ${mediaEntry.malId})")
        }

        // Placeholder for full MAL metadata extraction and EXIF embedding (next update)
        // Downloads image, saves under correct genre-based folder, embeds rich metadata
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun logMessage(message: String) {
        runOnUiThread {
            tvLogs.append("${System.currentTimeMillis() % 100000}: $message\n")
        }
    }

    companion object {
        private const val TAG = "MALDownloader"
        private const val REQUEST_CODE_XML = 1001
    }
}