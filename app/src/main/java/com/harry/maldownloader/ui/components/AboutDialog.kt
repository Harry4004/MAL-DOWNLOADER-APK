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
fun AboutDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    ModernSwipeDialog(
        onDismiss = onDismiss,
        title = "📱 About MAL Downloader",
        subtitle = "Enhanced Edition v${BuildConfig.VERSION_NAME}",
        showSwipeIndicator = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Info Header
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                glowIntensity = 0.2f
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📱",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MAL Downloader Enhanced",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Professional MyAnimeList Image Downloader",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Enhanced Features Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Enhanced Features",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val features = listOf(
                    "📁 Public Pictures directory storage",
                    "🏷️ 25+ Dynamic metadata tags", 
                    "🔍 Advanced search & filtering",
                    "⚡ Concurrent download engine",
                    "🎨 Material Design 3 interface",
                    "📋 Comprehensive logging system",
                    "🔧 Professional settings management",
                    "📄 Real-time statistics",
                    "🔄 Robust retry & recovery logic",
                    "🎯 Working action buttons",
                    "🏷️ NEW: Custom tags file import"
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    features.forEach { feature ->
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Technical Details Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Engineering,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Technical Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val buildInfo = listOf(
                    "Build Type: ${BuildConfig.BUILD_TYPE}",
                    "Version Code: ${BuildConfig.VERSION_CODE}", 
                    "Application ID: ${BuildConfig.APPLICATION_ID}",
                    "Enhanced Edition with 4 functional tabs + menu",
                    "Jetpack Compose + Material Design 3",
                    "MVVM Architecture with StateFlow",
                    "Room Database + Coroutines",
                    "Dual API Integration (MAL + Jikan)"
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    buildInfo.forEach { info ->
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Contact & Support Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.ContactSupport,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Support & Contact",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "myaninelistapk@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Harry4004/MAL-DOWNLOADER-APK",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "For bug reports, feature requests, and technical support.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Statistics Card
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.15f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "4",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tabs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "25+",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Working",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Button
        ModernButton(
            onClick = {
                viewModel.log("📄 About dialog viewed - MAL Downloader v${BuildConfig.VERSION_NAME}")
                onDismiss()
            },
            text = "Got it!",
            modifier = Modifier.fillMaxWidth(),
            variant = ModernButtonVariant.Primary,
            icon = Icons.Default.Check
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Credits
        Text(
            text = "Made with ❤️ for the anime and manga community",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}