package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.data.DownloadItem

@Composable
fun StatisticsContent(
    entries: List<AnimeEntry>,
    downloads: List<DownloadItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatisticsOverviewCard(entries, downloads)
        }
        
        item {
            LibraryStatsCard(entries)
        }
        
        item {
            DownloadStatsCard(downloads)
        }
        
        item {
            TypeBreakdownCard(entries)
        }
        
        item {
            StatusBreakdownCard(entries)
        }
    }
}

@Composable
fun StatisticsOverviewCard(
    entries: List<AnimeEntry>,
    downloads: List<DownloadItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewStat(
                    title = "Total Entries",
                    value = entries.size.toString(),
                    icon = Icons.Filled.LibraryBooks
                )
                
                OverviewStat(
                    title = "Downloads",
                    value = downloads.size.toString(),
                    icon = Icons.Filled.Download
                )
                
                OverviewStat(
                    title = "Completed",
                    value = downloads.count { it.status == "completed" }.toString(),
                    icon = Icons.Filled.CheckCircle
                )
            }
        }
    }
}

@Composable
fun OverviewStat(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun LibraryStatsCard(entries: List<AnimeEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Library Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val avgScore = entries.mapNotNull { it.score.takeIf { score -> score > 0 } }.average()
            val totalEpisodes = entries.sumOf { it.episodesWatched }
            
            StatRow("Average Score", if (avgScore.isNaN()) "N/A" else "%.1f".format(avgScore))
            StatRow("Total Episodes", totalEpisodes.toString())
            StatRow("Unique Titles", entries.size.toString())
            StatRow("With Images", entries.count { !it.imageUrl.isNullOrEmpty() }.toString())
        }
    }
}

@Composable
fun DownloadStatsCard(downloads: List<DownloadItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Download Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatRow("Total Downloads", downloads.size.toString())
            StatRow("Completed", downloads.count { it.status == "completed" }.toString())
            StatRow("Failed", downloads.count { it.status == "failed" }.toString())
            StatRow("In Progress", downloads.count { it.status == "downloading" }.toString())
            StatRow("Pending", downloads.count { it.status == "pending" }.toString())
        }
    }
}

@Composable
fun TypeBreakdownCard(entries: List<AnimeEntry>) {
    val typeGroups = entries.groupBy { it.type }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Type Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            typeGroups.forEach { (type, items) ->
                StatRow(type, items.size.toString())
            }
        }
    }
}

@Composable
fun StatusBreakdownCard(entries: List<AnimeEntry>) {
    val statusGroups = entries.groupBy { it.status ?: "Unknown" }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Status Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            statusGroups.forEach { (status, items) ->
                StatRow(status, items.size.toString())
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
