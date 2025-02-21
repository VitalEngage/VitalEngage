package com.eemphasys.vitalconnect.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.enableErrorResettingOnTextChanged
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.onSubmit
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.DialogNewConversationBinding
import com.eemphasys.vitalconnect.viewModel.CheckNameCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create the dialog instance
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // Prevent dismissal when tapping outside
        dialog.setCanceledOnTouchOutside(false)
        // Optionally, prevent dismissal with the back button
        isCancelable = false
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newConversationNameInputHolder.enableErrorResettingOnTextChanged()
        binding.newConversationNameInput.onSubmit { }
        binding.createConversation.setOnClickListener {
            binding.progressBarID.visibility = View.VISIBLE
            binding.createConversation.isEnabled = false
            binding.cancelButton.isEnabled = false
            createConversation()
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_Contacts_NewConversationDialog_ConfirmClick",
                "Contacts",
                "NewConversationDialog"
            )
        }
        binding.cancelButton.setOnClickListener { dismiss()
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_Contacts_NewConversationDialog_CancelClick",
                "Contacts",
                "NewConversationDialog"
            )}
    }

    private fun createConversation() {

        val friendlyName = binding.newConversationNameInput.text.toString().trim()
        if (friendlyName.isBlank()) {
            binding.createConversation.isEnabled = true
            binding.cancelButton.isEnabled = true
            binding.progressBarID.visibility = View.GONE
            binding.newConversationNameInputHolder.error = getString(R.string.blank_conversation_name)
            return
        }
        contactListViewModel.checkName(friendlyName, object : CheckNameCallback {
            override fun onResult(exists: Boolean) {
                if (exists) {
                    // The conversation exists
                    binding.newConversationNameInputHolder.error = getString(R.string.name_exists)
                    binding.newConversationNameInputHolder.enableErrorResettingOnTextChanged()
                    binding.createConversation.isEnabled = true
                    binding.cancelButton.isEnabled = true
                    binding.progressBarID.visibility = View.GONE
                } else {
                    // The conversation does not exist
                    val attributes = mapOf("isWebChat" to true)
                        val jsonObject = JSONObject(attributes)
                        contactListViewModel.createWebConversation(friendlyName, Attributes(jsonObject),Constants.CURRENT_CONTACT){
                            binding.progressBarID.visibility = View.GONE
                            dismiss()
                        }
                }
            }
        })
    }
}