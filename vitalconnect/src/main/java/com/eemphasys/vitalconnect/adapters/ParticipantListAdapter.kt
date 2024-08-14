package com.eemphasys.vitalconnect.adapters

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.AppContextHelper
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
        holder.binding.participantItem.setOnClickListener {
            chatParticipant?.let { onParticipantClicked(it) }
        }
        holder.binding.participantAvatar.text = Constants.getInitials(chatParticipant?.friendlyName!!.trim { it <= ' '} )

        changeButtonBackgroundColor(
            holder.binding.participantAvatar,
            ParticipantColorManager.getColorForParticipant(chatParticipant?.friendlyName!!)
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

    private fun changeButtonBackgroundColor(textView: TextView?, colorid: Int) {
        try {
            val background = textView!!.background
            if (background is ShapeDrawable) {
                background.paint.color = colorid
            } else if (background is GradientDrawable) {
                background.setColor(colorid)
            } else if (background is ColorDrawable) {
                background.color = colorid
            }
        } catch (e: Exception) {
            EETLog.error(
                AppContextHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
    }
}
