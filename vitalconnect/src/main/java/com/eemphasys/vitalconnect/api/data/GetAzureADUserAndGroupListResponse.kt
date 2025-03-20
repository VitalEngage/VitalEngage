package com.eemphasys.vitalconnect.api.data

data class GetAzureADUserAndGroupListResponse(
    val objectId: String,
    val userName: String,
    val loginName: String?,
    val fullName: String,
    val firstName: String?,
    val lastName: String?,
    val emailId: String?,
    val isGroup: Boolean
)
