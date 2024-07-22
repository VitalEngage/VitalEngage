package com.eemphasys.vitalconnect.adapters

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.RowContactItemBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants

class ContactListAdapter(
    private var itemList: List<ContactListViewItem>,
    private val originalList: List<ContactListViewItem>,
    private val itemClickListener: OnContactItemClickListener
) : RecyclerView.Adapter<ContactListAdapter.ViewHolder>() {

    inner class ViewHolder(private val itemBinding: RowContactItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val contact = itemList[position]
                itemClickListener.onContactItemClick(contact)
            }
        }

        fun bind(item: ContactListViewItem) {
            itemBinding.contactName.text = item.name
            itemBinding.contactNumber.text = item.number
            itemBinding.contactType.text = item.type
            itemBinding.participantIcon.text = item.initials
            itemBinding.designation.text = item.designation
            itemBinding.department.text = "(" + item.department + ")"
            itemBinding.customerName.text = item.customerName

            if (itemBinding.department.text.isNullOrBlank() || Constants.SHOW_DEPARTMENT == "false") {
                itemBinding.department.visibility = View.GONE
            }
            if (itemBinding.designation.text.isNullOrBlank() || Constants.SHOW_DESIGNATION == "false") {
                itemBinding.designation.visibility = View.GONE
            }
            if (itemBinding.contactNumber.text.isNullOrBlank()) {
                itemBinding.contactNumber.visibility = View.GONE
            }
            if (itemBinding.customerName.text.isNullOrBlank()) {
                itemBinding.customerName.visibility = View.GONE
            }

            if (itemBinding.contactName.text == Constants.CUSTOMER_NAME) {
                itemBinding.defaultContact.visibility = View.VISIBLE
            }

            changeButtonBackgroundColor(
                itemBinding.participantIcon,
                ParticipantColorManager.getColorForParticipant(item.name)
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
        holder.bind(item)
    }

    override fun getItemCount(): Int {
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
                        contact.number.contains(lowerCaseQuery)
                // Add more fields to search through if necessary
            }
        }
        notifyDataSetChanged()
    }

    private fun changeButtonBackgroundColor(textView: TextView?, colorid: Int) {
        try {
            val background = textView?.background
            if (background is ShapeDrawable) {
                background.paint.color = colorid
            } else if (background is GradientDrawable) {
                background.setColor(colorid)
            } else if (background is ColorDrawable) {
                background.color = colorid
            }
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            )
        }
    }
}



interface OnContactItemClickListener {
     fun onContactItemClick(contact: ContactListViewItem)
}