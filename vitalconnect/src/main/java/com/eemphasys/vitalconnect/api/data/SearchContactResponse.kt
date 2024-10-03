package com.eemphasys.vitalconnect.api.data

data class SearchContactResponse(
    val contactId: Int,
    val fullName : String,
    val mobileNumber : String,
    val createdBy : String,
    val createdOn : String,
    val modifiedBy : String,
    val modifiedOn : String,
    val tenantCode : String,
    val isDeleted : Boolean,
    val totalCount : Int,
    val designation : String,
    val customerName : String,
    val department : String,
    val role : String,
    val bpId: String
)
