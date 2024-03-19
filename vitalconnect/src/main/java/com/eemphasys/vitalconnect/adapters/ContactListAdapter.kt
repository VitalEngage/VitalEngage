package com.eemphasys.vitalconnect.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.RowContactItemBinding

class ContactListAdapter(val itemList : List<ContactListViewItem>,private val itemClickListener: OnContactItemClickListener) : RecyclerView.Adapter<ContactListAdapter.ViewHolder>(){

    inner class ViewHolder(val itemBinding : RowContactItemBinding)
        : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener{

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

            fun bind(item: ContactListViewItem){
                itemBinding.contactName.text = item.name + "  " + item.number
                itemBinding.contactType.text = item.type

            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowContactItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }


}


interface OnContactItemClickListener {
     fun onContactItemClick(contact: ContactListViewItem)
}