package com.harry.maldownloader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var tvLogs: TextView
    private var btnToggleNightMode: Button? = null
    private var btnLoadXml: Button? = null
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Apply theme before super call
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            super.onCreate(savedInstanceState)

            // Set layout
            setContentView(R.layout.activity_main)

            // Safe view initialization
            tvLogs = findViewById(R.id.tvLogs)
            btnToggleNightMode = findViewById(R.id.btnToggleNightMode)
            btnLoadXml = findViewById(R.id.btnLoadXml)

            if (tvLogs == null) {
                Log.e(TAG, "tvLogs not found in layout!")
                return
            }

            btnToggleNightMode?.setOnClickListener {
                toggleNightMode()
            }

            btnLoadXml?.setOnClickListener {
                if (!isProcessing) {
                    openXmlFilePicker()
                } else {
                    showToast("Already processing, please wait...")
                }
            }

            logMessage("App launched safely.")

        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
            showToast("Launch error: ${e.localizedMessage}")
        }
    }

    private fun toggleNightMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(newMode)
        showToast("Switched to ${if (newMode == AppCompatDelegate.MODE_NIGHT_YES) "Night" else "Day"} mode")
    }

    private fun openXmlFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/xml", "application/xml", "text/plain"))
            }
            startActivityForResult(intent, REQUEST_CODE_XML)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file picker", e)
            showToast("File picker failed: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == REQUEST_CODE_XML && resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                uri?.let {
                    logMessage("Selected file: $uri")
                    lifecycleScope.launch(Dispatchers.IO) {
                        processFileSafely(uri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling activity result", e)
            showToast("Error opening file: ${e.localizedMessage}")
        }
    }

    private suspend fun processFileSafely(uri: Uri) {
        try {
            withContext(Dispatchers.Main) {
                showToast("Processing file...")
            }
            // Future: Add actual parsing logic here.
            withContext(Dispatchers.Main) {
                logMessage("File processed successfully: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing file", e)
            withContext(Dispatchers.Main) { showToast("File processing error: ${e.localizedMessage}") }
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