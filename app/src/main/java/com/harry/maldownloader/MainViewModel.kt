// Add cancellation and retry functions
fun cancelDownload(downloadId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        repository.cancelDownload(downloadId)
    }
}

fun retryDownload(downloadId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        repository.retryDownload(downloadId)
    }
}