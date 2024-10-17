package com.eemphasys.vitalconnect.adapters

import android.content.Context
import android.util.Log
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

class ConversationListAdapter(private val callback: OnConversationEvent, private val applicationContext: Context) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true) // Enable stable IDs
    }

    // The list of all conversations
    var allConversations: List<ConversationListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        // Apply filter whenever the list is set or filtered
        conversations = filterConversations(new)
        DiffUtil.calculateDiff(ConversationDiff(old, conversations)).dispatchUpdatesTo(this)
    }

    // The list of conversations to display
    var conversations: List<ConversationListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(ConversationDiff(old, new)).dispatchUpdatesTo(this)
    }
    // Current filter state
    private var filterCriteria: MutableSet<String> = mutableSetOf("All")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RowConversationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = conversations.size

    override fun getItemId(position: Int): Long {
        // Return a unique ID for the item at the given position
        return conversations[position].sid.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversationItem = conversations[position]

        holder.binding.conversation = conversationItem

        holder.binding.conversationItem.setOnClickListener {
            holder.binding.conversation?.sid?.let { callback.onConversationClicked(it) }
        }

        if (Constants.getStringFromVitalTextSharedPreferences(
                applicationContext,
                "showDepartment"
            ) == "false"
        ) {
            holder.binding.department.visibility = View.GONE
        }
        if (Constants.getStringFromVitalTextSharedPreferences(
                applicationContext,
                "showDesignation"
            ) == "false"
        ) {
            holder.binding.designation.visibility = View.GONE
        }
            holder.binding.conversationItem.setOnLongClickListener {
                holder.binding.conversation?.let { conversation ->
                    callback.onConversationLongClicked(conversation)
                }
                true
            }

        holder.binding.participantIcon.setOnClickListener {
            holder.binding.conversation?.let { conversation ->
                callback.onParticipantIconClicked(conversation)
            }
            true
        }



            val context = holder.itemView.context
            if (conversationItem.isWebChat == "true") {
                holder.binding.conversationType.text = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!!
                holder.binding.conversationType.setBackgroundResource(R.drawable.bg_dealer)
                holder.binding.conversationType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.dealer_name
                    )
                )
            } else {
                holder.binding.conversationType.text = "Customer"
                holder.binding.conversationType.setBackgroundResource(R.drawable.bg_customer)
                holder.binding.conversationType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.customer_name_text
                    )
                )
            }
        }
        fun isMuted(position: Int) = conversations[position].isMuted

        fun setFilter(criteria: String, add: Boolean) {
            notifyDataSetChanged()
            if (add) {
                filterCriteria.add(criteria)
                if (criteria != "All") {
                    filterCriteria.remove("All")
                }
            } else {
                filterCriteria.remove(criteria)
                if (filterCriteria.size == 0) {
                    filterCriteria.add("All")
                }
            }
            conversations = filterConversations(allConversations)
            notifyDataSetChanged()
        }

        private fun filterConversations(conversations: List<ConversationListViewItem>): List<ConversationListViewItem> {
            val filteredConversations = conversations.filter { conversation ->
                if (filterCriteria.contains(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!!) && filterCriteria.contains("Customer") && filterCriteria.size == 2) {
                    filterCriteria.all { criteria ->
                        when ("All") {
                            "All" -> conversation.sid != ""
                            else -> true
                        }
                    }
                } else {
                    filterCriteria.all { criteria ->
                        when (criteria) {
                            "All" -> conversation.sid != ""
                            "Unread" -> conversation.unreadMessageCount != "0"
                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!! -> conversation.isWebChat == "true"
                            "Customer" -> conversation.isWebChat != "true"
                            else -> true
                        }
                    }
                }
            }
            return filteredConversations.sortedWith(compareByDescending { it.isPinned })
        }

        class ViewHolder(val binding: RowConversationItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                // Handle long click events
                itemView.setOnLongClickListener {
                    binding.conversation?.let { conversation ->
                        (itemView.context as? OnConversationEvent)?.onConversationLongClicked(
                            conversation
                        )
                    }
                    true
                }

                binding.participantIcon.setOnClickListener {
                    binding.conversation?.let { conversation ->
                        (itemView.context as? OnConversationEvent)?.onParticipantIconClicked(conversation)
                    }
                }
            }
        }

        class ConversationDiff(
            private val oldItems: List<ConversationListViewItem>,
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
    fun onConversationLongClicked(conversation: ConversationListViewItem)

    fun onParticipantIconClicked(conversation: ConversationListViewItem)
}