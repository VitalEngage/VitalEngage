package com.eemphasys.vitalconnect.api.data

data class UserListResponse(
     val userId : Int,
     val userSID : String,
     val fullName :  String ,
     val createdOn :  String ,
     val createdBy :  String ,
     val modifiedOn :  String ,
     val modifiedBy :  String ,
     val totalCount : Int,
     val department :  String ,
     val userName :  String,
     val email : String
)
