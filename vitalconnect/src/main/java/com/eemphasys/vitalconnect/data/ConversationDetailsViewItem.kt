package com.eemphasys.vitalconnect.data

data class ConversationDetailsViewItem(
    val conversationSid: String,
    val conversationName: String,
    val createdBy: String,
    val dateCreated: String,
    val isMuted: Boolean = false
)
