package com.eemphasys.vitalconnect.api.data

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
