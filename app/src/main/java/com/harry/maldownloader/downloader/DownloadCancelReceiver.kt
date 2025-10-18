package com.harry.maldownloader.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getStringExtra("downloadId")
        if (!downloadId.isNullOrBlank()) {
            WorkManager.getInstance(context).cancelUniqueWork("download_$downloadId")
        }
    }
}
