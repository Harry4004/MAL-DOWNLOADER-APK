package com.harry.maldownloader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Xml
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

data class MediaEntry(
    val malId: Int,
    val title: String,
    val type: String // "anime" or "manga"
)

class MainActivity : AppCompatActivity() {
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
                val entries = mutableListOf<MediaEntry>()
                var animeCount = 0
                var mangaCount = 0
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name?.lowercase()) {
                            "anime" -> {
                                animeCount++
                                val entry = parseMediaEntry(parser, "anime")
                                entry?.let { entries.add(it) }
                            }
                            "manga" -> {
                                mangaCount++
                                val entry = parseMediaEntry(parser, "manga")
                                entry?.let { entries.add(it) }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                withContext(Dispatchers.Main) {
                    logMessage("XML parsing complete: Found $animeCount anime and $mangaCount manga entries. Starting info fetch...")
                }
                for (entry in entries) {
                    withContext(Dispatchers.Main) {
                        logMessage("Ready to fetch info for ${entry.type.capitalize()} ${entry.title} (ID: ${entry.malId})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { logMessage("Error reading XML: ${e.message}") }
            }
        }
    }

    private fun parseMediaEntry(parser: XmlPullParser, type: String): MediaEntry? {
        var malId: String? = null
        var title: String? = null
        try {
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.name?.equals(type, ignoreCase = true) == true) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG) {
                    if (type == "anime") {
                        if (parser.name == "series_animedb_id") malId = parser.nextText()?.trim()
                        if (parser.name == "series_title") title = parser.nextText()?.trim()
                    } else if (type == "manga") {
                        if (parser.name == "manga_mangadb_id") malId = parser.nextText()?.trim()
                        if (parser.name == "manga_title") title = parser.nextText()?.trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $type entry", e)
        }
        return if (!malId.isNullOrEmpty() && malId.toIntOrNull() != null && !title.isNullOrEmpty()) {
            MediaEntry(malId.toInt(), title!!, type.lowercase())
        } else {
            null
        }
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