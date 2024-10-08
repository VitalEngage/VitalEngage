package com.eemphasys.vitalconnect.api.data

data class addParticipantToWebConversationRequest(
     val tenantCode :   String ,
     val currentUser :   String ,
     val webUsers : List<webParticipant>,
     val conversationSid :   String ,
     val conversationName :   String ,
     val autoRegistration : Boolean,
     val proxyNumber :   String
)

data class webParticipant(
     val identity :   String ,
     val fullName :   String ,
     val participantSid :   String
)
