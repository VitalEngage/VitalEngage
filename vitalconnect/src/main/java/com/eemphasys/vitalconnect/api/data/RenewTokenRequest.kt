package com.eemphasys.vitalconnect.api.data

data class RenewTokenRequest(
    val refreshToken: String,
    val currentUser: String,
    val tenantCode: String
)
