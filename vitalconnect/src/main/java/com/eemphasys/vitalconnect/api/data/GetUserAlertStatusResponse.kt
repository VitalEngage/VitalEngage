package com.eemphasys.vitalconnect.api.data

data class GetUserAlertStatusResponse(
    val userName  :  String,
    val status : String,
    val tenantCode  : String
)
