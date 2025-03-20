package com.eemphasys.vitalconnect.adapters
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.Constants.Companion.changeButtonBackgroundColor
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.RowContactItemBinding

class SuggestionAdapter(
    private val applicationContext: Context,
    private val suggestions: List<ContactListViewItem>,
    private val itemClickListener: (ContactListViewItem) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    inner class ViewHolder(private val itemBinding: RowContactItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: ContactListViewItem) {
            itemBinding.contactName.text = item.name
            itemBinding.contactType.text = item.type
            itemBinding.participantIcon.text = item.initials
            itemBinding.designation.text = item.designation
            itemBinding.department.text = "(" + item.department + ")"
            itemBinding.customerName.text = item.customerName

//            if(item.type == "SMS"){
//                itemBinding.contactNumber.text = item.number
//            }else {
//                itemBinding.contactNumber.text = item.email
//            }
            itemBinding.contactNumber.text = ""

            if (item.type == "SMS") {
                if (!item.number.isNullOrBlank()) {
                    itemBinding.contactNumber.text = item.number
                    itemBinding.contactNumber.visibility = View.VISIBLE
                } else {
                    itemBinding.contactNumber.visibility = View.GONE
                }
            } else {
                if (!item.email.isNullOrBlank()) {
                    itemBinding.contactNumber.text = item.email
                    itemBinding.contactNumber.visibility = View.VISIBLE
                } else {
                    itemBinding.contactNumber.visibility = View.GONE
                }
            }
            if (item.department.isNullOrBlank() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDepartment")!! == "false") {
                itemBinding.department.visibility = View.GONE
            }
            else{
                itemBinding.department.visibility = View.VISIBLE
            }
            if (item.designation.isNullOrBlank() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDesignation")!! == "false") {
                itemBinding.designation.visibility = View.GONE
            }
            else{
                itemBinding.designation.visibility = View.VISIBLE
            }
            if (itemBinding.contactNumber.text.isNullOrBlank()) {
                itemBinding.contactNumber.visibility = View.GONE
            }
            else{
                itemBinding.contactNumber.visibility = View.VISIBLE
            }
            if (itemBinding.customerName.text.isNullOrBlank()) {
                itemBinding.customerName.visibility = View.GONE
            }
            else{
                itemBinding.customerName.visibility = View.VISIBLE
            }

            changeButtonBackgroundColor(
                itemBinding.participantIcon,
                ParticipantColorManager.getColorForParticipant(item.name),
                ParticipantColorManager.getDarkColorForParticipant(item.name)
            )
        }
//        val textView: TextView = itemView.findViewById(android.R.id.text1)

        init {
            itemView.setOnClickListener {
                itemClickListener(suggestions[adapterPosition])
            }
        }
    }

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
//        return ViewHolder(view)
//    }
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionAdapter.ViewHolder {
    val itemBinding = RowContactItemBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
    )
    return ViewHolder(itemBinding)
}

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = suggestions[position]
//        holder. = contact.name
        holder.bind(contact)
    }

    override fun getItemCount() = suggestions.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Reset the view state here if necessary
    }
}
