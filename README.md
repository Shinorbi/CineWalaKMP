# CineWala KMP (iOS)

Kotlin Multiplatform version of CineWala app targeting **iOS** (and Android for testing).

## Project Structure

```
CineWalaKMP/
├── shared/                    # Shared Kotlin Multiplatform module
│   ├── commonMain/           # Shared code (models, ViewModels, Compose UI)
│   └── iosMain/              # iOS-specific code
├── iosApp/                   # iOS app (SwiftUI shell + Compose content)
│   ├── iosApp/
│   │   ├── iOSApp.swift      # SwiftUI app entry
│   │   ├── ContentView.swift # Wraps ComposeUIViewController
│   │   └── Info.plist
│   └── Podfile               # Cocoapods config for shared framework
├── androidApp/               # Android app (for testing shared code)
└── gradle/                   # Gradle config
```

## Requirements

- **Xcode 15+** (for iOS development)
- **JDK 11+** (for Gradle builds)
- **CocoaPods** (`sudo gem install cocoapods`)
- **Kotlin Multiplatform plugin** in IntelliJ/Android Studio

## iOS Setup

1. **Build the shared framework:**
```bash
cd CineWalaKMP
./gradlew :shared:podInstall
```

2. **Install CocoaPods dependencies:**
```bash
cd iosApp
pod install
```

3. **Open and run:**
```bash
open iosApp.xcworkspace
```
Select the `iosApp` target and run on a simulator or device.

## Android Testing

```bash
cd CineWalaKMP
./gradlew :androidApp:assembleDebug
```

## Features Ported from Android

- ✅ Home screen (recent movies, series, recently viewed)
- ✅ Movies screen (grid + infinite scroll)
- ✅ Series screen (grid + infinite scroll)
- ✅ Search screen (debounced search + pagination)
- ✅ Movie details screen
- ✅ Series details screen (seasons)
- ✅ Netflix-style dark theme
- ✅ TMDB API integration (Ktor client + kotlinx.serialization)
- ✅ Image loading (Coil 3)

## Dependencies

| Library | Purpose |
|---------|---------|
| Compose Multiplatform | UI framework |
| Ktor Client | Networking |
| kotlinx.serialization | JSON parsing |
| kotlinx.coroutines | Async/Flow |
| Coil 3 | Image loading |
| kotlinx-datetime | Date handling |

## Note

The Android player (WebView-based) is not yet implemented on iOS. The "Play Now" button in movie details is a placeholder for future video player integration.

Your original Android app remains **completely unchanged** in the parent directory.