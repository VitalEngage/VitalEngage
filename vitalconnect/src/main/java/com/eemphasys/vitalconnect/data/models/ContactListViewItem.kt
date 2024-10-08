package com.eemphasys.vitalconnect.data.models

import org.json.JSONArray

data class ContactListViewItem(
    val name : String,
    val email : String,
    val number : String,
    val type : String,
    val initials : String,
    val designation : String?,
    val department : String?,
    val customerName : String?,
    val countryCode: String?,
    val isGlobal : Boolean,
    val bpId : String,
    val role : String
)
