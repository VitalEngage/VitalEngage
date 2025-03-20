package com.eemphasys.vitalconnect.adapters

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants.Companion.changeButtonBackgroundColor
import com.eemphasys.vitalconnect.data.models.ParticipantListViewItem
import com.eemphasys.vitalconnect.databinding.RowParticipantItemBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import kotlin.properties.Delegates

class ParticipantListAdapter(private val onParticipantClicked: (participant: ParticipantListViewItem) -> Unit) : RecyclerView.Adapter<ParticipantListAdapter.ViewHolder>() {

    var participants: List<ParticipantListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(ConversationDiff(old, new)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowParticipantItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = participants.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.participant = participants[position]
        val chatParticipant = holder.binding.participant
//        holder.binding.participantItem.setOnClickListener {
//            chatParticipant?.let { onParticipantClicked(it) }
//        }
        if(chatParticipant!!.identity.isNullOrEmpty()) {
            val text = cleanConversationName()

            // Create a SpannableString to hold the text
            val spannableString = SpannableString("$text ")

            // Load your drawable icon (can be from resources)
            val iconDrawable = ContextCompat.getDrawable(holder.binding.root.context, R.drawable.icon_customer_chat_passive) // Replace with icon resource
            iconDrawable?.setBounds(0, 0, iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)  // Set icon size

            // Create an ImageSpan to embed the drawable inside the text
            val imageSpan = ImageSpan(iconDrawable!!, ImageSpan.ALIGN_BASELINE)

            // Append the ImageSpan to the text (at the end, in this case)
            spannableString.setSpan(imageSpan, text.length, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set the SpannableString to your TextView
            holder.binding.participantName.text = spannableString
//            holder.binding.participantName.text = cleanConversationName()
            holder.binding.participantAvatar.text = Constants.getInitials(cleanConversationName().trim { it <= ' '} )
        }else{
            holder.binding.participantName.text = chatParticipant!!.friendlyName
            holder.binding.participantAvatar.text = Constants.getInitials(chatParticipant?.friendlyName!!.trim { it <= ' '} )
        }

        changeButtonBackgroundColor(
            holder.binding.participantAvatar,
            ParticipantColorManager.getColorForParticipant(chatParticipant?.friendlyName!!),
            ParticipantColorManager.getDarkColorForParticipant(chatParticipant?.friendlyName!!)
        )
    }

    class ViewHolder(val binding: RowParticipantItemBinding) : RecyclerView.ViewHolder(binding.root)

    class ConversationDiff(private val oldItems: List<ParticipantListViewItem>,
                           private val newItems: List<ParticipantListViewItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].sid == newItems[newItemPosition].sid
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }


    fun cleanConversationName(): String {
        var name = Constants.getStringFromVitalTextSharedPreferences(AppContextHelper.appContext,"currentConversationName")!!
        return name.replace(Regex("[0-9+]+"), "")
    }
}
