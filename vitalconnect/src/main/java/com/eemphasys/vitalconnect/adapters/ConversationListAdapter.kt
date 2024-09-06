package com.eemphasys.vitalconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.databinding.RowConversationItemBinding
import java.util.Locale
import kotlin.properties.Delegates

class ConversationListAdapter(private val callback: OnConversationEvent) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {
    var conversations: List<ConversationListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(ConversationDiff(old, new)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowConversationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        val context = parent.context
//        if(binding.conversation!!.isWebChat =="true"){
//            binding.conversationType.text = Constants.DEALER_NAME
//            binding.conversationType.setBackgroundResource(R.drawable.bg_dealer)
//            binding.conversationType.setTextColor(ContextCompat.getColor(context,R.color.dealer_name))
//        }
//        else{
//            binding.conversationType.text = Constants.DEALER_NAME
//            binding.conversationType.setBackgroundResource(R.drawable.bg_customer)
//            binding.conversationType.setTextColor(ContextCompat.getColor(context,R.color.customer_name))
//        }
        return ViewHolder(binding)
    }
    override fun getItemCount(): Int = conversations.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        holder.binding.conversation = conversations[position]
        val conversationItem = conversations[position]

        // Bind the conversation item
        holder.binding.conversation = conversationItem

        holder.binding.conversationItem.setOnClickListener {
            holder.binding.conversation?.sid?.let { callback.onConversationClicked(it) }
        }

        if(Constants.SHOW_DEPARTMENT == "false" ) {
            holder.binding.department.visibility = View.GONE
        }
        if(Constants.SHOW_DESIGNATION == "false") {
            holder.binding.designation.visibility = View.GONE
        }

        val context = holder.itemView.context
        if (conversationItem.isWebChat == "true") {
            holder.binding.conversationType.text = Constants.DEALER_NAME
            holder.binding.conversationType.setBackgroundResource(R.drawable.bg_dealer)
            holder.binding.conversationType.setTextColor(ContextCompat.getColor(context, R.color.dealer_name))
        } else {
            holder.binding.conversationType.text = "Customer"
            holder.binding.conversationType.setBackgroundResource(R.drawable.bg_customer)
            holder.binding.conversationType.setTextColor(ContextCompat.getColor(context, R.color.customer_name))
        }

    }
    fun isMuted(position: Int) = conversations[position].isMuted

    class ViewHolder(val binding: RowConversationItemBinding) : RecyclerView.ViewHolder(binding.root)

    class ConversationDiff(private val oldItems: List<ConversationListViewItem>,
                           private val newItems: List<ConversationListViewItem>
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

}

interface OnConversationEvent {

    fun onConversationClicked(conversationSid: String)
}