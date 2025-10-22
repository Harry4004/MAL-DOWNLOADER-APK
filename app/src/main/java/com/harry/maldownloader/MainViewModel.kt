    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 Starting MAL file processing with Client ID: ${MainApplication.MAL_CLIENT_ID.take(8)}...")
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
            log("📝 Parsed entries: ${entries.size}, URI: $uri")
            if (entries.isEmpty()) {
                log("❌ No entries found. The file content may be unreadable or not a valid MAL XML export.")
                // Optionally: Show a user-visible Snackbar/toast here
                return
            }
            _animeEntries.value = entries

            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    val enriched = enrichWithBestAvailableApi(entry)
                    enriched?.let {
                        val list = _animeEntries.value.toMutableList()
                        val idx = list.indexOfFirst { it.malId == entry.malId }
                        if (idx != -1) list[idx] = it else list.add(it)
                        _animeEntries.value = list
                        downloadImages(it)
                    }
                    delay(1200)
                } catch (e: Exception) {
                    log("❌ Failed ${entry.title}: ${e.message}")
                }
            }
            log("🎉 Completed")
        } finally { _isProcessing.value = false }
    }