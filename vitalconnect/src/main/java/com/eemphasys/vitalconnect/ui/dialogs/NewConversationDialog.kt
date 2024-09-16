package com.eemphasys.vitalconnect.ui.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.enableErrorResettingOnTextChanged
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.onSubmit
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.DialogNewConversationBinding
import com.eemphasys.vitalconnect.viewModel.CheckNameCallback
import com.twilio.conversations.Attributes
import org.json.JSONObject

class NewConversationDialog: BaseBottomSheetDialogFragment() {
    lateinit var binding: DialogNewConversationBinding

    val conversationListViewModel by lazyActivityViewModel { injector.createConversationListViewModel(applicationContext) }
    val contactListViewModel by lazyActivityViewModel { injector.createContactListViewModel(applicationContext) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogNewConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newConversationNameInputHolder.enableErrorResettingOnTextChanged()
        binding.newConversationNameInput.onSubmit { createConversation() }
        binding.createConversation.setOnClickListener { createConversation() }
        binding.cancelButton.setOnClickListener { dismiss() }
    }

    private fun createConversation() {
        val friendlyName = binding.newConversationNameInput.text.toString()
        if (friendlyName.isBlank()) {
            binding.newConversationNameInputHolder.error = getString(R.string.profile_friendly_name_error_text)
            return
        }
        contactListViewModel.checkName(friendlyName, object : CheckNameCallback {
            override fun onResult(exists: Boolean) {
                if (exists) {
                    // The conversation exists
                    binding.newConversationNameInputHolder.error = "Conversation already exists, try different name"
                    binding.newConversationNameInputHolder.enableErrorResettingOnTextChanged()
                } else {
                    // The conversation does not exist
                    val attributes = mapOf("isWebChat" to true)
                        val jsonObject = JSONObject(attributes)
                        contactListViewModel.createWebConversation(friendlyName, Attributes(jsonObject),Constants.CURRENT_CONTACT)
                        dismiss()
                }
            }
        })
    }
}