package com.harry.maldownloader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MalDownloaderApp() {
    val tabs = listOf("Library", "Queue", "Logs", "Stats")
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MAL Downloader") }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }
            when (selectedTab) {
                0 -> LibraryTab()
                1 -> QueueTab()
                2 -> LogsTab()
                3 -> StatsTab()
            }
        }
    }
}

@Composable
fun LibraryTab() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        var searchQuery by remember { mutableStateOf("") }
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .height(40.dp),
                ) {
                    if (searchQuery.isEmpty()) Text("Search...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    inner()
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // TODO: Replace with real filtered list
            items(20) { index ->
                Card(Modifier.padding(8.dp).fillMaxWidth().height(150.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Item #$index")
                    }
                }
            }
        }
    }
}

@Composable
fun QueueTab() {
    // TODO: Display download queue and progress with cancel option
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Download queue will appear here")
    }
}

@Composable
fun LogsTab() {
    // TODO: Display real-time logs with timestamps
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Application logs will appear here")
    }
}

@Composable
fun StatsTab() {
    // TODO: Show parsing stats and collection overview
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Statistics will be shown here")
    }
}
