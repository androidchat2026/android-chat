package com.androidchat.app.utils

object Constants {
    // ── AdMob unit IDs ────────────────────────────────────────────────────────
    // Replace these test IDs with real IDs from the AdMob console before release.

    /** Banner shown at the bottom of the Conversations list screen */
    const val ADMOB_BANNER_CONVERSATIONS = "ca-app-pub-4231032317737732/1128073352"

    /** Banner shown at the bottom of the Chat screen */
    const val ADMOB_BANNER_CHAT = "ca-app-pub-4231032317737732/1480682750"

    /** Interstitial shown once on cold app launch (after login) */
    const val ADMOB_INTERSTITIAL_APP_OPEN = "ca-app-pub-4231032317737732/4388093292"

    // ── Firebase collections ──────────────────────────────────────────────────
    const val COLLECTION_USERS = "users"
    const val COLLECTION_MESSAGES = "messages"

    // ── Notification channels ─────────────────────────────────────────────────
    const val CHANNEL_ID_MESSAGES = "chat_messages"
    const val CHANNEL_NAME_MESSAGES = "New Messages"
}
