# Android Chat — Setup Guide

## 1. Firebase Setup

1. Go to https://console.firebase.google.com and create a new project.
2. Add an **Android app** with package name `com.androidchat.app`.
3. Download `google-services.json` and replace `app/google-services.json`.
4. Enable the following services:
   - **Authentication → Email/Password**
   - **Firestore Database** (start in production mode, then apply `firestore.rules`)
   - **Firebase Storage** (apply `storage.rules`)
   - **Cloud Messaging** (enabled by default)

Deploy rules:
```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:rules,storage
```

## 2. AdMob Setup

1. Create an account at https://admob.google.com.
2. Create an Android app in AdMob.
3. Create **3 ad units**:
   | Ad Unit | Placement | Type |
   |---------|-----------|------|
   | Conversations Banner | Bottom of conversations list | Banner |
   | Chat Banner | Bottom of chat screen | Banner |
   | App Open Interstitial | Shown once on cold launch | Interstitial |
4. Ad unit IDs are already set in `Constants.kt`:
   ```kotlin
   ADMOB_BANNER_CONVERSATIONS  = "ca-app-pub-4231032317737732/1128073352"
   ADMOB_BANNER_CHAT           = "ca-app-pub-4231032317737732/1480682750"
   ADMOB_INTERSTITIAL_APP_OPEN = "ca-app-pub-4231032317737732/4388093292"
   ```
5. AdMob App ID is already set in `app/build.gradle`:
   ```groovy
   manifestPlaceholders = [admobAppId: "ca-app-pub-4231032317737732~6697350419"]
   ```

## 3. GitHub Actions Cleanup

1. Go to your GitHub repo → **Settings → Secrets and variables → Actions**.
2. Add two secrets:
   - `FIREBASE_PROJECT_ID` — your Firebase project ID (e.g. `my-chat-app-12345`)
   - `FIREBASE_SERVICE_ACCOUNT_KEY` — paste the full JSON content of a Firebase service account key
     (create one at Firebase Console → Project Settings → Service Accounts → Generate new private key)
3. The workflow runs automatically every day at **02:00 UTC**.
   You can also trigger it manually from the **Actions** tab.

## 4. Build & Run

```bash
# Open the project in Android Studio
# File → Open → select the "Android Chat" folder
# Wait for Gradle sync, then Run ▶
```

## Architecture Overview

```
User sends text message:
  ChatActivity → ChatViewModel → ChatRepository
    → Room (saved locally, status=SENDING)
    → FirebaseRepository → Firestore /messages/{id}
    → Room (status=SENT)

User receives message (Firestore listener):
  FirebaseRepository.listenForIncomingMessages()
    → ChatRepository.handleIncomingMessage()
      → if voice: download from Storage → save to /filesDir/voice_notes/
      → Room (saved locally, status=DELIVERED)
      → Firestore: mark consumed=true
      → Storage blob deleted immediately after download

GitHub Actions (02:00 UTC daily):
  scripts/firebase-cleanup.js
    → Delete consumed Firestore docs older than 24h
    → Delete stale undelivered docs older than 48h
    → Delete Storage blobs older than 24h (safety net)
```

## Ad Unit Placement Map

| Screen | Location | Ad Type |
|--------|----------|---------|
| Conversations list | Bottom of screen, above keyboard | Banner (320×50) |
| Chat screen | Bottom of screen, above input bar | Banner (320×50) |
| App launch | After login / on cold start | Interstitial (full-screen) |
