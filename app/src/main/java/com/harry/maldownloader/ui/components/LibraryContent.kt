@Composable
fun LibraryItem(entry: AnimeEntry, onDownloadImage: (AnimeEntry) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.imagePath ?: entry.imageUrl,
                contentDescription = entry.title,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleLarge)
                Text(entry.status ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { onDownloadImage(entry) }) {
                Icon(Icons.Filled.Download, contentDescription = "Download image")
            }
        }
    }
}

@Composable
fun LibraryContent(
    entries: List<AnimeEntry>,
    onDownloadImages: (AnimeEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(entries) { entry ->
            LibraryItem(entry = entry, onDownloadImage = onDownloadImages)
        }
    }
}