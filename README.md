# YouTube Without Ads (Android APK)

An ad-free, high-performance Android web wrapper for YouTube built with native Kotlin and Android WebView.

---

## 🚀 Features

- **Built-in Ad-Block Engine**:
  - **Network-Level Blocking**: Intercepts known ad tracking hostnames (`googleads`, `doubleclick`, `stats.youtube.com`, etc.) before they are loaded.
  - **DOM & CSS Element Hiding**: Permanently hides promotional banners, sponsored suggestions, and masthead ads.
  - **Video Ad Fast-Forward & Auto-Skip**: Mutes and fast-forwards through video ads at 16x speed and auto-clicks "Skip Ad" buttons immediately.
  - **Anti-Adblock Bypass**: Dismisses YouTube's anti-adblock detection modal dialogs and unpauses playback.
- **Background Audio Playback**: Continues audio playback when minimizing the app or locking the screen (spoofs `document.visibilityState`).
- **Picture-in-Picture (PiP)**: Automatically enters Android native PiP mode when leaving the app while watching a video.
- **Fullscreen HTML5 Video**: Smooth transition to landscape fullscreen video player with system immersive mode.
- **Google Account Login**: Uses a customized Chrome Mobile User-Agent to allow standard Google Account sign-in without security rejection.
- **Swipe-to-Refresh**: Pull down to refresh the page easily.

---

## 📁 Project Architecture

```
youtube-without-ads/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml                  # Permissions, PiP & deep-link filters
│   │   ├── assets/
│   │   │   ├── adblock.js                       # JavaScript injection engine
│   │   │   └── ad_hosts.txt                     # Network-level domain blocklist
│   │   ├── java/com/adfree/youtube/
│   │   │   ├── MainActivity.kt                  # WebView, PiP, navigation & fullscreen
│   │   │   └── AdBlocker.kt                     # Host matching & script asset loader
│   │   └── res/
│   │       ├── layout/activity_main.xml         # Fullscreen & webview layout
│   │       └── values/                          # App theme & strings
│   ├── build.gradle.kts                         # App module configuration
│   └── proguard-rules.pro
├── .github/workflows/build-apk.yml              # CI/CD to build APK on GitHub
├── build.gradle.kts                             # Top-level Gradle config
└── settings.gradle.kts                          # Module definitions
```

---

## 🛠️ How to Build the APK

### Method 1: Automatic via GitHub Actions
1. Push this repository to GitHub.
2. Go to the **Actions** tab in your GitHub repository.
3. The `Build Android APK` workflow will run automatically on push or via **Run workflow**.
4. Download the compiled `YouTubeWithoutAds-debug-apk` from the workflow summary.

### Method 2: Local Build with Android Studio
1. Clone this repo to your PC:
   ```bash
   git clone https://github.com/amitpadhan979/youtube-without-ads.git
   ```
2. Open the project in **Android Studio** (Hedgehog or newer).
3. Allow Gradle to sync dependencies.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)** or run:
   ```bash
   ./gradlew assembleDebug
   ```
5. Transfer the generated APK (`app/build/outputs/apk/debug/app-debug.apk`) to your phone and install!
