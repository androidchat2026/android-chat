package com.androidchat.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.androidchat.app.databinding.ActivityChatBinding
import com.androidchat.app.utils.Constants
import com.androidchat.app.utils.toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import java.io.File

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_PARTICIPANT_ID = "participant_id"
        const val EXTRA_PARTICIPANT_NAME = "participant_name"
    }

    private lateinit var binding: ActivityChatBinding
    private val vm: ChatViewModel by viewModels()

    private lateinit var conversationId: String
    private lateinit var participantId: String
    private lateinit var participantName: String

    // Voice recording state
    private var recorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var recordingStartMs = 0L
    // 50 s max keeps base64-encoded audio safely under Firestore's 1 MB document limit
    private val MAX_RECORDING_MS = 50_000L

    // Playback
    private var player: ExoPlayer? = null

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) toast("Microphone permission is required for voice notes")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        participantId = intent.getStringExtra(EXTRA_PARTICIPANT_ID) ?: return
        participantName = intent.getStringExtra(EXTRA_PARTICIPANT_NAME) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = participantName
            setDisplayHomeAsUpEnabled(true)
        }

        // ── AdMob: banner at the bottom of the chat screen ────────────────────
        loadBannerAd()

        // ── Messages list ─────────────────────────────────────────────────────
        val adapter = ChatAdapter { path -> playVoiceNote(path) }
        binding.recyclerView.adapter = adapter

        vm.getMessages(conversationId).observe(this) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty())
                binding.recyclerView.scrollToPosition(messages.size - 1)
        }

        vm.markRead(conversationId)

        // ── Send text ─────────────────────────────────────────────────────────
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            vm.sendText(conversationId, participantId, text)
            binding.etMessage.setText("")
        }

        // ── Voice note: hold to record, release to send ───────────────────────
        binding.btnVoice.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startRecording()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopAndSendRecording()
            }
            true
        }

        vm.error.observe(this) { err -> err?.let { toast(it) } }
    }

    // ── AdMob banner at the bottom of the chat ────────────────────────────────
    private fun loadBannerAd() {
        val adView = AdView(this).apply {
            adUnitId = Constants.ADMOB_BANNER_CHAT
            setAdSize(AdSize.BANNER)
        }
        binding.adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    // ── Voice recording ───────────────────────────────────────────────────────

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val dir = File(filesDir, "voice_notes").apply { mkdirs() }
        voiceFile = File(dir, "recording_${System.currentTimeMillis()}.m4a")

        @Suppress("DEPRECATION")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(voiceFile!!.absolutePath)
            prepare()
            start()
        }
        recordingStartMs = SystemClock.elapsedRealtime()
        binding.tvRecording.visibility = View.VISIBLE

        // Auto-stop at 50 s to stay within Firestore 1 MB document limit
        binding.btnVoice.postDelayed({ stopAndSendRecording() }, MAX_RECORDING_MS)
    }

    private fun stopAndSendRecording() {
        binding.tvRecording.visibility = View.GONE
        val durationMs = SystemClock.elapsedRealtime() - recordingStartMs
        try {
            recorder?.apply { stop(); release() }
        } catch (e: Exception) {
            voiceFile?.delete()
            return
        } finally {
            recorder = null
        }

        val file = voiceFile ?: return
        if (durationMs < 500) {
            // Discard very short clips
            file.delete()
            return
        }
        vm.sendVoice(conversationId, participantId, file, durationMs)
    }

    // ── Voice playback ────────────────────────────────────────────────────────

    private fun playVoiceNote(path: String) {
        player?.release()
        player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(File(path))))
            prepare()
            play()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        player?.release()
    }
}
