package com.harry.maldownloader

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.harry.maldownloader.adapter.AnimeAdapter
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.pipeline.MalPipeline
import kotlinx.coroutines.launch
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var selectFileBtn: Button
    private lateinit var downloadBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollView: ScrollView  // For auto-scroll logs
    private var selectedFileUri: Uri? = null
    private lateinit var malPipeline: MalPipeline
    private lateinit var animeAdapter: AnimeAdapter
    private val entries: MutableList<AnimeEntry> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)  // Force dark theme
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        selectFileBtn = findViewById(R.id.selectFileBtn)
        downloadBtn = findViewById(R.id.downloadBtn)
        recyclerView = findViewById(R.id.animeListRecycler)
        scrollView = findViewById(R.id.logScrollView)  // Assume added in layout
        malPipeline = MalPipeline(this) { log(it) }

        checkPermissions()

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        animeAdapter = AnimeAdapter(entries) { position, newTags ->
            if (position < entries.size) {
                val entry = entries[position].copy(tags = newTags.joinToString(","))
                entries[position] = entry
                animeAdapter.notifyItemChanged(position)
                log("Tags updated for ${entry.title}: ${entry.tags}")
            }
        }
        recyclerView.adapter = animeAdapter
        recyclerView.visibility = View.GONE

        val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedFileUri = it
                val fileName = getFileNameFromUri(it)
                log("Selected file: $fileName")
                downloadBtn.isEnabled = true
            }
        }

        selectFileBtn.setOnClickListener {
            filePicker.launch("application/xml")  // Prefer XML files
        }

        downloadBtn.setOnClickListener {
            selectedFileUri?.let { uri ->
                lifecycleScope.launch {
                    log("Parsing MAL XML file...")
                    entries.clear()
                    val processedEntries = malPipeline.processMalFile(uri)
                    entries.addAll(processedEntries)
                    animeAdapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                    log("Done. Displayed ${entries.size} anime entries with images and editable tags.")
                }
            } ?: log("Please select a MAL XML file first.")
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "unknown.xml"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun log(msg: String) {
        Log.d("MALDownloader", msg)  // For Logcat debugging
        runOnUiThread {
            logView.append("$msg\n")
            // Auto-scroll to bottom
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                log("Permissions granted.")
            } else {
                log("Permissions denied. File access limited.")
            }
        }
    }
}
