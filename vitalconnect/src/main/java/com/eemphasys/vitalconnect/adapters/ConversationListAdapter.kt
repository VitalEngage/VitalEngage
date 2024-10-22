package com.eemphasys.vitalconnect.adapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.databinding.RowConversationItemBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import kotlin.properties.Delegates

class ConversationListAdapter(private val callback: OnConversationEvent, private val applicationContext: Context) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true) // Enable stable IDs
    }
    val type = object : TypeToken<ArrayList<String>>() {}.type
    var jsonString = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"contextList")

    val contextItems: ArrayList<String> = if(jsonString.isNullOrEmpty()){
        arrayListOf()
    }
    else Gson().fromJson(jsonString, type)
    // The list of all conversations
    var allConversations: List<ConversationListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        // Apply filter whenever the list is set or filtered
        conversations = filterConversations(new,contextItems)
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
                holder.binding.participantIcon.text = ""
                holder.binding.participantIcon.setBackgroundResource(R.drawable.icon_multi_user_conversation)
            } else {
                holder.binding.conversationType.text = "Customer"
                holder.binding.conversationType.setBackgroundResource(R.drawable.bg_customer)
                holder.binding.conversationType.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.customer_name_text
                    )
                )
                holder.binding.participantIcon.setBackgroundResource(R.drawable.bg_participant_icon)
                changeButtonBackgroundColor(holder.binding.participantIcon,ParticipantColorManager.getColorForParticipant(conversationItem.name))
                holder.binding.participantIcon.text = Constants.getInitials(conversationItem.name.trim { it <= ' '} )
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
            conversations = filterConversations(allConversations,contextItems)
            notifyDataSetChanged()
        }

        private fun filterConversations(conversations: List<ConversationListViewItem>,contextItems: ArrayList<String>): List<ConversationListViewItem> {
           if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext") == "true") {
               filterCriteria.remove("All")
               filterCriteria.add("CONTEXT")
           }
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
                            "CONTEXT" -> contextItems.any { contextItem ->
                                conversation.name.contains(contextItem, ignoreCase = true)
                            } // Check for CONTEXT
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