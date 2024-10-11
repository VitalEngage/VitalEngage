package com.eemphasys.vitalconnect.ui.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.MessageType.MEDIA
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.databinding.DialogMessageActionsBinding

class MessageActionsDialog : BaseBottomSheetDialogFragment() {

    lateinit var binding: DialogMessageActionsBinding

    val messageListViewModel by lazyActivityViewModel {
        val conversationSid = requireArguments().getString(ARGUMENT_CONVERSATION_SID)!!
        injector.createMessageListViewModel(applicationContext, conversationSid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogMessageActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = messageListViewModel.selectedMessage ?: run {
            dismiss()
            return
        }

        val reactionsView = binding.editReactions.root
        reactionsView.reactions = message.reactions
        messageListViewModel.selfUser.observe(this) { reactionsView.identity = it.identity }

        binding.copy.visibility = if (message.type == MEDIA) GONE else VISIBLE

        messageListViewModel.isWebChat.observe(this){ isWebChat ->
            if(isWebChat.toLowerCase() == "true") {
                if(message.author == Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!) {
                    binding.delete.visibility = View.VISIBLE
                }
            }
            else{
                binding.delete.visibility = View.GONE
            }
        }

        reactionsView.onChangeListener = {
            messageListViewModel.setReactions(reactionsView.reactions)
            dismiss()
        }

        binding.copy.setOnClickListener {
            messageListViewModel.copyMessageToClipboard()
            dismiss()
        }

        binding.share.setOnClickListener {
            shareMessage(message)
            dismiss()
        }

        binding.delete.setOnClickListener {
            messageListViewModel.onShowRemoveMessageDialog.call()
            dismiss()
        }
    }

    private fun shareMessage(message: MessageListViewItem) {
        val intent = Intent(Intent.ACTION_SEND)

        if (message.type == MEDIA) {
            intent.type = message.mediaType
            val uri = message.mediaUploadUri ?: message.mediaUri ?: return
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, message.body)
        }

        startActivity(Intent.createChooser(intent, null))
    }

    companion object {

        private const val ARGUMENT_CONVERSATION_SID = "ARGUMENT_CONVERSATION_SID"

        fun getInstance(conversationSid: String) = MessageActionsDialog().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_CONVERSATION_SID, conversationSid)
            }
        }
    }
}
