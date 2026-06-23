package com.androidchat.app.ui.conversations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.androidchat.app.data.remote.FirebaseRepository
import com.androidchat.app.databinding.DialogNewConversationBinding
import com.androidchat.app.utils.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Bottom-sheet: find another user by email address OR @username.
 *
 * Email   — contains "@" and "."  → searched against the "email" field
 * Username — everything else      → searched against the "username" field
 *            (leading "@" is stripped automatically)
 */
class NewConversationDialog(
    private val onUserFound: (uid: String, name: String, email: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogNewConversationBinding? = null
    private val binding get() = _binding!!
    private val firebase = FirebaseRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogNewConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSearch.setOnClickListener {
            val query = binding.etEmail.text.toString().trim()
            if (query.isEmpty()) {
                requireContext().toast("Enter an email or @username")
                return@setOnClickListener
            }
            searchUser(query)
        }
    }

    private fun searchUser(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSearch.isEnabled = false

        lifecycleScope.launch {
            try {
                val data = firebase.searchUser(query)
                binding.progressBar.visibility = View.GONE
                binding.btnSearch.isEnabled = true

                if (data == null) {
                    val label = if (query.contains("@") && query.contains(".")) "email" else "username"
                    requireContext().toast("No user found with that $label")
                    return@launch
                }

                val uid   = data["uid"] as? String ?: return@launch
                val name  = data["displayName"] as? String ?: query
                val email = data["email"] as? String ?: ""
                onUserFound(uid, name, email)
                dismiss()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSearch.isEnabled = true
                requireContext().toast(e.message ?: "Search failed")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NewConversationDialog"
    }
}
