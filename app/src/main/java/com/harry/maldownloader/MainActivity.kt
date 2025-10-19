package com.harry.maldownloader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Xml
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var tvLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvLogs = findViewById(R.id.tvLogs)
        findViewById<Button>(R.id.btnLoadXml).setOnClickListener {
            // Open document picker for XML files
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/xml"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/xml","application/xml"))
            }
            startActivityForResult(intent, REQUEST_CODE_XML)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_XML && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                parseMalXml(uri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun parseMalXml(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            // Launch a coroutine to do parsing and downloading off the main thread
            lifecycleScope.launch(Dispatchers.IO) {
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)
                var event = parser.eventType
                // Iterate through XML
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "anime") {
                        // Found an <anime> entry; read its children
                        var malId: String? = null
                        var title: String? = null
                        while (true) {
                            event = parser.next()
                            if (event == XmlPullParser.END_TAG && parser.name == "anime") break
                            if (event == XmlPullParser.START_TAG) {
                                when (parser.name) {
                                    "series_animedb_id" -> malId = parser.nextText().trim()
                                    "series_title" -> title = parser.nextText().trim()
                                }
                            }
                        }
                        // If we got both ID and title, process it
                        if (!malId.isNullOrEmpty()) {
                            val name = title ?: "Anime-$malId"
                            runOnUiThread { tvLogs.append("Processing: $name (ID $malId)\n") }
                            downloadAndSaveCover(malId.toInt(), name)
                        }
                    }
                    event = parser.next()
                }
                runOnUiThread { tvLogs.append("Done processing XML.\n") }
            }
        } ?: run {
            tvLogs.append("Failed to open XML file.\n")
        }
    }

    private fun downloadAndSaveCover(animeId: Int, animeTitle: String) {
        try {
            // 1. Fetch anime info from Jikan
            val url = "https://api.jikan.moe/v4/anime/$animeId"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) {
                    runOnUiThread { tvLogs.append("Failed to fetch anime $animeId data.\n") }
                    return
                }
                val json = JSONObject(res.body!!.string())
                val data = json.getJSONObject("data")
                // Attempt to get large image URL
                val images = data.getJSONObject("images").getJSONObject("jpg")
                val imageUrl = images.optString("large_image_url", "")
                if (imageUrl.isEmpty()) {
                    runOnUiThread { tvLogs.append("No image URL for $animeTitle.\n") }
                    return
                }
                // 2. Download image bytes
                val imgReq = Request.Builder().url(imageUrl).build()
                client.newCall(imgReq).execute().use { imgRes ->
                    if (!imgRes.isSuccessful) {
                        runOnUiThread { tvLogs.append("Failed to download image for $animeTitle.\n") }
                        return
                    }
                    val input = imgRes.body!!.byteStream()
                    // 3. Save to MediaStore or external file
                    saveImageStream(input, animeTitle)
                    runOnUiThread { tvLogs.append("Saved cover for $animeTitle\n") }
                }
            }
        } catch (e: Exception) {
            runOnUiThread { tvLogs.append("Error for $animeTitle: ${e.message}\n") }
        }
    }

    private fun saveImageStream(inputStream: java.io.InputStream, seriesName: String) {
        val filename = "${seriesName}.jpg"
        if (Build.VERSION.SDK_INT >= 29) {
            // Use MediaStore for Android 10+
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MAL_Export/$seriesName")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { out ->
                    inputStream.copyTo(out)
                }
            }
        } else {
            // Legacy path for older Android versions
            val picturesDir = 
                File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "MAL_Export/$seriesName")
            picturesDir.mkdirs()
            val file = File(picturesDir, filename)
            FileOutputStream(file).use { out ->
                inputStream.copyTo(out)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_XML = 1001
    }
}