# MAL Downloader APK v2.0

An advanced Android application that processes MyAnimeList (MAL) XML export files and downloads anime/manga cover images with comprehensive metadata embedding and intelligent folder organization.

## 🌟 Features

### Core Functionality
- **Smart XML Parsing**: Processes MAL XML exports with robust error handling
- **Jikan API Integration**: Enriches data with additional metadata from MyAnimeList
- **Retry Logic**: Intelligent retry system for failed API calls and downloads
- **Rate Limiting**: Respects Jikan API rate limits with automatic backoff

### Advanced Organization
- **Intelligent Folder Structure**: Organizes by content type and genre
  ```
  Pictures/MAL_Export/
  ├── Anime/[Genre]/
  ├── Manga/[Genre]/
  ├── Hentai/Anime/[Genre]/
  └── Hentai/Manga/[Genre]/
  ```
- **Hentai Detection**: Automatic content classification based on genres
- **Custom Tags**: Personalized tagging system (A-Action, M-Romance, H-NTR, etc.)
- **Duplicate Prevention**: Intelligent duplicate detection and skipping

### Metadata & Quality
- **XMP Metadata Embedding**: Rich metadata directly embedded in images
  - MAL ID, title, synopsis, score, status, episodes, year
  - Genres, custom tags, and content ratings
  - Adult content warnings for appropriate content
- **In-Memory Processing**: Efficient XMP embedding without temporary files
- **High-Quality Images**: Downloads highest available image quality

### User Experience
- **Material3 Design**: Modern UI with day/night theme support
- **Real-time Logging**: Detailed progress tracking with emoji indicators
- **Auto-scrolling Logs**: Always see the latest progress
- **Status Updates**: Clear status indicators throughout processing
- **Permission Management**: Proper Android 13+ permission handling

### Technical Excellence
- **Scoped Storage**: Full Android 10+ scoped storage compliance
- **Coroutines**: Efficient async processing with proper cancellation
- **Memory Efficient**: Optimized for low memory usage during processing
- **Error Recovery**: Graceful error handling and user feedback

## 📱 How to Use

1. **Export your data**: Go to MyAnimeList → Export → Download as XML
2. **Install the app**: Download from releases or build from source
3. **Grant permissions**: Allow media access when prompted (Android 13+)
4. **Select XML file**: Tap "Select MyAnimeList XML File" and choose your export
5. **Watch the magic**: Monitor real-time progress as your collection is organized

## 📁 Output Structure

```
Pictures/MAL_Export/
├── Anime/
│   ├── Action/
│   │   ├── attack_on_titan_16498.jpg
│   │   └── demon_slayer_38000.jpg
│   └── Romance/
│       └── your_name_32281.jpg
├── Manga/
│   ├── Comedy/
│   │   └── one_piece_13.jpg
│   └── Drama/
│       └── monster_1.jpg
└── Hentai/
    ├── Anime/
    │   └── Vanilla/
    └── Manga/
        └── NTR/
```

## 🔧 Building

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 34
- Kotlin 1.9.0+

### GitHub Actions (Recommended)
The repository includes automated CI/CD:
- Builds APK on every push
- Runs tests and quality checks
- Creates release artifacts automatically

### Manual Build
```bash
# Clone the repository
git clone https://github.com/Harry4004/MAL-DOWNLOADER-APK.git
cd MAL-DOWNLOADER-APK

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Find APKs in
ls app/build/outputs/apk/
```

## 📋 Requirements

### System Requirements
- **Android 7.0** (API 24) or higher
- **RAM**: 2GB+ recommended for large collections
- **Storage**: Variable based on collection size
- **Network**: Internet connection for API calls

### Permissions
- `INTERNET`: Required for Jikan API calls
- `READ_MEDIA_IMAGES`: Android 13+ media access
- `READ_EXTERNAL_STORAGE`: Android 12 and below (legacy)
- `WRITE_EXTERNAL_STORAGE`: Android 9 and below (legacy)

## 🏗️ Technical Architecture

### Core Technologies
- **Language**: Kotlin 100%
- **UI Framework**: Android Views + Material3 (Compose ready)
- **Architecture**: MVVM with coroutines
- **Build System**: Gradle with Kotlin DSL
- **Min API**: 24 (Android 7.0)
- **Target API**: 34 (Android 14)

### Key Libraries
- **Networking**: OkHttp 4.12.0 + Retrofit 2.9.0
- **JSON**: Moshi + native org.json
- **Coroutines**: Kotlinx Coroutines 1.7.3
- **XML Processing**: Android XmlPullParser
- **Metadata**: Custom XMP implementation
- **Storage**: MediaStore API with scoped storage

### Performance Features
- **Serial Queue**: Prevents storage conflicts
- **Memory Management**: Efficient bitmap processing
- **Rate Limiting**: Respects API constraints
- **Cancellation**: Proper coroutine lifecycle management
- **Background Processing**: Non-blocking UI operations

## 🔄 Version History

### v2.0 (Current)
- ✅ Complete rewrite with modern Android practices
- ✅ Enhanced folder organization with genre support
- ✅ Retry logic for API calls and downloads
- ✅ In-memory XMP metadata embedding
- ✅ Proper Android 13+ permission handling
- ✅ Material3 design with day/night themes
- ✅ Real-time progress tracking
- ✅ Intelligent hentai content detection
- ✅ Duplicate prevention system

### v1.0 (Legacy)
- Basic XML parsing and image downloading
- Simple folder structure
- Limited error handling
- Basic EXIF metadata

## 📄 License & Disclaimer

This project is intended for personal use only. Please respect:
- **MyAnimeList Terms of Service**
- **Jikan API rate limits** (1 request per second)
- **Copyright laws** regarding downloaded content
- **Privacy** - all processing happens locally on your device

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Follow Kotlin coding standards
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For issues, feature requests, or questions:
- **GitHub Issues**: [Create an issue](https://github.com/Harry4004/MAL-DOWNLOADER-APK/issues)
- **Discussions**: Use GitHub Discussions for general questions

---

**Developed with ❤️ by Harry4004**

*Last updated: October 2025*