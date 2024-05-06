package com.eemphasys.vitalconnect.api.data

data class UserAlertRequest(
    val userName : String,
    val status : Boolean,
    val tenantCode : String
)
