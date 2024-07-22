package com.eemphasys.vitalconnect.api.data

data class UpdatePasswordReq(
    val tenantCode : String,
    val userName : String,
    val password : String,
    val otp : String
)
