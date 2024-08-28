package com.eemphasys.vitalconnect.api.data

data class SearchUsersResponse(
    val userId : Int,
    val culture : String,
    val timeZone : String,
     val tenantCode : String,
     val count : Int,
     val email : String,
     val jwtToken : String,
     val lastActivityDate :  String,
     val vdPath : String,
     val userImgPath : String,
     val isSuperAdmin : Boolean,
     val isNLogTracingEnable : Boolean,
     val showDesignation : Boolean,
     val showDepartment : Boolean,
     val dealerName : String,
     val fullName : String,
     val isDelete : Boolean,
     val createdOn :  String,
     val createdBy : String,
     val modifiedOn :  String,
     val modifiedBy : String,
     val totalCount : Int,
     val isAzureADUser : Boolean,
     val roleId : Int,
     val roleName : String,
     val proxyNumber : String,
     val mobileNumber : String,
     val supervisorId : Int,
     val supervisorName : String,
     val department : String,
     val countryCode : String,
     val enableNotification : Boolean,
     val userName : String
    )
