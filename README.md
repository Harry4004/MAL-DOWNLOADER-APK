# 📱 MAL Downloader v3.1 - Enhanced Edition

> **Advanced MyAnimeList Image Downloader with XMP Metadata & 25+ Dynamic Tags**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🚀 **What is MAL Downloader?**

MAL Downloader is a powerful Android application that automatically downloads high-quality images from your MyAnimeList (MAL) collection and enriches them with **comprehensive metadata** for seamless integration with gallery apps like **AVES Gallery**.

### ✨ **Key Features**

- 📥 **Robust Image Downloading** - Multi-source download with retry logic
- 🏷️ **25+ Dynamic Tags** - Automatic tagging from MAL & Jikan APIs
- 🎯 **XMP Metadata Embedding** - Full AVES Gallery compatibility
- 🔐 **Official MAL API** - Premium data quality with Client ID authentication
- 🌐 **Jikan API Fallback** - Comprehensive backup data source
- 📁 **Smart Organization** - Auto-categorized folder structure (Anime/Manga/Adult)
- 🎭 **NSFW Detection** - Automatic adult content identification and tagging
- ⚡ **Rate Limited** - Respectful API usage with intelligent delays
- 🔄 **Resume Support** - Skip already downloaded images
- 📊 **Real-time Progress** - Live download status and comprehensive logging

---

## 📱 **Screenshots**

### Main Interface
- **Import Tab**: MAL XML file processing with API enrichment
- **Entries Tab**: View parsed entries with statistics and tag counts
- **Downloads Tab**: Monitor download progress and status
- **Logs Tab**: Real-time processing logs with color-coded messages

---

## 🛠️ **Technical Architecture**

### **Built With**
- **🏗️ Architecture**: MVVM with Jetpack Compose
- **🎨 UI Framework**: Material Design 3 + Jetpack Compose
- **🌐 Networking**: Retrofit + OkHttp + Moshi JSON
- **💾 Database**: Room with KSP code generation
- **🔄 Async**: Kotlin Coroutines + StateFlow
- **📁 Storage**: Scoped Storage + SAF (Storage Access Framework)
- **🏷️ Metadata**: EXIF + XMP for gallery compatibility

### **API Integration**

#### **MyAnimeList Official API**
- **Authentication**: OAuth 2.0 Client ID
- **Endpoints**: `/anime/{id}` and `/manga/{id}`
- **Data Quality**: Premium metadata with comprehensive details
- **Rate Limits**: Respected with intelligent delays

#### **Jikan API (Unofficial)**
- **Fallback Source**: When MAL API fails or is unavailable
- **Endpoints**: `/anime/{id}/full` and `/manga/{id}/full`
- **Data Richness**: Extended information including themes, demographics
- **Free Access**: No authentication required

---

## 📋 **Installation & Setup**

### **Prerequisites**
- Android 7.0+ (API Level 24+)
- Internet connection for API calls and image downloads
- Storage permissions for saving images
- MAL Client ID (optional but recommended for premium features)

### **Installation Steps**

1. **Download APK**
   ```bash
   # From GitHub Releases
   wget https://github.com/Harry4004/MAL-DOWNLOADER-APK/releases/latest/download/mal-downloader-v3.1.apk
   
   # Install via ADB
   adb install mal-downloader-v3.1.apk
   ```

2. **Grant Permissions**
   - Storage access for image saving
   - Notification permissions for download status
   - Document access for MAL XML import

3. **Setup MAL Client ID** (Optional)
   - Visit [MAL API Registration](https://myanimelist.net/apiconfig)
   - Create a new application
   - Add your Client ID to the app or build configuration

---

## 🎯 **How to Use**

### **Step 1: Export Your MAL List**
1. Go to [MyAnimeList.net](https://myanimelist.net)
2. Navigate to your profile → Export Lists
3. Download XML format for Anime and/or Manga lists

### **Step 2: Import & Process**
1. Open MAL Downloader app
2. Go to **Import Tab**
3. Tap "Import MAL XML & Download Images with Metadata"
4. Select your exported XML file
5. Watch real-time processing in **Logs Tab**

### **Step 3: Monitor Downloads**
1. Switch to **Downloads Tab** to see progress
2. View detailed logs with timestamps
3. Check **Entries Tab** for download statistics

### **Step 4: Enjoy Enhanced Gallery**
- Images saved to `/Android/data/com.harry.maldownloader/files/MAL_Images/`
- Organized by type: `ANIME/General`, `MANGA/General`, `ANIME/Adult`
- Full XMP metadata embedded for AVES Gallery
- 25+ tags automatically applied

---

## 📊 **Folder Structure**

```
MAL_Images/
├── ANIME/
│   ├── General/
│   │   ├── 12345_Attack_on_Titan.jpg
│   │   └── 67890_Demon_Slayer.jpg
│   └── Adult/
│       └── 11111_Adult_Anime.jpg
└── MANGA/
    ├── General/
    │   ├── 22222_One_Piece.jpg
    │   └── 33333_Naruto.jpg
    └── Adult/
        └── 44444_Adult_Manga.jpg
```

---

## 🏷️ **Dynamic Tagging System**

### **Automatic Tags Generated**
- **Basic**: Type (Anime/Manga), MAL ID, Status
- **Content**: Genres, Themes, Demographics
- **Production**: Studios, Producers, Authors
- **Metadata**: Year, Season, Episodes/Chapters
- **Content Rating**: Age ratings and NSFW detection
- **Custom**: User-defined tags from XML + app management

### **Example Tag Set**
```
Anime, MAL-12345, TV, Completed, Action, Drama, Studio: Madhouse,
Producer: Sony Pictures, Year: 2023, Season: Fall, Episodes: 24,
Rating: R-17+, Shounen, User-Custom-Tag
```

---

## 🔧 **Development Setup**

### **Building from Source**

```bash
# Clone repository
git clone https://github.com/Harry4004/MAL-DOWNLOADER-APK.git
cd MAL-DOWNLOADER-APK

# Setup MAL Client ID (optional)
echo "MAL_CLIENT_ID=your_client_id_here" > local.properties

# Build debug APK
./gradlew :app:assembleDebug

# Install to device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### **Development Requirements**
- Android Studio Arctic Fox or newer
- Kotlin 1.9.25+
- Gradle 8.7+
- JDK 17+
- Android SDK API 34

---

## 🐛 **Troubleshooting**

### **Common Issues**

**❌ "No entries found" in logs**
- Ensure XML file is valid MAL export (not HTML page)
- Check file permissions and storage access
- Verify XML format contains `<anime>` or `<manga>` elements

**❌ "Download failed" errors**
- Check internet connectivity
- Verify storage permissions granted
- Ensure sufficient storage space available
- Some image URLs may be region-locked

**❌ "API rate limit exceeded"**
- App automatically handles rate limiting
- MAL API: ~1000 requests/hour per Client ID
- Jikan API: ~3 requests/second (handled automatically)

**❌ App crashes or black screen**
- Clear app data and restart
- Ensure all permissions are granted
- Check Android version compatibility (7.0+)

---

## 📋 **Changelog**

### **v3.1 - Enhanced Edition** (Current)
- 🆕 **Robust Download Engine** with retry logic and progress tracking
- 🆕 **XMP Metadata Embedding** for full AVES Gallery compatibility
- 🆕 **Enhanced API Integration** with MAL official API + Jikan fallback
- 🆕 **Smart Folder Organization** with Adult/General categorization
- 🆕 **Real-time Logging** with timestamps and color coding
- 🆕 **KSP Migration** from deprecated KAPT for future compatibility
- 🆕 **Enhanced UI** with Material Design 3 and expanded log area
- 🆕 **Error Diagnostics** with comprehensive error reporting

### **v3.0 - Major Overhaul**
- ✅ Complete Jetpack Compose UI
- ✅ 25+ Dynamic metadata tags
- ✅ Professional loading screens
- ✅ Enhanced database with migrations
- ✅ Background processing

---

## ⚖️ **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### **Third-Party Libraries**
- **Retrofit** - Type-safe HTTP client
- **Moshi** - Modern JSON library
- **Room** - SQLite abstraction layer
- **Compose** - Modern Android UI toolkit
- **OkHttp** - Efficient HTTP client
- **Coil** - Image loading for Kotlin

---

## 👨‍💻 **Author**

**Harry4004** - [GitHub Profile](https://github.com/Harry4004)

---

## 🙏 **Acknowledgments**

- **MyAnimeList** - For providing the comprehensive anime/manga database
- **Jikan Team** - For maintaining the excellent unofficial MAL API
- **AVES Gallery** - For inspiring enhanced metadata implementation
- **Android Community** - For Jetpack Compose and modern development tools

---

## 📞 **Support**

For support, feature requests, or bug reports:
- 🐛 **Issues**: [GitHub Issues](https://github.com/Harry4004/MAL-DOWNLOADER-APK/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/Harry4004/MAL-DOWNLOADER-APK/discussions)
- 📧 **Email**: harshitkhatriofficial@gmail.com

---

## 🔮 **Roadmap**

### **Planned Features**
- 🔄 **Background Sync** - Automatic list updates
- 🎨 **Custom Themes** - Personalized UI themes
- 📊 **Analytics Dashboard** - Collection insights and statistics
- 🔍 **Advanced Search** - Filter and search downloaded content
- 🌐 **Multi-language Support** - Internationalization
- 📱 **Widget Support** - Home screen widgets
- ☁️ **Cloud Backup** - Optional cloud synchronization

---

*Made with ❤️ for the anime and manga community*

**⭐ Star this repo if you find it useful!**