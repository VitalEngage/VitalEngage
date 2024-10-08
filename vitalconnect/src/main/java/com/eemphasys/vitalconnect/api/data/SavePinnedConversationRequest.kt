package com.eemphasys.vitalconnect.api.data

data class SavePinnedConversationRequest(
       val currentUser   : String   ,
       val conversationList   : ArrayList<String>,
       val tenantCode   : String
)
