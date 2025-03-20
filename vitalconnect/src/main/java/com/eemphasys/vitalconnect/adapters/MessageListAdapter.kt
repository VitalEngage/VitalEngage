package com.eemphasys.vitalconnect.adapters

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.format.Formatter
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.common.enums.SendStatus
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.enums.Direction
import com.eemphasys.vitalconnect.common.enums.DownloadState.COMPLETED
import com.eemphasys.vitalconnect.common.enums.DownloadState.DOWNLOADING
import com.eemphasys.vitalconnect.common.enums.DownloadState.ERROR
import com.eemphasys.vitalconnect.common.enums.DownloadState.NOT_STARTED
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.databinding.RowMessageItemIncomingBinding
import com.eemphasys.vitalconnect.databinding.RowMessageItemOutgoingBinding
import com.eemphasys.vitalconnect.databinding.ViewReactionItemBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants

class MessageListAdapter(
    private val onDisplaySendError: (message: MessageListViewItem) -> Unit,
    private val onDownloadMedia: (message: MessageListViewItem) -> Unit,
    private val onOpenMedia: (location: Uri, mimeType: String) -> Unit,
    private val onItemLongClick: (messageIndex: Long) -> Unit,
    private val onReactionClicked: (messageIndex: Long) -> Unit

) : PagedListAdapter<MessageListViewItem, MessageListAdapter.ViewHolder>(MESSAGE_COMPARATOR) {

//    init {
//        setHasStableIds(true) // Enable stable IDs
//    }
//    override fun getItemId(position: Int): Long {
//        return getItem(position)?.sid?.hashCode()?.toLong() ?: position.toLong()
//    }

    fun getMessage(position: Int): MessageListViewItem? {
        return getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.direction?.value ?: Direction.OUTGOING.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {

            Direction.INCOMING.value ->
                RowMessageItemIncomingBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            else -> RowMessageItemOutgoingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        if (message == null) {
            return
        }

        val binding = holder.binding
        val context = binding.root.context

        val mediaSize = Formatter.formatShortFileSize(context, message.mediaSize ?: 0)
        val mediaUploadedBytes = Formatter.formatShortFileSize(context, message.mediaUploadedBytes ?: 0)
        val mediaDownloadedBytes = Formatter.formatShortFileSize(context, message.mediaDownloadedBytes ?: 0)

        val attachmentInfoText = when {
            message.sendStatus == SendStatus.ERROR -> context.getString(R.string.err_failed_to_upload_media)

            message.mediaUploading -> context.getString(R.string.attachment_uploading, mediaUploadedBytes)

            message.mediaUploadUri != null ||
                    message.mediaDownloadState == COMPLETED -> context.getString(R.string.attachment_tap_to_open)

            message.mediaDownloadState == NOT_STARTED -> mediaSize

            message.mediaDownloadState == DOWNLOADING -> context.getString(
                R.string.attachment_downloading,
                mediaDownloadedBytes
            )

            message.mediaDownloadState == ERROR -> context.getString(R.string.err_failed_to_download_media)

            else -> error("Never happens")
        }

        val attachmentInfoColor = when {
            message.sendStatus == SendStatus.ERROR ||
                    message.mediaDownloadState == ERROR -> ContextCompat.getColor(context, R.color.colorEet)

            message.mediaUploading -> ContextCompat.getColor(context, R.color.text_subtitle)

            message.mediaUploadUri != null ||
                    message.mediaDownloadState == COMPLETED -> ContextCompat.getColor(context, R.color.colorPrimary)

            else -> ContextCompat.getColor(context, R.color.text_subtitle)
        }

        val attachmentOnClickListener = View.OnClickListener {
            if (message.mediaDownloadState == COMPLETED && message.mediaUri != null) {
                onOpenMedia(message.mediaUri, message.mediaType!!)
            } else if (message.mediaUploadUri != null) {
                onOpenMedia(message.mediaUploadUri, message.mediaType!!)
            } else if (message.mediaDownloadState != DOWNLOADING) {
                onDownloadMedia(message)
            }
        }

        val longClickListener = View.OnLongClickListener {
            onItemLongClick(message.index)
            return@OnLongClickListener true
        }

        binding.root.setOnLongClickListener(longClickListener)

        if (message.sendStatus == SendStatus.ERROR) {
            binding.root.setOnClickListener {
                onDisplaySendError(message)
            }
        }

        when (binding) {
            is RowMessageItemIncomingBinding -> {
                binding.message = message
                addReactions(binding.messageReactionHolder, message)
                binding.attachmentInfo.text = attachmentInfoText
                binding.attachmentInfo.setTextColor(attachmentInfoColor)
                binding.attachmentBackground.setOnClickListener(attachmentOnClickListener)
                binding.attachmentBackground.setOnLongClickListener(longClickListener)

                if(Constants.isExternalContact( message.author)){
                    val text = cleanConversationName()

                    // Create a SpannableString to hold the text
                    val spannableString = SpannableString("$text ")

                    // Load your drawable icon (can be from resources)
                    val iconDrawable = ContextCompat.getDrawable(context, R.drawable.icon_customer_chat_passive) // Replace with icon resource
                    iconDrawable?.setBounds(0, 0, iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)  // Set icon size

                    // Create an ImageSpan to embed the drawable inside the text
                    val imageSpan = ImageSpan(iconDrawable!!, ImageSpan.ALIGN_BASELINE)

                    // Append the ImageSpan to the text (at the end, in this case)
                    spannableString.setSpan(imageSpan, text.length, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // Set the SpannableString to your TextView
                    binding.messageAuthor.text = spannableString
//                    binding.messageAuthor.text = cleanConversationName()
                    binding.participantIcon.text = Constants.getInitials(cleanConversationName().trim { it <= ' '} )
                }else {
                    if(message.friendlyName == ""){
                        binding.messageAuthor.text = message.author
                        binding.participantIcon.text = Constants.getInitials(message.author.trim { it <= ' '} )
                    }else {
                        binding.messageAuthor.text = message.friendlyName
                        binding.participantIcon.text = Constants.getInitials(message.friendlyName.trim { it <= ' '} )
                    }
                }




                Constants.changeButtonBackgroundColor(
                    binding.participantIcon,
                    ParticipantColorManager.getColorForParticipant(message.author),
                    ParticipantColorManager.getDarkColorForParticipant(message.author)
                )
            }
            is RowMessageItemOutgoingBinding -> {
                binding.message = message
                addReactions(binding.messageReactionHolder, message)
                binding.attachmentInfo.text = attachmentInfoText
                binding.attachmentInfo.setTextColor(attachmentInfoColor)
                binding.attachmentBackground.setOnClickListener(attachmentOnClickListener)
                binding.attachmentBackground.setOnLongClickListener(longClickListener)
            }
            else -> error("Unknown binding type: $binding")
        }

    }

    private fun addReactions(rootView: LinearLayout, message: MessageListViewItem) {
        rootView.setOnClickListener { onReactionClicked(message.index) }
        rootView.removeAllViews()
        message.reactions.forEach { reaction ->
            if (reaction.value.isNotEmpty()) {
                val emoji = ViewReactionItemBinding.inflate(LayoutInflater.from(rootView.context))
                emoji.emojiIcon.setText(reaction.key.emoji)
                emoji.emojiCounter.text = reaction.value.size.toString()

                val color = if (message.direction == Direction.OUTGOING) R.color.white else R.color.colorPrimary
                emoji.emojiCounter.setTextColor(ContextCompat.getColor(rootView.context, color))

                rootView.addView(emoji.root)
            }
        }
    }

    class ViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    fun cleanConversationName(): String {
        var name = Constants.getStringFromVitalTextSharedPreferences(AppContextHelper.appContext,"currentConversationName")!!
        return name.replace(Regex("[0-9+]+"), "")
    }
    companion object {
        val MESSAGE_COMPARATOR = object : DiffUtil.ItemCallback<MessageListViewItem>() {
            override fun areContentsTheSame(oldItem: MessageListViewItem, newItem: MessageListViewItem) =
                oldItem == newItem

            override fun areItemsTheSame(oldItem: MessageListViewItem, newItem: MessageListViewItem) =
                oldItem.sid == newItem.sid
        }
    }
}
