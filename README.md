# 🛡️ Privacy Shield — Native Android (Kotlin + Compose)

AI-powered privacy display protection that detects shoulder-surfers and hides your screen — **built natively** with Kotlin, Jetpack Compose, CameraX, and Google ML Kit.

---

## ☁️ Build in the Cloud (No Setup Required)

### Step 1 — Create a GitHub Repository
1. Go to [github.com/new](https://github.com/new)
2. Name it `privacy-shield` (public or private)
3. Click **Create repository**

### Step 2 — Upload This Project
**Option A — GitHub Web UI (drag & drop):**
1. Open your new repo in the browser
2. Click **uploading an existing file**
3. Drag the entire `privacy_shield_native` folder contents
4. Commit with message: `Initial commit`

**Option B — Git (if installed):**
```bash
cd C:\Users\Aditya\Documents\privacy_shield_native
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/privacy-shield.git
git push -u origin main
```

### Step 3 — GitHub Actions Builds Automatically
- After pushing, go to your repo → **Actions** tab
- Watch the `🛡️ Build Privacy Shield APK` workflow run (~5–8 mins)
- When complete, click the run → scroll to **Artifacts**
- Download `privacy-shield-debug-X` → extract → install APK on your Android phone!

> **Enable Unknown Sources**: Settings → Security → Install Unknown Apps → allow your file manager

---

## Features

| Feature | Details |
|---------|---------|
| 👁 **AI Face Detection** | Google ML Kit, on-device, no internet required |
| 🌫️ **3 Overlay Styles** | Blur (frosted glass), Dark Curtain, Mosaic |
| 🔒 **Auto-Lock** | Locks after 10s/30s/60s when owner walks away |
| 🔐 **Biometric Dismiss** | Fingerprint or PIN to remove overlay |
| 📵 **Screenshot Block** | `FLAG_SECURE` prevents screen capture in privacy mode |
| ⚡ **Battery Efficient** | ~8 fps face detection at low resolution |
| 🎛️ **Settings** | All preferences saved with DataStore |

## Privacy Guarantee
> All face detection is **100% on-device** with Google ML Kit.  
> Zero network requests. No camera data ever leaves your device.

---

## Architecture

```
app/src/main/kotlin/com/privacyshield/app/
│
├── MainActivity.kt               ← FragmentActivity, Compose root, FLAG_SECURE
├── PrivacyShieldApp.kt           ← Application class
│
├── model/
│   ├── PrivacyState.kt           ← Enum: DISABLED / NORMAL / OWNER_AWAY / PEEKER_ALERT / LOCKED
│   └── AppSettings.kt            ← DataStore-backed settings repository
│
├── service/
│   ├── CameraAnalyzer.kt         ← CameraX ImageAnalysis.Analyzer + ML Kit
│   └── PrivacyForegroundService.kt  ← Keeps camera alive in background
│
├── viewmodel/
│   └── PrivacyViewModel.kt       ← State machine, CameraX binding, timers
│
└── ui/
    ├── HomeScreen.kt             ← Animated shield, status, toggle, info tiles
    ├── PrivacyOverlayScreen.kt   ← Full-screen overlay with biometric auth
    ├── SettingsScreen.kt         ← All settings
    └── theme/
        ├── Color.kt              ← Samsung-inspired dark palette
        └── Theme.kt              ← Material3 dark theme
```

## Privacy State Machine

```
DISABLED ──[enable]──► NORMAL ──[2+ faces × N frames]──► PEEKER_ALERT
                          │                                     │
                    [0 faces × 5 frames]              [peeker leaves / auth]
                          ↓                                     │
                      OWNER_AWAY ◄──────────────────────────────┘
                          │
                    [30s timeout]
                          ↓
                        LOCKED ──[biometric auth]──► NORMAL
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Camera | CameraX (camera-camera2) |
| Face Detection | Google ML Kit (on-device) |
| State | ViewModel + StateFlow |
| Settings | DataStore Preferences |
| Biometric | AndroidX Biometric |
| Permissions | Accompanist Permissions |
| Build | Gradle 8.7 + Kotlin DSL |
| CI/CD | GitHub Actions |

## Minimum Requirements
- Android 7.0 (API 24) or higher
- Front-facing camera
