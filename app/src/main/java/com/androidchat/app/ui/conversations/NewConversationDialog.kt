package com.androidchat.app.ui.conversations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.androidchat.app.databinding.DialogNewConversationBinding
import com.androidchat.app.utils.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Bottom-sheet that lets the user find another registered user by email
 * before opening a chat with them.
 */
class NewConversationDialog(
    private val onUserFound: (uid: String, name: String, email: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogNewConversationBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogNewConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSearch.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                requireContext().toast("Enter an email address")
                return@setOnClickListener
            }
            searchUser(email)
        }
    }

    private fun searchUser(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSearch.isEnabled = false

        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                binding.btnSearch.isEnabled = true

                val doc = result.documents.firstOrNull()
                if (doc == null) {
                    requireContext().toast("No user found with that email")
                    return@addOnSuccessListener
                }
                val uid  = doc.getString("uid")  ?: doc.id
                val name = doc.getString("displayName") ?: email
                onUserFound(uid, name, email)
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSearch.isEnabled = true
                requireContext().toast(e.message ?: "Search failed")
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
