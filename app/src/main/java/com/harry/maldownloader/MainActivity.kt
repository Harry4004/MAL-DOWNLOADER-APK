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
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvLogs: TextView
    private lateinit var btnLoadXml: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            tvLogs = findViewById(R.id.tvLogs)
            btnLoadXml = findViewById(R.id.btnLoadXml)

            btnLoadXml.setOnClickListener {
                openXmlFilePicker()
            }

            logMessage("App ready. Select an XML file to parse.")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showToast("Launch error: ${e.localizedMessage}")
        }
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
                var animeCount = 0
                var mangaCount = 0

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name?.lowercase()) {
                            "anime" -> animeCount++
                            "manga" -> mangaCount++
                        }
                    }
                    eventType = parser.next()
                }

                withContext(Dispatchers.Main) {
                    logMessage("XML parsing complete: Found $animeCount anime and $mangaCount manga entries.")
                }
            } catch (e: XmlPullParserException) {
                withContext(Dispatchers.Main) { logMessage("XML parsing error: ${e.message}") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { logMessage("Error reading XML: ${e.message}") }
            }
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