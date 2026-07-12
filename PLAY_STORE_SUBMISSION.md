# Secure AI — Google Play Store Submission Guide

## App Identity

- **App Name**: Secure AI
- **Package Name**: one.secureai.app
- **Developer**: SecureAI One
- **Website**: https://secureai.one
- **Support Email**: secureai.one@gmail.com
- **Privacy Policy**: https://secureai.one/privacy

## Current Build

- **Version Name**: 1.0.0
- **Version Code**: 1
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **AAB Location**: `app/build/outputs/bundle/release/app-release.aab`
- **AAB Size**: ~10 MB

## Release Keystore

- **File**: `app/keystore/release.keystore` (gitignored — DO NOT lose this file)
- **Password**: `5t36tljR+edjBqGJOrp/Jjc0Ngc6pgeg`
- **Key Alias**: `secureai`
- **Key Password**: `5t36tljR+edjBqGJOrp/Jjc0Ngc6pgeg`
- **Algorithm**: RSA 2048-bit
- **Validity**: 10,000 days (expires ~2053)
- **DN**: CN=Secure AI, OU=Mobile, O=SecureAI One, L=Houston, ST=Texas, C=US

IMPORTANT: Back up this keystore and password. If lost, you cannot push updates
to the same Play Store listing — Google requires the same signing key forever.
Consider enrolling in Play App Signing (lets Google hold a copy).

## How to Rebuild

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/herbertperryman/Documents/secure-ai-android-native
./gradlew bundleRelease
```

The signed AAB appears at `app/build/outputs/bundle/release/app-release.aab`.

Signing credentials are read from `local.properties` (already configured).

## Play Console Setup Checklist

### 1. Create App
- Go to https://play.google.com/console
- Create app > "Secure AI"
- Select: App, Free, English

### 2. Store Listing

**Short description** (80 chars max):
```
Your private AI assistant — chat, create images, and organize your life.
```

**Full description** (4000 chars max):
```
Secure AI is your personal AI assistant that keeps your data private and secure.

Chat naturally with advanced AI models to get help with writing, coding, brainstorming, research, and everyday questions. Generate stunning images from text descriptions. Organize your thoughts with memories, tasks, and projects.

KEY FEATURES

- AI Chat: Conversational AI powered by the latest Claude models
- Image Generation: Create images from natural language prompts
- Memories: Save important information the AI remembers across conversations
- Projects: Organize your work into separate contexts
- Library: Store and reference your documents and photos
- Incognito Mode: Chat without saving history
- Background Themes: Personalize your chat with Ocean, Sunset, Forest, and more
- Biometric Lock: Protect your conversations with fingerprint or face unlock
- Push Notifications: Stay updated on important responses
- Home Screen Widget: Quick access to start a new chat
- Share Target: Send text or images from any app directly to Secure AI
- Cross-Platform: Syncs with Secure AI on iOS and web

PRIVACY FIRST

Your conversations are encrypted and private. We don't sell your data or use it to train AI models. Incognito mode ensures nothing is saved — not even on your device.

SUBSCRIPTION TIERS

- Free: Daily message and image limits with standard models
- Plus: Higher limits and access to premium AI models
- Pro: Unlimited messages, priority processing, and all models

Download Secure AI and experience the future of private AI assistance.
```

**Category**: Tools or Productivity
**Tags**: AI, assistant, chatbot, privacy, image generation

### 3. Graphics Assets

Required uploads in Play Console:

| Asset | Size | Notes |
|-------|------|-------|
| App icon | 512x512 PNG | Auto-generated from your ic_launcher |
| Feature graphic | 1024x500 PNG | Banner shown at top of listing |
| Phone screenshots | 1080x1920+ | Min 2, max 8. Show: home, chat, sidebar, settings |
| 7" tablet screenshots | 1200x1920+ | Optional but recommended |
| 10" tablet screenshots | 1920x1200+ | Optional but recommended |

Recommended screenshots to capture (on a real device or emulator):
1. Home screen with greeting card
2. Active chat conversation
3. Sidebar open showing navigation
4. Settings screen (dark theme)
5. Image generation result
6. Background theme picker
7. Incognito mode active

### 4. Content Rating
- Go to Policy > App content > Content rating
- Complete the IARC questionnaire
- Category: Utility / Productivity
- No violence, no sexual content, no gambling
- User-generated content: Yes (AI responses)
- Expected rating: Everyone / PEGI 3

### 5. Privacy & Data Safety
- Data safety section required
- Data collected: email, name (for account), chat messages (for AI processing)
- Data shared with third parties: No
- Data encrypted in transit: Yes
- Users can request deletion: Yes
- Privacy policy URL: https://secureai.one/privacy

### 6. App Access
- If review team needs to test, provide a test account or note that
  anonymous/guest mode works without sign-in

### 7. Pricing & Distribution
- Free (with in-app purchases)
- Countries: All
- Contains ads: No
- In-app purchases: Yes (subscription tiers)

### 8. Upload AAB
- Go to Release > Production > Create new release
- Upload `app-release.aab`
- Add release notes: "Initial release of Secure AI for Android"
- Review and roll out

## Permissions Declared

| Permission | Why |
|------------|-----|
| INTERNET | AI chat and sync |
| POST_NOTIFICATIONS | Push notifications |
| USE_BIOMETRIC / USE_FINGERPRINT | Biometric app lock |
| CAMERA | Take photos to send in chat |
| READ_MEDIA_IMAGES | Attach photos to chat |
| ACCESS_NETWORK_STATE | Check connectivity |

## App Features

- Firebase Auth (Google Sign-In + anonymous)
- Firebase Firestore (user profiles, conversations)
- Firebase Cloud Messaging (push notifications)
- Firebase Crashlytics (crash reporting)
- Cloudflare Workers backend (AI inference)
- Google Play Billing (subscriptions)
- Google Play In-App Updates
- Google Play In-App Review
- Deep linking (secureai.one)
- Share target (text + images)
- Home screen widget
- Biometric lock screen

## Version History

| Version | Code | Date | Notes |
|---------|------|------|-------|
| 1.0.0 | 1 | 2026-07-11 | Initial Play Store release |

## Updating the App

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Run `./gradlew bundleRelease`
3. Upload new AAB to Play Console
4. Add release notes
5. Roll out (staged or full)
