package com.harry.maldownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.harry.maldownloader.ui.AppUI
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val pickMalFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            lifecycleScope.launch {
                val tmp = FileUtil.copyUriToTempFile(this@MainActivity, uri, "mal_input.xml")
                if (tmp != null) {
                    vm.onMalFilePicked(tmp.absolutePath, getBaseOutputDir())
                } else {
                    vm.appendLog("Failed to read selected file")
                }
            }
        } else {
            vm.appendLog("No file selected")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            vm.appendLog("Storage permission denied; using picker access only")
        }
    }

    private fun requestReadPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT <= 32) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestReadPermissionIfNeeded()

        setContent {
            AppUI(
                state = vm.uiState,
                onPickMalClick = { openMalPicker() },
                onCustomTagsChanged = vm::setCustomTags,
                onClearLogs = vm::clearLogs
            )
        }
    }

    private fun openMalPicker() {
        pickMalFileLauncher.launch(arrayOf("text/xml", "application/xml"))
    }

    private fun getBaseOutputDir(): File {
        // Pictures/MAL_Export inside app external files area; gallery apps can index it
        val pics = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val root = File(pics, "MAL_Export")
        if (!root.exists()) root.mkdirs()
        return root
    }
}