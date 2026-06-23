package com.androidchat.app.ui.conversations

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.androidchat.app.R
import com.androidchat.app.databinding.ActivityConversationsBinding
import com.androidchat.app.ui.auth.LoginActivity
import com.androidchat.app.ui.chat.ChatActivity
import com.androidchat.app.ui.profile.ProfileActivity
import com.androidchat.app.ui.support.SupportActivity
import com.androidchat.app.utils.Constants
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.auth.FirebaseAuth

class ConversationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationsBinding
    private val vm: ConversationsViewModel by viewModels()
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // ── AdMob (SDK already initialised in ChatApplication) ───────────────
        loadBannerAd()
        loadInterstitialAd()

        // ── RecyclerView ──────────────────────────────────────────────────────
        val adapter = ConversationsAdapter { conv ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conv.id)
                putExtra(ChatActivity.EXTRA_PARTICIPANT_ID, conv.participantId)
                putExtra(ChatActivity.EXTRA_PARTICIPANT_NAME, conv.participantName)
            }
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter

        vm.conversations.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // ── FAB — start new conversation ──────────────────────────────────────
        binding.fab.setOnClickListener { showNewChatDialog() }

    }

    // ── AdMob: banner at the bottom of the conversations list ─────────────────
    private fun loadBannerAd() {
        val adView = AdView(this).apply {
            adUnitId = Constants.ADMOB_BANNER_CONVERSATIONS
            setAdSize(AdSize.BANNER)
        }
        binding.adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    // ── AdMob: interstitial shown once on app open ────────────────────────────
    private fun loadInterstitialAd() {
        InterstitialAd.load(
            this,
            Constants.ADMOB_INTERSTITIAL_APP_OPEN,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.show(this@ConversationsActivity)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showNewChatDialog() {
        NewConversationDialog { uid, name, _ ->
            vm.startNewConversation(uid, name) { convId, partId, partName ->
                startActivity(
                    Intent(this, ChatActivity::class.java).apply {
                        putExtra(ChatActivity.EXTRA_CONVERSATION_ID, convId)
                        putExtra(ChatActivity.EXTRA_PARTICIPANT_ID, partId)
                        putExtra(ChatActivity.EXTRA_PARTICIPANT_NAME, partName)
                    }
                )
            }
        }.show(supportFragmentManager, NewConversationDialog.TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_conversations, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_support -> {
                startActivity(Intent(this, SupportActivity::class.java))
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
