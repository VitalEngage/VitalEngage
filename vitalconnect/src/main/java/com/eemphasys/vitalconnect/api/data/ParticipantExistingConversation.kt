package com.eemphasys.vitalconnect.api.data

data class ParticipantExistingConversation(
    val conversationSid : String,
    val conversationName: String,
    val conversationDate:String,
    val participantCount:Int,
    val messagesCount:Int,
    val attributes: Attributes
)


data class Attributes(
    val Designation: String,
    val Department: String,
    val CustomerName: String
)