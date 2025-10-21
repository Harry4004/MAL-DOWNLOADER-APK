package com.harry.maldownloader

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // UI elements
    private lateinit var btnLoadXml: Button
    private lateinit var btnAddTag: Button
    private lateinit var btnViewTags: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLogs: TextView
    private lateinit var scrollView: ScrollView

    // Tag storage
    private val animeCustomTags = mutableListOf<String>()
    private val mangaCustomTags = mutableListOf<String>()
    private val hentaiCustomTags = mutableListOf<String>()

    private val prefs by lazy { getSharedPreferences("mal_downloader_prefs", MODE_PRIVATE) }

    // OkHttp client
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init UI references
        btnLoadXml = findViewById(R.id.btnLoadXml)
        btnAddTag = findViewById(R.id.btnAddTag)
        btnViewTags = findViewById(R.id.btnViewTags)
        tvStatus = findViewById(R.id.tvStatus)
        tvLogs = findViewById(R.id.tvLogs)
        scrollView = findViewById(R.id.scrollView)

        loadCustomTagsFromPrefs()
        bindButtons()

        btnLoadXml.setOnClickListener { openFilePicker() }
    }

    // Bind the buttons with click listeners
    private fun bindButtons() {
        btnAddTag.setOnClickListener { showAddTagDialog() }
        btnViewTags.setOnClickListener { showViewTagsDialog() }
    }

    // Open XML picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/xml"
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_XML)
    }

    // Receive selected file
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_XML && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { stream ->
                    parseXmlAndProcess(stream)
                } ?: showToast(getString(R.string.file_open_failed))
            }
        }
    }

    private fun parseXmlAndProcess(xmlInputStream: InputStream) {
        tvStatus.text = getString(R.string.status_parsing)
        tvLogs.text = getString(R.string.log_initialized)
        scope.launch {
            val entries = withContext(Dispatchers.IO) { parseMalXml(xmlInputStream) }
            logMessage(getString(R.string.xml_parsing_complete, entries.size))
            if (entries.isEmpty()) {
                showToast("No entries found in XML.")
            } else {
                tvStatus.text = getString(R.string.starting_api_enrichment)
                processEntries(entries)
            }
        }
    }

    private suspend fun processEntries(entries: List<MediaEntry>) {
        var successCount = 0
        var failedCount = 0

        entries.forEachIndexed { index, entry ->
            updateStatus(getString(R.string.status_processing, index + 1, entries.size, entry.title))
            try {
                val enrichedEntryAndImage = fetchAndProcessMediaEntry(entry)
                if (enrichedEntryAndImage != null) {
                    val (enrichedEntry, imageUrl) = enrichedEntryAndImage
                    if (imageUrl.isNotEmpty()) {
                        downloadCoverAndEmbedXmp(imageUrl, enrichedEntry)
                        successCount++
                        logMessage(getString(R.string.saved_successfully, enrichedEntry.title, enrichedEntry.primaryFolder))
                    } else {
                        logMessage(getString(R.string.no_image_found, enrichedEntry.title))
                        failedCount++
                    }
                } else {
                    failedCount++
                }
            } catch (e: Exception) {
                failedCount++
                logMessage(getString(R.string.failed_processing, entry.title, e.message ?: "Unknown error"))
            }
        }

        tvStatus.text = getString(R.string.status_complete, successCount, failedCount)
        logMessage(getString(R.string.processing_complete))
    }

    private suspend fun fetchAndProcessMediaEntry(entry: MediaEntry): Pair<MediaEntry, String>? = withContext(Dispatchers.IO) {
        val apiUrl = when (entry.type.lowercase()) {
            "anime" -> "https://api.jikan.moe/v4/anime/${entry.id}"
            "manga" -> "https://api.jikan.moe/v4/manga/${entry.id}"
            else -> return@withContext null
        }
        val request = Request.Builder().url(apiUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val jsonString = response.body?.string() ?: return@withContext null
            val jsonObj = JSONObject(jsonString).optJSONObject("data") ?: return@withContext null

            // Extract image URL
            val images = jsonObj.optJSONObject("images")?.optJSONObject("jpg")
            val imageUrl = images?.optString("large_image_url")?.takeIf { it.isNotEmpty() }
                ?: images?.optString("image_url") ?: ""

            // Enrich genres and check hentai
            val genresArray = jsonObj.optJSONArray("genres") ?: return@withContext null
            val genres = mutableListOf<String>()
            for (i in 0 until genresArray.length()) {
                val genreObj = genresArray.optJSONObject(i)
                genreObj?.optString("name")?.let { genres.add(it) }
            }
            entry.genres = genres
            entry.isHentai = genres.any { it.equals("Hentai", true) }

            return@withContext Pair(entry, imageUrl)
        }
    }

    private fun downloadCoverAndEmbedXmp(url: String, entry: MediaEntry) {
        // Your existing code for image download and XMP embedding
        // Ensure you handle IO and concurrency properly here
    }

    private fun parseMalXml(xmlInputStream: InputStream): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlInputStream, null)

            var eventType = parser.eventType
            var currentEntry: MediaEntry? = null
            var text = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("anime", true) || tagName.equals("manga", true)) {
                            currentEntry = MediaEntry()
                            currentEntry.type = tagName.lowercase()
                        }
                    }
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> {
                        currentEntry?.let {
                            when (tagName.lowercase()) {
                                "series_animedb_id", "manga_malid" -> it.id = text.toIntOrNull() ?: 0
                                "series_title", "manga_title" -> it.title = text
                                "my_tags" -> {
                                    if (text.isNotEmpty()) {
                                        it.customTags = text.split(",").map { t -> t.trim() }
                                    }
                                }
                                "anime", "manga" -> entries.add(it)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (ex: Exception) {
            runOnUiThread {
                showToast(getString(R.string.xml_parsing_error, ex.localizedMessage ?: ""))
            }
        }
        return entries
    }

    /**
     * Shows dialog to add a custom tag for chosen category (Anime/Manga/Hentai)
     */
    private fun showAddTagDialog() {
        val types = arrayOf("Anime", "Manga", "Hentai")
        var selected = 0
        val input = android.widget.EditText(this).apply { hint = "e.g., A-Action, M-Colored, H-NTR" }
        AlertDialog.Builder(this)
            .setTitle("Add Custom Tag")
            .setSingleChoiceItems(types, 0) { _, which -> selected = which }
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty()) {
                    when (selected) {
                        0 -> animeCustomTags.add(tag)
                        1 -> mangaCustomTags.add(tag)
                        2 -> hentaiCustomTags.add(tag)
                    }
                    saveCustomTagsToPrefs()
                    showToast("Added tag: $tag")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showViewTagsDialog() {
        val allTags = mutableListOf<String>()
        allTags += animeCustomTags.map { "Anime: $it" }
        allTags += mangaCustomTags.map { "Manga: $it" }
        allTags += hentaiCustomTags.map { "Hentai: $it" }
        if (allTags.isEmpty()) {
            showToast("No custom tags yet")
            return
        }
        val items = allTags.toTypedArray()
        val builder = AlertDialog.Builder(this).setTitle("Custom Tags")
        builder.setItems(items, null)
        builder.setNegativeButton("Close", null)
        val dialog = builder.create()
        dialog.show()

        // Long-press to remove tag support
        val listView = dialog.listView
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val tag = items[position]
            dialog.dismiss()
            showRemoveTagDialog(tag)
            true
        }
    }

    /**
     * Confirm and remove a tagged item from the custom tags lists
     */
    private fun showRemoveTagDialog(tag: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Tag?")
            .setMessage("Are you sure you want to remove $tag?")
            .setPositiveButton("Remove") { dialogInterface, _ ->
                when {
                    tag.startsWith("Anime: ") -> animeCustomTags.remove(tag.removePrefix("Anime: "))
                    tag.startsWith("Manga: ") -> mangaCustomTags.remove(tag.removePrefix("Manga: "))
                    tag.startsWith("Hentai: ") -> hentaiCustomTags.remove(tag.removePrefix("Hentai: "))
                }
                saveCustomTagsToPrefs()
                showToast("Removed: $tag")
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .show()
    }

    private fun saveCustomTagsToPrefs() {
        prefs.edit {
            putStringSet("animeTags", animeCustomTags.toSet())
            putStringSet("mangaTags", mangaCustomTags.toSet())
            putStringSet("hentaiTags", hentaiCustomTags.toSet())
        }
    }

    private fun loadCustomTagsFromPrefs() {
        animeCustomTags.clear()
        mangaCustomTags.clear()
        hentaiCustomTags.clear()
        animeCustomTags.addAll(prefs.getStringSet("animeTags", emptySet()) ?: emptySet())
        mangaCustomTags.addAll(prefs.getStringSet("mangaTags", emptySet()) ?: emptySet())
        hentaiCustomTags.addAll(prefs.getStringSet("hentaiTags", emptySet()) ?: emptySet())
    }

    private fun logMessage(msg: String) {
        tvLogs.append("$msg\n")
        scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    private fun updateStatus(status: String) {
        tvStatus.text = status
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_CODE_PICK_XML = 1001
    }

    data class MediaEntry(
        var id: Int = 0,
        var title: String = "",
        var type: String = "", // "anime" or "manga"
        var genres: List<String> = emptyList(),
        var isHentai: Boolean = false,
        var customTags: List<String> = emptyList()
    )
}
