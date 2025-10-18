suspend fun fetchAndCacheImage(entry: AnimeEntry, context: Context): AnimeEntry {
    val imageDownloader = ImageDownloader(context, this)
    val updatedEntry = imageDownloader.downloadImageForEntry(entry)
    // Optionally persist updatedEntry with local image path
    return updatedEntry
}