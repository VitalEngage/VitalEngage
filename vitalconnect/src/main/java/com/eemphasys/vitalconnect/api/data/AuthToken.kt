package com.eemphasys.vitalconnect.api.data

data class AuthToken(
    val userId: Int,
    val userName: String,
    val fullName: String,
    val roleId: Int,
    val culture: String,
    val timeZone: String,
    val tenantCode: String,
    val count: Int,
    val email: String,
    val jwtToken: String,
    val lastActivityDate: String,
    val vdPath: String,
    val userImgPath: String,
    //val userImgData: String,
    val isSuperAdmin: Boolean,
    val isNLogTracingEnable: Boolean,
    val proxyNumber: String,
    val department: String,
    val countryCode: String,
    val enableNotification : Boolean,
    val mobileNumber: String ,
    val showDesignation: Boolean,
    val showDepartment: Boolean,
    val dealerName: String,
    val pinedConversation: ArrayList<String>
)
