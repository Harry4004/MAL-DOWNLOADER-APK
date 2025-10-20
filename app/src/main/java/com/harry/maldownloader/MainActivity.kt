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
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.w3c.dom.Document
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name?.lowercase()) {
                            "anime", "manga", "hentai" -> {
                                val entry = parseMediaEntry(parser, parser.name ?: "")
                                entry?.let { entries.add(it) }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                withContext(Dispatchers.Main) {
                    logMessage("XML parsing complete: Found ${entries.size} entries. Starting info fetch...")
                }
                for (entry in entries) {
                    try {
                        fetchAndProcessMediaEntry(entry)
                        delay(1200)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed processing ${entry.title}", e)
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
                if (eventType == XmlPullParser.END_TAG && parser.name?.equals(type, ignoreCase = true) == true) break
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "series_animedb_id", "manga_mangadb_id" -> malId = parser.nextText()?.trim()
                        "series_title", "manga_title" -> title = parser.nextText()?.trim()
                        "genres" -> genres = parseGenres(parser)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $type entry", e)
        }
        return if (!malId.isNullOrEmpty() && malId.toIntOrNull() != null && !title.isNullOrEmpty()) {
            MediaEntry(malId.toInt(), title!!, type.lowercase(), genres, type.lowercase() == "hentai")
        } else null
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

    private suspend fun fetchAndProcessMediaEntry(entry: MediaEntry) {
        withContext(Dispatchers.Main) {
            logMessage("Fetching info for ${entry.type.capitalize()} '${entry.title}' (ID: ${entry.malId})")
        }
        try {
            val apiUrl = "https://api.jikan.moe/v4/${entry.type}/${entry.malId}"
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", "MAL-Downloader/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        logMessage("API request failed for ${entry.title}: ${response.code}")
                    }
                    return
                }
                val jsonString = response.body?.string() ?: ""
                if (jsonString.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        logMessage("Empty API response for ${entry.title}")
                    }
                    return
                }
                val json = JSONObject(jsonString)
                val data = json.getJSONObject("data")
                val imageUrl = data.getJSONObject("images").getJSONObject("jpg").optString("large_image_url", "")
                val apiGenres = mutableListOf<String>()
                val genresArray = data.optJSONArray("genres")
                if (genresArray != null) {
                    for (i in 0 until genresArray.length()) {
                        apiGenres.add(genresArray.getJSONObject(i).getString("name"))
                    }
                }
                val synopsis = data.optString("synopsis", "")
                val score = data.optDouble("score", 0.0).toFloat()
                val status = data.optString("status", "")
                val episodes = data.optInt("episodes", 0)
                val year = data.optJSONObject("aired")?.optString("from", "")?.take(4)?.toIntOrNull() ?: 0
                val enrichedEntry = entry.copy(
                    genres = apiGenres,
                    isHentai = apiGenres.any { it.contains("Hentai", ignoreCase = true) },
                    synopsis = synopsis,
                    score = score,
                    status = status,
                    episodes = episodes,
                    year = year
                )
                if (imageUrl.isNotEmpty()) {
                    downloadCoverAndEmbedXmp(imageUrl, enrichedEntry)
                } else {
                    withContext(Dispatchers.Main) {
                        logMessage("No image found for ${entry.title}")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logMessage("Error fetching info for ${entry.title}: ${e.message}")
            }
        }
    }

    private suspend fun downloadCoverAndEmbedXmp(imageUrl: String, entry: MediaEntry) {
        withContext(Dispatchers.Main) {
            logMessage("Downloading cover for ${entry.title}...")
        }
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .addHeader("User-Agent", "MAL-Downloader/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        logMessage("Image download failed for ${entry.title}: ${response.code}")
                    }
                    return
                }
                response.body?.byteStream()?.use { inputStream ->
                    saveImageWithXmp(inputStream, entry)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logMessage("Error downloading cover for ${entry.title}: ${e.message}")
            }
        }
    }

    private fun saveImageWithXmp(inputStream: InputStream, entry: MediaEntry) {
        try {
            val sanitizedFolder = when {
                entry.isHentai -> "Hentai/${entry.type.capitalize()}_Hentai"
                entry.type == "anime" -> "Anime"
                entry.type == "manga" -> "Manga/Anime_Manga"
                else -> "Misc"
            }
            val sanitizedGenre = entry.genres.firstOrNull()?.replace(Regex("[^\\w\\s-]"), "_")?.replace(" ", "_") ?: "Unknown"
            val folderPath = "MAL_Export/$sanitizedFolder/$sanitizedGenre"
            val fileName = "${entry.title.replace(Regex("[^\\w\\s-]"), "_").take(30)}_${entry.malId}.jpg"
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderPath")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { imageUri ->
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                    try {
                        val tempFile = File(cacheDir, fileName)
                        contentResolver.openInputStream(uri)?.use { uriInputStream ->
                            tempFile.outputStream().use { tempOutputStream ->
                                uriInputStream.copyTo(tempOutputStream)
                            }
                        }
                        writeXmpMetadata(tempFile, entry)
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            tempFile.inputStream().use { tempInputStream ->
                                tempInputStream.copyTo(outputStream)
                            }
                        }
                        tempFile.delete()
                        runOnUiThread { logMessage("XMP metadata embedded in ${entry.title}") }
                    } catch (xmpError: Exception) {
                        Log.e(TAG, "Failed writing XMP for ${entry.title}", xmpError)
                        runOnUiThread { logMessage("Failed to write XMP for ${entry.title}") }
                    }
                }
            } else {
                val picturesDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    folderPath
                )
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
                try {
                    writeXmpMetadata(file, entry)
                    runOnUiThread { logMessage("XMP metadata embedded in ${entry.title}") }
                } catch (xmpError: Exception) {
                    Log.e(TAG, "Failed writing XMP for ${entry.title}", xmpError)
                    runOnUiThread { logMessage("Failed to write XMP for ${entry.title}") }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image for ${entry.title}", e)
            runOnUiThread { logMessage("Failed saving image for ${entry.title}: ${e.message}") }
        }
    }

    private fun writeXmpMetadata(file: File, entry: MediaEntry) {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.newDocument()
            val rdf = doc.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF")
            rdf.setAttribute("xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
            rdf.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/")
            rdf.setAttribute("xmlns:mal", "http://myanimelist.net/")
            rdf.setAttribute("xmlns:xmp", "http://ns.adobe.com/xap/1.0/")
            doc.appendChild(rdf)
            val description = doc.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:Description")
            description.setAttribute("rdf:about", "")
            rdf.appendChild(description)
            description.setAttribute("dc:title", entry.title)
            description.setAttribute("dc:description", entry.synopsis.take(500))
            description.setAttribute("dc:creator", "MAL Downloader")
            description.setAttribute("dc:subject", entry.genres.joinToString(", "))
            description.setAttribute("mal:id", entry.malId.toString())
            description.setAttribute("mal:type", entry.type)
            description.setAttribute("mal:genres", entry.genres.joinToString(", "))
            description.setAttribute("mal:score", entry.score.toString())
            description.setAttribute("mal:status", entry.status)
            description.setAttribute("mal:episodes", entry.episodes.toString())
            description.setAttribute("mal:year", entry.year.toString())
            description.setAttribute("xmp:Rating", (entry.score / 2).toInt().toString())
            if (entry.isHentai) {
                description.setAttribute("dc:rights", "Adult Content - Hentai")
                description.setAttribute("mal:adult", "true")
            }
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty("omit-xml-declaration", "yes")
            val source = DOMSource(doc)
            val outputStream = ByteArrayOutputStream()
            val result = StreamResult(outputStream)
            transformer.transform(source, result)
            val xmpString = outputStream.toString("UTF-8")
            val xmpBytes = xmpString.toByteArray(Charsets.UTF_8)
            val originalBytes = file.readBytes()
            val newBytes = embedXmpInJpeg(originalBytes, xmpBytes)
            file.writeBytes(newBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating XMP for ${entry.title}", e)
            throw e
        }
    }

    private fun embedXmpInJpeg(jpegBytes: ByteArray, xmpBytes: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        try {
            outputStream.write(jpegBytes, 0, 2)
            val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)
            val segmentSize = xmpHeader.size + xmpBytes.size + 2
            if (segmentSize <= 65535) {
                outputStream.write(0xFF)
                outputStream.write(0xE1)
                outputStream.write((segmentSize shr 8) and 0xFF)
                outputStream.write(segmentSize and 0xFF)
                outputStream.write(xmpHeader)
                outputStream.write(xmpBytes)
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
                outputStream.write(jpegBytes[i].toInt() and 0xFF)
                i++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding XMP", e)
            return jpegBytes
        }
        return outputStream.toByteArray()
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
