# MAL Downloader APK v3.0 🚀

A professional Android application that transforms MyAnimeList (MAL) XML exports into beautifully organized image collections with 25+ dynamic metadata tags and intelligent content management.

## ✨ Latest Features (v3.0)

### 🎯 25+ Dynamic Metadata Tags
- **Advanced API Integration**: Enhanced Jikan API enrichment
- **Smart Content Detection**: Automatic hentai/adult content classification
- **Dynamic Tag System**: 25+ metadata tags per entry including:
  - Genres, Studios, Demographics, Themes
  - Rating, Status, Episodes, Duration, Year
  - Custom user tags with personalized management
  - Content warnings and age ratings

### 🖥️ Enhanced User Experience
- **Professional Loading Screen**: No more black screen issues
- **Material3 Design**: Modern UI with smooth animations
- **Real-time Progress Tracking**: Live updates during processing
- **Error Recovery System**: Comprehensive error handling with troubleshooting
- **Background Processing**: Non-blocking initialization

### 🛡️ Reliability & Performance
- **Database v3**: Enhanced Room database with migration support
- **Crash Recovery**: Global exception handling with logging
- **Memory Optimization**: Efficient processing for large collections
- **Network Resilience**: Smart retry logic with exponential backoff

## 🌟 Core Features

### Smart Organization System
- **Intelligent Folder Structure**: Content-aware organization
  ```
  Pictures/MAL_Export/
  ├── Anime/
  │   ├── Action/ (Attack on Titan, etc.)
  │   ├── Romance/ (Your Name, etc.)
  │   └── Comedy/ (One Piece, etc.)
  ├── Manga/
  │   ├── Drama/ (Monster, etc.)
  │   └── Thriller/ (Death Note, etc.)
  └── Hentai/ (Age-appropriate organization)
      ├── Anime/[Genres]/
      └── Manga/[Genres]/
  ```

### Advanced Metadata System
- **XMP Metadata Embedding**: Professional-grade metadata directly in images
- **AVES Gallery Compatible**: Works seamlessly with photo management apps
- **Rich Information**: Title, synopsis, score, status, episodes, year, genres
- **Custom Tags**: Personalized tagging system (A-Action, R-Romance, etc.)
- **Content Ratings**: Automatic adult content detection and labeling

### Modern Android Integration
- **Android 14 Ready**: Full compatibility with latest Android versions
- **Scoped Storage**: Proper modern storage handling
- **Permission Management**: Smart permission requests for media access
- **Notification System**: Progress notifications with download channels

## 🚀 Getting Started

### Quick Setup
1. **Export MAL Data**: MyAnimeList → Settings → Export → Download XML
2. **Install App**: Download from GitHub releases or build from source
3. **Grant Permissions**: Allow media access when prompted
4. **Import & Process**: Select XML file and watch the magic happen!

### First Launch Experience
- 🚀 Professional loading screen with initialization progress
- ⚡ Background database setup (no blocking)
- 🎯 Ready-to-use interface with all features available
- 📱 Smooth Material3 animations throughout

## 📱 User Interface

### Main Features Tabs
1. **🎥 Import**: XML processing with API enrichment
2. **📁 Entries**: Browse processed anime/manga with statistics
3. **⬇️ Downloads**: Monitor active and completed downloads
4. **📝 Logs**: Real-time processing logs with error tracking

### Smart Features
- **Tag Manager**: Create and manage custom tags
- **Progress Tracking**: Real-time status updates
- **Error Recovery**: Detailed error messages with solutions
- **Statistics Dashboard**: Collection insights and metrics

## 🔧 Technical Specifications

### Architecture
- **Language**: Kotlin 100% (modern Android development)
- **UI Framework**: Jetpack Compose (Material3 Design)
- **Architecture Pattern**: MVVM with Repository pattern
- **Database**: Room 2.6.1 with migrations
- **Async Processing**: Kotlin Coroutines with proper lifecycle management

### Core Dependencies
```kotlin
// Networking & API
OkHttp 4.12.0 + Retrofit 2.9.0 + Moshi
Jikan API integration with rate limiting

// Database & Storage
Room 2.6.1 with TypeConverters
DataStore for preferences
Scoped Storage compliance

// UI & UX
Jetpack Compose with Material3
Coil for image loading
Work Manager for background tasks

// Quality & Testing
JUnit, Espresso, MockK
Proguard optimization
Crash reporting integration
```

### Performance Features
- **Memory Efficient**: Optimized bitmap processing
- **Rate Limited**: Respects Jikan API constraints (1 req/sec)
- **Background Processing**: Non-blocking UI operations
- **Smart Caching**: Efficient data management
- **Error Recovery**: Graceful failure handling

## 📋 System Requirements

### Minimum Requirements
- **Android 7.0** (API 24) or higher
- **RAM**: 2GB+ (4GB recommended for large collections)
- **Storage**: Variable (depends on collection size)
- **Network**: Stable internet for API enrichment

### Optimal Performance
- **Android 12+** for best scoped storage experience
- **4GB+ RAM** for processing 1000+ entries
- **Fast internet** for quick API enrichment
- **Modern device** for smooth animations

### Required Permissions
- `INTERNET`: Jikan API calls and image downloads
- `READ_MEDIA_IMAGES`: Android 13+ media access
- `POST_NOTIFICATIONS`: Download progress notifications
- `READ_EXTERNAL_STORAGE`: Legacy Android versions

## 🏗️ Building from Source

### Prerequisites
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or higher
- **Android SDK**: 34 (Android 14)
- **Gradle**: 8.0+ with Kotlin DSL

### Build Commands
```bash
# Clone repository
git clone https://github.com/Harry4004/MAL-DOWNLOADER-APK.git
cd MAL-DOWNLOADER-APK

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Find APKs
ls app/build/outputs/apk/
```

### GitHub Actions CI/CD
- ✅ Automated builds on every push
- ✅ Quality checks and testing
- ✅ Release artifact generation
- ✅ Code scanning and security analysis

## 🔄 Version History & Changelog

### v3.0 (Current - October 2025)
- 🚀 **Major UI Overhaul**: Complete Jetpack Compose migration
- 🎯 **25+ Dynamic Tags**: Enhanced metadata system with custom tags
- 🛡️ **Black Screen Fix**: Professional loading screen and error recovery
- 📊 **Database v3**: Enhanced Room database with better migrations
- 🔧 **Background Processing**: Non-blocking initialization and processing
- 🎨 **Material3 Design**: Modern UI with smooth animations
- 📱 **Android 14 Ready**: Full compatibility with latest Android
- 🔔 **Notification Channels**: Enhanced download progress notifications
- 🏷️ **Tag Management**: Advanced custom tag system
- 📈 **Statistics**: Collection insights and download metrics

### v2.0 (Legacy)
- ✅ Modern Android practices implementation
- ✅ Enhanced folder organization
- ✅ Retry logic and error handling
- ✅ XMP metadata embedding
- ✅ Material3 theming

### v1.0 (Original)
- Basic XML parsing and downloading
- Simple folder structure
- Limited error handling

## 🛠️ Troubleshooting

### Common Issues & Solutions

#### Black Screen on Startup
- ✅ **Fixed in v3.0**: Now shows professional loading screen
- 🔧 If issues persist: Clear app data and restart

#### Permission Errors
- 📱 Grant media permissions when prompted
- ⚙️ Check Settings → Apps → MAL Downloader → Permissions

#### API Rate Limiting
- ⏱️ App automatically handles Jikan API limits
- 🔄 Retry logic built-in for failed requests

#### Database Issues
- 🗄️ v3.0 includes automatic migration from older versions
- 🔧 Fallback to destructive migration if needed

## 🤝 Contributing

### How to Contribute
1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Follow** Kotlin coding standards and Material3 guidelines
4. **Test** thoroughly on different Android versions
5. **Submit** a pull request with detailed description

### Development Guidelines
- Use Kotlin coroutines for async operations
- Follow MVVM architecture patterns
- Implement proper error handling
- Add comprehensive logging
- Test on Android 7.0+ devices
- Follow Material3 design principles

### Code Quality
- ✅ Kotlin coding conventions
- ✅ Proper null safety
- ✅ Comprehensive error handling
- ✅ Memory leak prevention
- ✅ Performance optimization

## 📄 License & Legal

### Usage Guidelines
- ✅ **Personal Use**: Freely use for your own MAL collection
- ⚠️ **API Respect**: Honor Jikan API rate limits and terms
- 🔒 **Privacy**: All processing happens locally on your device
- 📋 **Content**: Respect copyright and MyAnimeList terms of service

### Data Privacy
- 🔐 **Local Processing**: No data sent to external servers (except API calls)
- 📱 **Device Storage**: All files stored locally on your device
- 🛡️ **No Tracking**: No analytics or user tracking implemented
- 🔓 **Open Source**: Full transparency with public source code

## 🌟 Acknowledgments

### Special Thanks
- **MyAnimeList**: For the amazing platform and data export feature
- **Jikan API**: For providing excellent MAL API access
- **Android Team**: For modern development tools and libraries
- **Community**: For feedback, bug reports, and feature suggestions

### Technology Credits
- **Jetpack Compose**: Modern Android UI toolkit
- **Material3**: Beautiful design system
- **Room Database**: Robust local storage
- **Kotlin Coroutines**: Elegant async programming

## 📞 Support & Community

### Get Help
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/Harry4004/MAL-DOWNLOADER-APK/issues)
- 💡 **Feature Requests**: GitHub Issues with enhancement label
- 💬 **General Questions**: GitHub Discussions
- 📚 **Documentation**: This README and code comments

### Stay Updated
- ⭐ **Star the repository** to show support
- 👁️ **Watch** for notifications on new releases
- 🍴 **Fork** to contribute or customize

---

<div align="center">

**🎯 Developed with ❤️ by Harry4004**

*Professional MAL collection management for Android*

**📅 Last Updated: October 22, 2025**

![GitHub stars](https://img.shields.io/github/stars/Harry4004/MAL-DOWNLOADER-APK?style=social)
![GitHub forks](https://img.shields.io/github/forks/Harry4004/MAL-DOWNLOADER-APK?style=social)
![GitHub issues](https://img.shields.io/github/issues/Harry4004/MAL-DOWNLOADER-APK)
![Android](https://img.shields.io/badge/Android-7.0+-green)
![Kotlin](https://img.shields.io/badge/Kotlin-100%-purple)
![Material3](https://img.shields.io/badge/Material3-UI-blue)

</div>