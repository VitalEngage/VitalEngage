package com.eemphasys.vitalconnect.data.models

data class ConversationListViewItem (
    val sid: String,
    val name: String,
    val participantCount: Int,
    val unreadMessageCount: String,
    val showUnreadMessageCount: Boolean,
    val participatingStatus: Int,
    val lastMessageStateIcon: Int,
    val lastMessageText: String,
    val lastMessageColor: Int,
    val lastMessageDate: String,
    val isMuted: Boolean = false,
    val isLoading: Boolean = false,
    val department : String,
    val designation: String,
    val customer: String,
    val messageCount: Long,
    val isWebChat: String,
    var isPinned : Boolean,
    var role : String,
    var bpId : String
)