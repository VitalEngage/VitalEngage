package com.eemphasys.vitalconnect.api.data

data class ParticipantExistingConversation(
    val conversationSid : String,
    val conversationName: String,
    val conversationDate:String,
    val participantCount:Int,
    val messagesCount:Int
)
