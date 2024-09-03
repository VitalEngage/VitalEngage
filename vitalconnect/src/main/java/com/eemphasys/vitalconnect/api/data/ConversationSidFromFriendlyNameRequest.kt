package com.eemphasys.vitalconnect.api.data

data class ConversationSidFromFriendlyNameRequest(
    val tenantCode : String,
    val currentUser : String,
    val searchCriteria : String
)
