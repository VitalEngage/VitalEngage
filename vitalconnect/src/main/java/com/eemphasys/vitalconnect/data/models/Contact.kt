package com.eemphasys.vitalconnect.data.models

import org.json.JSONArray

data class Contact(
    val name: String,
    val number: String,
    val customerName: String,
    val initials: String,
    val designation: String,
    val department: String,
    val customer: String,
    val countryCode: String,
    val bpId: String,
    val role : String
)