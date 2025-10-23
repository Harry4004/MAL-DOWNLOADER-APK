package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.BuildConfig
import com.harry.maldownloader.MainViewModel

@Composable
fun AboutScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📱", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("MAL Downloader Enhanced", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.APP_VERSION}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Professional MyAnimeList Image Downloader", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Moved full feature list here from Import tab
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("✨ Enhanced Features v${BuildConfig.APP_VERSION}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val features = listOf(
                    "✅ Public Pictures directory storage (gallery visible)",
                    "✅ 25+ Dynamic tags from dual MAL+Jikan API integration",
                    "✅ XMP metadata embedding (AVES Gallery compatible)",
                    "✅ Enhanced search, filter & sort functionality",
                    "✅ Working action buttons with context menus",
                    "✅ Adult content auto-detection & separation",
                    "✅ Real-time progress with comprehensive logging",
                    "✅ Professional error handling & recovery",
                    "✅ Custom tag management",
                    "✅ Custom tags file import support (XML format)"
                )
                features.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🛠️ Technical Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val buildInfo = listOf(
                    "Build Type: ${BuildConfig.BUILD_TYPE}",
                    "Version Code: ${BuildConfig.VERSION_CODE}",
                    "Application ID: ${BuildConfig.APPLICATION_ID}",
                    "Enhanced Edition with 6 functional tabs",
                    "Jetpack Compose + Material Design 3",
                    "MVVM Architecture with StateFlow",
                    "Room Database + Coroutines"
                )
                buildInfo.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 1.dp)) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📧 Support & Contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Email: myaninelistapk@gmail.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text("GitHub: Harry4004/MAL-DOWNLOADER-APK", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("For bug reports, feature requests, and technical support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { viewModel.log("📊 About screen viewed - MAL Downloader v${BuildConfig.APP_VERSION}"); if (onDismiss != {}) onDismiss() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Got it")
        }

        Spacer(Modifier.height(16.dp))
        Text("Made with ❤️ for the anime and manga community", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
