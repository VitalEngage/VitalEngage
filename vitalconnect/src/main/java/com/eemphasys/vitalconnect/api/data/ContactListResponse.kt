package com.eemphasys.vitalconnect.api.data

import com.google.gson.annotations.SerializedName

data class ContactListResponse(
    val totalCount : Int,
    val contacts: List<Contacts>
)

data class Contacts(
     val contactId : Int,
     val fullName :    String ,
     val mobileNumber :   String ,
     val createdBy :   String ,
     val createdOn :  String ,
     val modifiedBy :   String ,
     val modifiedOn :  String ,
     val tenantCode :   String ,
     val isDeleted : Boolean,
     val designation :   String ,
     val customerName :   String ,
     val department :   String ,
     val contactCode :   String
)

//data class ContactListResponse(
//     @SerializedName("totalCount") val totalCount: Int,
//     @SerializedName("contacts") val contacts: List<Contact>
//)
//
//data class Contact(
//     @SerializedName("contactId") val contactId: Int,
//     @SerializedName("fullName") val fullName: String,
//     @SerializedName("mobileNumber") val mobileNumber: String,
//     @SerializedName("createdBy") val createdBy: String,
//     @SerializedName("createdOn") val createdOn: String,
//     @SerializedName("modifiedBy") val modifiedBy: String,
//     @SerializedName("modifiedOn") val modifiedOn: String,
//     @SerializedName("tenantCode") val tenantCode: String,
//     @SerializedName("isDeleted") val isDeleted: Boolean,
//     @SerializedName("designation") val designation: String,
//     @SerializedName("customerName") val customerName: String,
//     @SerializedName("department") val department: String,
//     @SerializedName("contactCode") val contactCode: String
//)
