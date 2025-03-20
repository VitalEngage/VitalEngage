package com.eemphasys.vitalconnect.api.data

data class GetAzureADUserAndGroupListRequest(
    val TenantCode: String,
    val CurrentUser: String,
    val SearchUser: String
)
