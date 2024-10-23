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
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.RowContactItemBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants

class ContactListAdapter(
    private var itemList: List<ContactListViewItem>,
    private val originalList: List<ContactListViewItem>,
    private val applicationContext: Context,
    private val itemClickListener: OnContactItemClickListener
) : RecyclerView.Adapter<ContactListAdapter.ViewHolder>() {

    val sizeChange = SingleLiveEvent<Int>()
    inner class ViewHolder(private val itemBinding: RowContactItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener, View.OnLongClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)


            itemBinding.participantIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val contact = itemList[position]
                    itemClickListener.onParticipantIconClick(contact)
                }
            }
        }

        override fun onClick(view: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val contact = itemList[position]
                itemClickListener.onContactItemClick(contact)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val contact = itemList[position]
                itemClickListener.onContactItemLongClick(contact) // Notify long click
                return true // Return true to indicate the event was handled
            }
            return false
        }

        fun bind(item: ContactListViewItem, isFirst: Boolean) {
            itemBinding.contactName.text = item.name
//            itemBinding.contactNumber.text = item.number
            itemBinding.contactType.text = item.type
            itemBinding.participantIcon.text = item.initials
            itemBinding.designation.text = item.designation
            itemBinding.department.text = "(" + item.department + ")"
            itemBinding.customerName.text = item.customerName

            if(item.type == "SMS"){
                itemBinding.contactNumber.text = item.number
            }else {
                itemBinding.contactNumber.text = item.email
            }

            if (item.department.isNullOrBlank() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDepartment") == "false") {
                itemBinding.department.visibility = View.GONE
            }
            if (item.designation.isNullOrBlank() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDesignation") == "false") {
                itemBinding.designation.visibility = View.GONE
            }
            if (itemBinding.contactNumber.text.isNullOrBlank()) {
                itemBinding.contactNumber.visibility = View.GONE
            }
            if (itemBinding.customerName.text.isNullOrBlank()) {
                itemBinding.customerName.visibility = View.GONE
            }
            if(!item.isGlobal && isFirst && item.type == "SMS"){
                Log.d("default", "${item.name} $isFirst")
                itemBinding.defaultContact.visibility = View.VISIBLE
            }
            else{
                itemBinding.defaultContact.visibility = View.GONE
            }

            changeButtonBackgroundColor(
                itemBinding.participantIcon,
                ParticipantColorManager.getColorForParticipant(item.name),
                ParticipantColorManager.getDarkColorForParticipant(item.name)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = RowContactItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item,position == 0)
    }

    override fun getItemCount(): Int {
        sizeChange.value = itemList.size
        return itemList.size
    }

    // Filter method for search functionality
    fun filter(query: String) {
        itemList = if (query.isEmpty()) {
            originalList
        } else {
            val lowerCaseQuery = query.toLowerCase()
            originalList.filter { contact ->
                contact.name.toLowerCase().contains(lowerCaseQuery) ||
                        contact.number.contains(lowerCaseQuery) || contact.email.contains(lowerCaseQuery)
                // Add more fields to search through if necessary
            }
        }
        notifyDataSetChanged()
    }
    fun getPositionForLetter(letter: Char): Int {
        for (i in itemList.indices) {
            if (itemList[i].name.startsWith(letter, ignoreCase = true)) {
                return i
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun changeButtonBackgroundColor(textView: TextView?, colorid: Int,coloridText: Int) {
        try {
            val background = textView?.background
            if (background is ShapeDrawable) {
                background.paint.color = colorid
                textView.setTextColor(coloridText)
            } else if (background is GradientDrawable) {
                background.setColor(colorid)
                textView.setTextColor(coloridText)
            } else if (background is ColorDrawable) {
                background.color = colorid
                textView.setTextColor(coloridText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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



interface OnContactItemClickListener {
     fun onContactItemClick(contact: ContactListViewItem)
    fun onContactItemLongClick(contact: ContactListViewItem)

    fun onParticipantIconClick(contact: ContactListViewItem)
}