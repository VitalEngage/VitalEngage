package com.eemphasys.vitalconnect.api.data

data class ContactListRequest(
     val pageIndex : Int,
     val pageSize : Int,
     val searchCriteria :   String ,
     val sorting :   String ,
     val sortOrder :   String ,
     val tenantCode :   String ,
     val currentUser :   String ,
     val roleId : Int
)
