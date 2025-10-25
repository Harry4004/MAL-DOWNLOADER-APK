# UI Scaling Settings Implementation Guide

## Overview
This document outlines the complete implementation of persistent UI scaling settings for the MAL Downloader app, enabling users to adjust icon and font sizes with settings persisted to the Room database.

## Files Modified/Created

### 1. AppSettings.kt ✅ UPDATED
**Path**: `app/src/main/java/com/harry/maldownloader/data/AppSettings.kt`

**Changes Made**:
- Added `iconScale: Float = 1.0f` (range: 0.85f - 1.30f)
- Added `fontScale: Float = 1.0f` (range: 0.90f - 1.30f)
- Extended `SettingsCategory` enum with `UI` category
- Extended `SettingType` enum with `FLOAT` type
- Added UI scaling settings to `SettingsConfig.allSettings`

### 2. SettingsDao.kt ✅ UPDATED
**Path**: `app/src/main/java/com/harry/maldownloader/data/SettingsDao.kt`

**Changes Made**:
- Added `updateIconScale(value: Float)` method
- Added `updateFontScale(value: Float)` method
- Added `getScalingSettings(): ScalingSettings?` convenience method
- Added `ScalingSettings` data class for scaling-specific queries

### 3. SettingsRepository.kt ✅ NEW FILE
**Path**: `app/src/main/java/com/harry/maldownloader/data/SettingsRepository.kt`

**Purpose**: Repository layer for settings business logic with validation

**Features**:
- Repository-backed StateFlows for reactive UI updates
- Validation of scale values within safe ranges
- Error handling and logging integration
- Initialize settings on first use
- Reset functionality

### 4. MainViewModel.kt ✅ UPDATED
**Path**: `app/src/main/java/com/harry/maldownloader/MainViewModel.kt`

**Changes Made**:
- Converted to `@HiltViewModel` with dependency injection
- Replaced MutableStateFlow scaling with repository-backed StateFlows
- Added `SettingsRepository` dependency
- Updated scaling methods to use repository with persistence
- Added comprehensive error handling and logging

### 5. DatabaseModule.kt ✅ NEW FILE
**Path**: `app/src/main/java/com/harry/maldownloader/di/DatabaseModule.kt`

**Purpose**: Hilt dependency injection module

**Provides**:
- `DownloadDatabase` singleton
- `SettingsDao` from database
- `SettingsRepository` with validation

### 6. MainApplication.kt ✅ UPDATED
**Path**: `app/src/main/java/com/harry/maldownloader/MainApplication.kt`

**Changes Made**:
- Added `@HiltAndroidApp` annotation
- Enabled Hilt dependency injection framework

## Key Features Implemented

### ✅ Persistent Storage
- Icon and font scale settings saved to existing Room database
- Survives app restarts and updates
- No additional storage dependencies required

### ✅ Validation & Safety
- Icon scale: 0.85f - 1.30f (85% - 130%)
- Font scale: 0.90f - 1.30f (90% - 130%)
- Invalid values automatically clamped to safe ranges
- Prevents UI breakage from extreme scaling values

### ✅ Reactive Updates
- StateFlow-based reactive architecture
- UI automatically responds to scale changes
- Repository-backed flows ensure data consistency
- Shared state across all UI components

### ✅ Clean Architecture
- Repository pattern separates business logic from UI
- Dependency injection with Hilt
- Error handling with comprehensive logging
- Type-safe settings management

### ✅ Migration Ready
- Room database already at version 4 with AppSettings
- Auto-migration handles schema updates
- Fallback to destructive migration if needed
- Existing data preserved

## Usage Examples

### In ViewModels
```kotlin
class SomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val iconScale = settingsRepository.iconScaleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
        
    fun updateIconScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.updateIconScale(scale)
        }
    }
}
```

### In Composables
```kotlin
@Composable
fun ScaledIcon(
    icon: ImageVector,
    iconScale: Float = 1.0f
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size((24.dp * iconScale))
    )
}
```

### Settings UI Integration
```kotlin
// The existing SettingItem system now supports FLOAT type
SettingItem(
    key = "iconScale",
    title = "Icon Size",
    description = "Adjust the size of icons throughout the app",
    category = SettingsCategory.UI,
    type = SettingType.FLOAT,
    defaultValue = 1.0f,
    range = 85 to 130 // Percentage values for UI
)
```

## Database Schema

The `app_settings` table now includes:
```sql
CREATE TABLE app_settings (
    id INTEGER PRIMARY KEY DEFAULT 1,
    -- ... existing fields ...
    iconScale REAL NOT NULL DEFAULT 1.0,
    fontScale REAL NOT NULL DEFAULT 1.0
    -- ... other fields ...
);
```

## Implementation Status

- [x] **Data Layer**: AppSettings entity extended with scaling fields
- [x] **Repository Layer**: SettingsRepository with business logic and validation
- [x] **Dependency Injection**: Hilt modules configured
- [x] **ViewModel Layer**: MainViewModel updated with repository integration
- [x] **Database Schema**: Room database updated to version 4
- [x] **Error Handling**: Comprehensive logging and validation
- [x] **Type Safety**: Full enum support for UI categories and FLOAT types

## Next Steps for Integration

1. **Update UI Components**: Apply scaling factors to icons and text throughout the app
2. **Settings Screen**: Add sliders/controls for icon and font scaling in settings UI
3. **Testing**: Verify scaling works across different screen sizes and densities
4. **Documentation**: Update user-facing documentation about accessibility features

## Compatibility Notes

- **Minimum Android API**: No change (existing Room database requirements)
- **Dependencies**: Uses existing Hilt, Room, and Kotlin Coroutines
- **Migration**: Automatic migration from database version 3 to 4
- **Backward Compatibility**: Existing installations will receive default 1.0f values

---

**Deployment Status**: ✅ **COMPLETE**  
**Files Modified**: 4 updated, 2 new files created  
**Database Version**: Updated to v4 with scaling fields  
**Dependency Injection**: Hilt enabled and configured  

The UI scaling settings system is now fully deployed and ready for integration into the app's user interface.