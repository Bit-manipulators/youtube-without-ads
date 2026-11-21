<div align="center">

# 🎬 AdFreeYT

<!-- BIG ORIGINAL DOWNLOAD BUTTON -->
<a href="https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.9.apk">
  <img src="assets/badges/download-apk-button.svg" width="380" alt="Download AdFreeYT v4.9 APK"/>
</a>

<br/>
<br/>

<!-- OFFICIAL STORE / GITHUB BADGES -->
<a href="https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.9.apk">
  <img src="assets/badges/download-android-apk.svg" height="60" alt="Download Android APK"/>
</a>
&nbsp;&nbsp;
<a href="https://github.com/Bit-manipulators/youtube-without-ads/releases/latest">
  <img src="assets/badges/get-it-on-github.svg" height="60" alt="Get it on GitHub"/>
</a>

<p align="center">
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-key-features">Key Features</a> •
  <a href="#-installation-guide">Installation</a> •
  <a href="#-comparison">Why AdFreeYT?</a> •
  <a href="#-license--credits">Credits</a>
</p>

---

</div>

## 📱 Screenshots

<div align="center">
<table>
  <tr>
    <td align="center"><b>Home Feed (AMOLED Black)</b></td>
    <td align="center"><b>Immersive Auto-Hide Scroll</b></td>
    <td align="center"><b>In-App Mini Floating Player</b></td>
  </tr>
  <tr>
    <td><img src="screenshorts/Screenshot_20261120-121923_AdFreeYT.png" width="240" alt="AdFreeYT Home Feed" /></td>
    <td><img src="screenshorts/Screenshot_20261120-121927_AdFreeYT.png" width="240" alt="Immersive Auto-Hide" /></td>
    <td><img src="screenshorts/Screenshot_20261120-122016_AdFreeYT.png" width="240" alt="Mini Floating Player" /></td>
  </tr>
  <tr>
    <td align="center"><b>History & Top Actions</b></td>
    <td align="center"><b>AMOLED Black Settings</b></td>
    <td align="center"><b>Video Player & Details</b></td>
  </tr>
  <tr>
    <td><img src="screenshorts/Screenshot_20261120-122024_AdFreeYT.png" width="240" alt="History Tab" /></td>
    <td><img src="screenshorts/Screenshot_20261120-122029_AdFreeYT.png" width="240" alt="Settings Menu" /></td>
    <td><img src="screenshorts/Screenshot_20261120-122516_AdFreeYT.png" width="240" alt="Video Player" /></td>
  </tr>
</table>
</div>

## 🌟 Key Features

<table>
  <tr>
    <td width="50%">
      <h3>🚫 100% Ad-Free</h3>
      Enjoy YouTube uninterrupted. No video ads, audio ads, sponsored banners, or popups.
    </td>
    <td width="50%">
      <h3>🎵 Background & Lockscreen Audio</h3>
      Listen to podcasts, music, and videos with your screen off or while multitasking in other apps.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>🖼️ Picture-in-Picture (PiP)</h3>
      Keep streaming in a smooth, resizable floating popup window anywhere on your device.
    </td>
    <td width="50%">
      <h3>📱 Real YouTube Interface</h3>
      Redesigned modern card feed, branded top navigation bar, and Home/Save/History bottom tabs with animated auto-hide on scroll.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>🔒 Private by Design</h3>
      No Google account or Play Services required. Your watch history, bookmarks, and subscriptions remain securely on your device.
    </td>
    <td width="50%">
      <h3>💾 Offline Downloads</h3>
      Download high-definition video (up to 4K 60fps HDR), standalone audio (M4A/Opus), and subtitles directly to your local storage.
    </td>
  </tr>
</table>

---

## 🚀 What's New in v4.9

- **Video Detail Dock**: New glass floating dock with quick tabs for current running video content (Video / Related, Description, and Comments).
- **Fullscreen Swipe Gestures**: Swipe up on any video to enter fullscreen; swipe down in fullscreen to rotate back to normal.
- **3-Zone Fullscreen Controls**: Screen partitioned into three intuitive touch zones:
  - Left 1/3: Brightness adjustments.
  - Middle 1/3: Minimizing and exiting fullscreen on downward swipe.
  - Right 1/3: Volume adjustments.
- **Search Navigation & Back Button**: Added dedicated back arrow to search bar and fixed back navigation across search suggestions and results.
- **Tab Navigation Glitch Fix**: Fixed multi-tab triggering and unexpected redirects when switching tabs from the search page.
- **Instant Feed Tab Switching**: Kept feed page cached in memory when switching to history and back, completely eliminating unnecessary reloading.
- **Controller Spacing Fix**: Resolved empty top spacing in portrait video controller.

---

## 📥 Installation Guide

### Option 1: Direct Download (Standard Android)

1. Tap the [**Big Download Button**](https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.9.apk) above or visit [Releases](https://github.com/Bit-manipulators/youtube-without-ads/releases/latest).
2. Download `AdFreeYT_v4.9.apk`.
3. Open the downloaded file and select **Install** (allow "Install from Unknown Sources" if prompted).

### Option 2: Termux

If you are running Termux with storage permission:

```bash
# Download latest release directly
curl -L -o AdFreeYT_v4.9.apk https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.9.apk

# Install via Android Package Installer
termux-open AdFreeYT_v4.9.apk
```

---

## 📊 Why AdFreeYT?

| Feature | Official YouTube | AdFreeYT |
| :--- | :---: | :---: |
| **No Video Ads** | ❌ (Requires Premium) | ✅ **Built-in & Free** |
| **Background Playback** | ❌ (Requires Premium) | ✅ **Included** |
| **Picture-in-Picture** | ⚠️ (Region / Premium locked) | ✅ **Always Enabled** |
| **Local Video & Audio Downloads** | ❌ (Encrypted in-app only) | ✅ **Full MP4 / M4A / WebM files** |
| **Google Account Required** | ⚠️ Yes | ❌ **No Account Needed** |
| **App Size** | ~150+ MB | **~16 MB** |

---

## 🛠️ Building from Source

```bash
git clone -b newpipe https://github.com/Bit-manipulators/youtube-without-ads.git
cd youtube-without-ads
./gradlew assembleContinuous
```

The output APK will be generated at `app/build/outputs/apk/continuous/app-continuous.apk`.

---

## 📜 License & Credits

- Built with ❤️ based on the open-source [NewPipe](https://github.com/TeamNewPipe/NewPipe) project.
- Licensed under the **[GNU General Public License v3.0](LICENSE)**.
