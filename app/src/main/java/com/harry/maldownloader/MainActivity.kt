package com.harry.maldownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var selectFileBtn: Button
    private lateinit var downloadBtn: Button
    private var selectedFileUri: Uri? = null
    private lateinit var malPipeline: MalPipeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        selectFileBtn = findViewById(R.id.selectFileBtn)
        downloadBtn = findViewById(R.id.downloadBtn)
        malPipeline = MalPipeline(this) { log(it) }

        checkPermissions()

        val filePicker =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    selectedFileUri = it
                    val fileName = getFileNameFromUri(it)
                    log("Selected file: $fileName")
                }
            }

        selectFileBtn.setOnClickListener {
            filePicker.launch("*/*")
        }

        downloadBtn.setOnClickListener {
            selectedFileUri?.let { uri ->
                lifecycleScope.launch {
                    log("Parsing MAL XML file...")
                    malPipeline.processMalFile(uri)
                    log("Done.")
                }
            } ?: log("Please select a MAL XML file first.")
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
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
        logView.append("$msg\n")
    }
}
