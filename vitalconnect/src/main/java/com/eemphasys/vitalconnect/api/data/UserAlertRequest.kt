package com.eemphasys.vitalconnect.api.data

data class UserAlertRequest(
    val userName : String,
    val status : String,
    val tenantCode : String
)
