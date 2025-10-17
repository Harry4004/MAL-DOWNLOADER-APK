package com.harry.maldownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var startButton: Button

    // TODO: Replace this placeholder with your actual MAL API client ID string
    private val clientId = "aaf018d4c098158bd890089f32125add"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logTextView = findViewById(R.id.logTextView)
        logScrollView = findViewById(R.id.logScrollView)
        startButton = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            checkPermissionsAndStart()
        }
    }

    private fun checkPermissionsAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                startProcessing()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startProcessing()
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startProcessing() {
        // Reset logs
        logTextView.text = ""

        // Suppose XML file is known or selected, here hardcoded for demo
        val xmlFile = File(getExternalFilesDir(null), "myanimelist_export.xml")
        if (!xmlFile.exists()) {
            appendLog("Error: MAL export XML file not found at ${xmlFile.absolutePath}")
            return
        }

        appendLog("Parsing MAL XML...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entries = MalParser.parse(xmlFile)
                appendLog("Found ${entries.size} entries.")

                entries.forEach { entry ->
                    appendLog("Fetching image for: ${entry.title}")

                    val imageUrl = Downloader.fetchAnimeImage(entry.id, clientId)
                    if (imageUrl != null) {
                        appendLog("Downloading image: $imageUrl")
                        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        val cleanTitle = entry.title.replace("[^a-zA-Z0-9 ]".toRegex(), "_")
                        val saveFile = File(picturesDir, "$cleanTitle.jpg")
                        Downloader.downloadImage(imageUrl, saveFile)
                        appendLog("Saved image to ${saveFile.absolutePath}")
                    } else {
                        appendLog("Image not found for ${entry.title}")
                    }
                }

                appendLog("Done processing all entries.")
            } catch (e: Exception) {
                appendLog("Error during processing: ${e.message}")
                Log.e("MALDownloader", "Error", e)
            }
        }
    }

    private suspend fun appendLog(text: String) = withContext(Dispatchers.Main) {
        logTextView.append("$text\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
