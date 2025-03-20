package com.eemphasys.vitalconnect.api.data

data class AddAzureAdParticipantConversationRequest(
    val TenantCode: String?,
    val CurrentUser: String?,
    val Identity: String?,
    val Fullname: String?,
    val GroupId: String?,
    val AutoRegistration: String?,
    val ProxyNumber: String?,
    val ConversationSid: String?,
    val ConversationName: String?,
    val IsWebToWeb: String?
)
