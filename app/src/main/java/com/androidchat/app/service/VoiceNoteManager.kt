package com.androidchat.app.service

import android.content.Context
import java.io.File

/** Utility for managing on-device voice note files. */
object VoiceNoteManager {

    fun getVoiceNoteDir(context: Context): File =
        File(context.filesDir, "voice_notes").apply { mkdirs() }

    fun newRecordingFile(context: Context): File =
        File(getVoiceNoteDir(context), "rec_${System.currentTimeMillis()}.m4a")

    /** Delete voice note files for a conversation when the user deletes the conversation. */
    fun deleteForConversation(context: Context, messageIds: List<String>) {
        val dir = getVoiceNoteDir(context)
        messageIds.forEach { id ->
            File(dir, "$id.m4a").takeIf { it.exists() }?.delete()
        }
    }

    /** Returns total on-device voice storage used in bytes. */
    fun totalStorageUsed(context: Context): Long =
        getVoiceNoteDir(context).walkTopDown().sumOf { it.length() }
}
