package com.eemphasys.vitalconnect.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.enableErrorResettingOnTextChanged
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.onSubmit
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.DialogNewConversationBinding

class NewConversationDialog: BaseBottomSheetDialogFragment() {
    lateinit var binding: DialogNewConversationBinding

    val conversationListViewModel by lazyActivityViewModel { injector.createConversationListViewModel(applicationContext) }

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

            //conversationListViewModel.createConversation(friendlyName)
        dismiss()
    }
}