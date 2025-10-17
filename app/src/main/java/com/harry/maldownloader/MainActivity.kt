package com.harry.maldownloader

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harry.maldownloader.adapter.AnimeAdapter
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var selectFileBtn: Button
    private lateinit var downloadBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollView: ScrollView
    private var selectedFileUri: Uri? = null
    private lateinit var malPipeline: MalPipeline
    private lateinit var animeAdapter: AnimeAdapter
    private val entries: MutableList<AnimeEntry> = mutableListOf()

    private val uiScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        selectFileBtn = findViewById(R.id.selectFileBtn)
        downloadBtn = findViewById(R.id.downloadBtn)
        recyclerView = findViewById(R.id.animeListRecycler)
        scrollView = findViewById(R.id.logScrollView)
        malPipeline = MalPipeline(this) { log(it) }

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

        selectFileBtn.setOnClickListener { filePicker.launch("application/xml") }

        downloadBtn.setOnClickListener {
            selectedFileUri?.let { uri ->
                uiScope.launch {
                    log("Parsing MAL XML file...")
                    entries.clear()
                    val processedEntries = malPipeline.processMalFile(uri)
                    entries.addAll(processedEntries)
                    animeAdapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                    log("Done. Displayed ${entries.size} anime entries.")
                }
            } ?: log("Please select a MAL XML file first.")
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
        runOnUiThread {
            logView.append("$msg\n")
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
