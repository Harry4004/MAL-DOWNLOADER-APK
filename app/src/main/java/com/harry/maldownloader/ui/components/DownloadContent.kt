@Composable
fun DownloadListItem(
    downloadItem: DownloadItem,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(downloadItem.fileName, style = MaterialTheme.typography.titleMedium)
                Text(downloadItem.status, style = MaterialTheme.typography.bodyMedium)
            }
            when(downloadItem.status) {
                "downloading" -> {
                    IconButton(onClick = { onCancel(downloadItem.id) }) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                    }
                }
                "failed" -> {
                    IconButton(onClick = { onRetry(downloadItem.id) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsContent(
    downloads: List<DownloadItem>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(downloads) { item ->
            DownloadListItem(item, onCancelDownload, onRetryDownload)
        }
    }
}