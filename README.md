# Secure AI — Native Android

Native Kotlin + Jetpack Compose Android app for [secureai.one](https://secureai.one).

## Architecture

Hybrid shell — native Kotlin/Compose for onboarding, biometric lock, settings, and system integrations. WebView for the chat interface (live-updates from the website, no rebuild needed for product changes).

## Setup

### 1. Prerequisites
- Android Studio Ladybug or later
- JDK 17
- Google Play developer account

### 2. Firebase
1. Go to [Firebase Console](https://console.firebase.google.com) → your existing Secure AI project
2. Add an Android app with package name `one.secureai.app`
3. Download `google-services.json` and place it in `app/`

### 3. Keystore
Generate a release keystore (keep this safe — losing it means you can't update your app):
```bash
keytool -genkey -v \
  -keystore app/keystore/release.keystore \
  -alias secureai \
  -keyalg RSA -keysize 2048 -validity 10000
```

Get your SHA-256 fingerprint (needed for assetlinks.json):
```bash
keytool -list -v -keystore app/keystore/release.keystore -alias secureai
```

Update `public/.well-known/assetlinks.json` in the web project with the fingerprint, then deploy.

### 4. Local builds
```bash
# Debug
./gradlew assembleDebug

# Release (set env vars first)
export KEYSTORE_PATH=app/keystore/release.keystore
export KEYSTORE_PASSWORD=yourpassword
export KEY_ALIAS=secureai
export KEY_PASSWORD=yourpassword
./gradlew bundleRelease
```

## CI / GitHub Releases

Add these secrets to GitHub repo settings:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i app/keystore/release.keystore` |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `secureai` |
| `KEY_PASSWORD` | Your key password |

Then tag a release to trigger the build:
```bash
git tag v1.0.0 && git push origin v1.0.0
```

CI produces a signed `.aab` (Play Store) and `.apk` (sideload/testing).

## Features

- **Native onboarding** — 4-screen animated first-run flow
- **Biometric lock** — fingerprint/face on every open (toggle in Settings)
- **WebView chat** — loads secureai.one/chat with full JS, file picker, camera
- **Native bridge** — web app can call `window.SecureAI.openSettings()`
- **Offline screen** — detects no connection, retry button
- **App shortcuts** — long-press: New Chat, Tasks, Memories
- **Home screen widget** — 3 tap targets
- **Share target** — receive text/images from any app
- **Push notifications** — Firebase FCM
- **In-app update** — flexible update prompt from Play Store
- **Play Store review** — requests review after 5 sessions
- **Crashlytics** — crash reports in Firebase console
- **Settings screen** — biometric toggle, privacy policy, feedback, version

## Deep links

All `https://secureai.one/*` URLs open in the app. Requires `assetlinks.json` deployed to the web.
