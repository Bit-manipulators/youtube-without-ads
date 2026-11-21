<div align="center">

# 🎬 AdFreeYT

<!-- BIG ORIGINAL DOWNLOAD BUTTON -->
<a href="https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.10.apk">
  <img src="assets/badges/download-apk-button.svg" width="380" alt="Download AdFreeYT v4.10 APK"/>
</a>

<br/>
<br/>

<!-- OFFICIAL STORE / GITHUB BADGES -->
<a href="https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.10.apk">
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

## 🚀 What's New in v4.10

- **Search Header & Seamless Scroll**: Fixed empty black space during search scrolling; search bar and "showing results for..." header slide away together with results scrolling cleanly to the top.
- **Search Back Button & Navigation**: Added fully responsive back arrow and fixed hardware back navigation from search directly to the feed.
- **Tab Switching & Blank Screen Fix**: Resolved blank screen issue when switching tabs; now recreates adapter cleanly and synchronizes bottom dock with ViewPager without listener conflicts.
- **Direct Save & History Navigation from Search**: Bottom dock buttons (Saved, History, Feed) now work immediately when pressed from the search page.
- **Infinite Feed Prefetching (Zero Buffering)**: Pre-loads next feed items 6 items before hitting the bottom, increasing feed batch sizes to 30 items for smooth, endless scrolling without buffering pauses.
- **In-App Update Prompt & Fast Checks**: Added in-app update dialog so users are immediately notified of new releases even if notification tray permissions are disabled.
- **Player Dock & Transparent Navigation Bar**: Main bottom dock hides completely when expanding video detail; navigation bar stays 100% transparent with immersive edge-to-edge drawing.
- **Swipe Gestures to Fullscreen Landscape**: Swiping up enters fullscreen in landscape mode; swiping down exits fullscreen and rotates back to normal portrait mode.
- **Strict Portrait Lock Outside Fullscreen**: App rotation is strictly locked to portrait across all screens, rotating to landscape ONLY during fullscreen video playback.

---

## 📥 Installation Guide

### Option 1: Direct Download (Standard Android)

1. Tap the [**Big Download Button**](https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.10.apk) above or visit [Releases](https://github.com/Bit-manipulators/youtube-without-ads/releases/latest).
2. Download `AdFreeYT_v4.10.apk`.
3. Open the downloaded file and select **Install** (allow "Install from Unknown Sources" if prompted).

### Option 2: Termux

If you are running Termux with storage permission:

```bash
# Download latest release directly
curl -L -o AdFreeYT_v4.10.apk https://github.com/Bit-manipulators/youtube-without-ads/releases/latest/download/AdFreeYT_v4.10.apk

# Install via Android Package Installer
termux-open AdFreeYT_v4.10.apk
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
