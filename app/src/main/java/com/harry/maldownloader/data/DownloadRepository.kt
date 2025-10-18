    suspend fun cancelDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "downloading") {
            downloadDao.updateDownload(download.copy(status = "cancelled"))
            workManager.cancelUniqueWork("download_$downloadId")
            logInfo(downloadId, "Download cancelled")
        }
    }