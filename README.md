# MAL Downloader APK

An Android app that processes MyAnimeList (MAL) XML export files and downloads anime/manga cover images with metadata embedding.

## Features

- **Dark UI**: Material3 dark theme with black/grey tones and colored logs
- **File Picker**: Select MAL XML export files using Android's document picker
- **Custom Tags**: Add personalized tags (H-NTR, M-Colored, A-Harem, etc.)
- **Local Processing**: All operations happen on-device, no cloud storage
- **Image Download**: Fetches cover images with retry mechanism for failed downloads
- **Placeholder Generation**: Creates fallback images when downloads fail
- **Jikan API Integration**: Enriches missing data using the Jikan API
- **EXIF Metadata**: Embeds titles, descriptions, and tags into image EXIF data
- **Organized Storage**: Saves files to `Pictures/MAL_Export/[SeriesName]/` folders

## How to Use

1. Export your anime/manga list from MyAnimeList as XML
2. Open the MAL Downloader app
3. Tap "Pick MAL XML File" and select your exported file
4. Customize tags in the text field (comma-separated)
5. Watch the processing logs as the app downloads and organizes your collection

## Output Structure

```
Pictures/MAL_Export/
├── Anime Series 1/
│   ├── poster.jpg (with EXIF metadata)
│   └── meta.json
├── Anime Series 2/
│   ├── poster.jpg
│   └── meta.json
└── ...
```

## Building

### GitHub Actions (Automatic)

The repository includes a GitHub Actions workflow that automatically builds the APK on every push to the main branch. The built APK is available as a downloadable artifact.

### Manual Build

1. Clone the repository
2. Open in Android Studio or use command line:
   ```bash
   ./gradlew assembleDebug
   ```
3. Find the APK in `app/build/outputs/apk/debug/`

## Requirements

- Android 7.0 (API 24) or higher
- Internet permission for Jikan API calls
- Storage access for saving files

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material3
- **Architecture**: MVVM with ViewModel
- **Networking**: Retrofit + OkHttp + Moshi
- **Image Processing**: Android Bitmap + ExifInterface
- **XML Parsing**: Android XmlPullParser
- **Build System**: Gradle with Kotlin DSL

## License

This project is for personal use. Respect MyAnimeList's terms of service and API rate limits.