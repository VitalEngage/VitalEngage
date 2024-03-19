package com.eemphasys.vitalconnect.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.MessageListAdapter
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.enums.MessageType
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.extensions.onSubmit
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.extensions.showToast
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.databinding.ActivityMessageListBinding
import com.eemphasys.vitalconnect.ui.dialogs.AttachFileDialog
import com.eemphasys.vitalconnect.ui.dialogs.MessageActionsDialog
import com.eemphasys.vitalconnect.ui.dialogs.ReactionDetailsDialog

class MessageListActivity: AppCompatActivity() {

        val binding by lazy { ActivityMessageListBinding.inflate(layoutInflater) }

        val messageListViewModel by lazyViewModel {
            injector.createMessageListViewModel(applicationContext, intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            //Timber.d("onCreate")
            try{
                super.onCreate(savedInstanceState)
                setContentView(binding.root)

                initViews()}
            catch(e : Exception){
                println("${e.message}")

            }

        }

        override fun onCreateOptionsMenu(menu: Menu): Boolean {
            super.onCreateOptionsMenu(menu)
            menuInflater.inflate(R.menu.menu_message_list, menu)
            return true
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.show_conversation_details -> ConversationDetailsActivity.start(
                    this,
                    messageListViewModel.conversationSid
                )
            }
            return super.onOptionsItemSelected(item)
        }

        private fun initViews() {
            setSupportActionBar(binding.conversationToolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.conversationToolbar.setNavigationOnClickListener { onBackPressed() }
            val adapter = MessageListAdapter(
                onDisplaySendError = { message ->
                    //Timber.d("Display send error clicked: ${message.uuid}")
                    showSendErrorDialog(message)
                },
                onDownloadMedia = { message ->
                    //Timber.d("Download clicked: $message")
                    messageListViewModel.startMessageMediaDownload(message.index, message.mediaFileName)
                },
                onOpenMedia = { uri, mimeType ->
                    //Timber.d("Open clicked")
                    if (mimeType.startsWith("image/")) {
                        viewUri(uri)
                    } else {
                        shareUri(uri, mimeType)
                    }
                },
                onItemLongClick = { messageIndex ->
                    //Timber.d("Message long clicked: $messageIndex")
                    messageListViewModel.selectedMessageIndex = messageIndex
                    showMessageActionsDialog()
                },
                onReactionClicked = { messageIndex ->
                    //Timber.d("Reaction clicked:, $messageIndex")
                    messageListViewModel.selectedMessageIndex = messageIndex
                    showReactionDetailsDialog()
                }
            )
            binding.messageList.adapter = adapter
            binding.messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val index = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    if (index == RecyclerView.NO_POSITION) return

                    val message = adapter.getMessage(index)
                    message?.let { messageListViewModel.handleMessageDisplayed(it.index) }
                }
            })
            binding.messageInput.onSubmit {
                sendMessage()
            }
            binding.messageInputHolder.setEndIconOnClickListener {
                sendMessage()
            }
            binding.messageInput.doAfterTextChanged {
                messageListViewModel.typing()
            }
            messageListViewModel.conversationName.observe(this) { conversationName ->
                title = conversationName
            }
            messageListViewModel.messageItems.observe(this) { messages ->
                val lastVisibleMessageIndex =
                    (binding.messageList.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                // Scroll list to bottom when it was at the bottom before submitting new messages
                val commitCallback: Runnable? = if (lastVisibleMessageIndex == adapter.itemCount - 1) {
                    Runnable { binding.messageList.scrollToPosition(adapter.itemCount - 1) }
                } else {
                    null
                }
                adapter.submitList(messages, commitCallback)
            }
            messageListViewModel.onMessageCopied.observe(this) {
                binding.messageListLayout.showSnackbar(R.string.message_copied, R.id.messageInputHolder)
            }
            messageListViewModel.onShowRemoveMessageDialog.observe(this) {
                showRemoveMessageDialog()
            }
            messageListViewModel.onMessageRemoved.observe(this) {
                binding.messageListLayout.showSnackbar(R.string.message_deleted, R.id.messageInputHolder)
            }
            messageListViewModel.onMessageError.observe(this) { error ->
                if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                    finish()
                }
                if (error == ConversationsError.MESSAGE_SEND_FAILED) { // shown in message list inline
                    return@observe
                }
                binding.messageListLayout.showSnackbar(getErrorMessage(error), R.id.messageInputHolder)
            }
            messageListViewModel.typingParticipantsList.observe(this) { participants ->
                binding.typingIndicator.visibility = if (participants.isNotEmpty()) View.VISIBLE else View.GONE
                val text = if (participants.size == 1) participants[0] else participants.size.toString()
                binding.typingIndicator.text =
                    resources.getQuantityString(R.plurals.typing_indicator, participants.size, text)
            }
            binding.messageAttachmentButton.setOnClickListener {
                AttachFileDialog.getInstance(messageListViewModel.conversationSid)
                    .showNow(supportFragmentManager, null)
            }
        }

        private fun showRemoveMessageDialog() {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.remove_messgage_dialog_title)
                .setMessage(R.string.remove_messgage_dialog_message)
                .setPositiveButton(R.string.close, null)
                .setNegativeButton(R.string.delete) { _, _ -> messageListViewModel.removeMessage() }
                .create()

            dialog.setOnShowListener {
                val color = ContextCompat.getColor(this, R.color.colorAccent)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)

                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
            }

            dialog.show()
        }

        private fun showSendErrorDialog(message: MessageListViewItem) {
            val title = getString(R.string.send_error_dialog_title, message.errorCode)

            val text = when (message.errorCode) { // See https://www.twilio.com/docs/api/errors
                50511 -> getString(R.string.send_error_dialog_invalid_media_content_type)
                else -> getString(R.string.send_error_dialog_message_default)
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton(R.string.close, null)
                .setNegativeButton(R.string.retry) { _, _ -> resendMessage(message) }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
            }

            dialog.show()
        }

        private fun sendMessage() {
            binding.messageInput.text.toString().takeIf { it.isNotBlank() }?.let { message ->
                //Timber.d("Sending message: $message")
                messageListViewModel.sendTextMessage(message)
                binding.messageInput.text?.clear()
            }
        }

        private fun resendMessage(message: MessageListViewItem) {
            if (message.type == MessageType.TEXT) {
                messageListViewModel.resendTextMessage(message.uuid)
            } else if (message.type == MessageType.MEDIA) {
                val fileInputStream = message.mediaUploadUri?.let { contentResolver.openInputStream(it) }
                if (fileInputStream != null) {
                    messageListViewModel.resendMediaMessage(fileInputStream, message.uuid)
                } else {
                    //Timber.w("Could not get input stream for file reading: ${message.mediaUploadUri}")
                    showToast(R.string.err_failed_to_resend_media)
                }
            }
        }

        private fun viewUri(uri: Uri) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, null))
        }

        private fun shareUri(uri: Uri, mimeType: String) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = mimeType
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, null))
        }

        private fun showMessageActionsDialog() {
            MessageActionsDialog.getInstance(messageListViewModel.conversationSid)
                .showNow(supportFragmentManager, null)
        }

        private fun showReactionDetailsDialog() {
            ReactionDetailsDialog.getInstance(messageListViewModel.conversationSid)
                .showNow(supportFragmentManager, null)
        }

        companion object {

            private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

            fun start(context: Context, conversationSid: String) =
                context.startActivity(getStartIntent(context, conversationSid))

            fun getStartIntent(context: Context, conversationSid: String) =
                Intent(context, MessageListActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)

            fun startfromFragment(context: Context, conversationSid: String) =
                context.startActivity(getStartIntentFromFragment(context, conversationSid))

            fun getStartIntentFromFragment(context: Context, conversationSid: String) =
                Intent(context, MessageListActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }