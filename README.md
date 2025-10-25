# 📱 MAL Downloader v3.2 — Enhanced UI + Stability

> Professional MyAnimeList Image Downloader with a polished, modern UI and rock‑solid workflow

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🚀 What’s New (v3.2)

A focused UI/UX refinement pass with safe, incremental commits. No business‑logic changes — just cleaner visuals and better ergonomics.

- 🧊 Glassmorphism design system across cards, dialogs, and headers
- 🧭 Top bar + animated tabs rebuilt; tabs now compact with app‑tinted glyphs and single‑line labels
- 🍥 Import tab polished (primary actions with ModernButton variants)
- 📂 Entries list redesigned (glass item cards, consistent actions)
- ⬇️ Downloads tab updated (stats pills, modern item cards, progress/error styling)
- 📝 Logs panel upgraded (glass header, copy/share/clear with swipe dialog)
- 🧩 New Modern UI components: ModernGlassCard, ModernSwipeDialog, ModernButton, GlassmorphismTopBar, AnimatedTabRow, ModernGlassmorphismDrawer
- 🔤 Design tokens added (spacing + typography) for consistent paddings and text sizes
- 🏷️ Tag Manager dialog refreshed; fixed a Kotlin import typo that caused a build failure
- 🛠️ Build fixes: resolved duplicate drawer overloads, corrected imports (Offset, RoundedCornerShape), added missing Error/Loading screens

> Result: cleaner hierarchy, better contrast, compact controls, and consistent visuals — with zero changes to your ViewModel or data flow.

---

## 🖼️ UI Highlights

- Compact, theme‑tinted glyph tabs — no more multi‑line wrapped labels
- Polished primary actions using ModernButton (Primary/Secondary/Tertiary)
- Glass headers with subtle gradients and glow for status/section blocks
- Swipe‑to‑dismiss, liquid glass dialogs for Tag Manager, Logs clear, etc.
- Consistent chips, icons, and spacing across all tabs

---

## 📚 Tabs Overview

- 🍥 Import: Status header, primary import action, custom tags flow
- 📂 Entries: Search + mode chip, type filters, glass cards, action buttons
- ⬇️ Downloads: Stats pills, progress bars, error containers, item menus
- 📝 Logs: Filter chips (All/Error/Warn/Info), copy/share/clear with confirmation
- ⚙️ Settings: Consolidated configuration (unchanged behavior)
- ℹ️ About: Version/build info, feature showcase, contact

---

## 🧱 Architecture (unchanged)

- MVVM + Repository, Jetpack Compose (Material3), Coroutines + StateFlow
- Public Pictures storage (MediaStore), dual API enrichment (MAL + Jikan)
- Room DB, robust queue + retry logic, XMP/metadata tagging

---

## 🛠️ Build & Run

```bash
# Clone
git clone https://github.com/Harry4004/MAL-DOWNLOADER-APK.git
cd MAL-DOWNLOADER-APK

# Configure MAL Client ID
echo "MAL_CLIENT_ID=your_client_id" > local.properties

# Build
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requirements: Android 7.0+ (API 24+), JDK 17, Kotlin 1.9.25+, Gradle 8.7+, Target SDK 34.

---

## 🧩 Key Components (new/updated)

- `ui/components/ModernUIComponents.kt`
  - ModernGlassCard, ModernSwipeDialog, ModernButton
  - GlassmorphismTopBar, AnimatedTabRow (compact themed tabs)
  - ModernGlassmorphismDrawer, permission chips
- `ui/components/EnhancedImportTab.kt` — polished actions and status header
- `ui/components/EnhancedEntriesList.kt` — glass cards + modern actions
- `ui/components/EnhancedDownloadsTab.kt` — stats pills + progress/error styling
- `ui/components/EnhancedLogsPanel.kt` — glass header + swipe dialog
- `ui/components/TagManagerDialog.kt` — refreshed UI; fixed import typo
- `ui/theme/Tokens.kt` — spacing + typography tokens

---

## 🐛 Fixes & Stability

- Fixed Cyrillic typo in `TagManagerDialog.kt` (`импорт` → `import`)
- Removed duplicate `ModernDrawer.kt` causing overload conflicts
- Corrected imports (`Offset` from `ui.geometry`, `RoundedCornerShape` in downloads)
- Re‑added `LoadingScreen` and `ErrorScreen` composables in `MainActivity.kt`
- Adjusted alignment in `AboutDialog.kt` (Horizontal vs Vertical)

---

## 📬 Support

- Issues: https://github.com/Harry4004/MAL-DOWNLOADER-APK/issues
- Discussions: https://github.com/Harry4004/MAL-DOWNLOADER-APK/discussions
- Email: myaninelistapk@gmail.com

If the app helps you, please ⭐ star the repo!

---

### Version History
- v3.2 — Enhanced UI polish, compact tabs, stability fixes
- v3.1 — Complete feature suite (6 tabs, advanced functionality)
- v3.0 — Major Compose overhaul
- v2.x — Basic downloader
- v1.x — Prototype
