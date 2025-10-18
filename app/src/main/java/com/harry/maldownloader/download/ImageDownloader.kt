suspend fun fetchAndCacheImage(entry: AnimeEntry, context: Context): AnimeEntry = withContext(Dispatchers.IO) {
    val imageDownloader = ImageDownloader(context)
    val result = imageDownloader.downloadImageForEntry(entry)
    downloadDao.insertDownload(
        DownloadItem(
            id = result.seriesId.toString(),
            url = result.imagePath ?: "",
            fileName = result.title ?: "Unknown",
            status = "completed",
            progress = 100
        )
    )
    logInfo(result.seriesId.toString(), "Cached image for ${result.title}")
    result
}

