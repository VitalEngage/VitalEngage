package com.eemphasys.vitalconnect.data.models

sealed class ListItem {
    data class ContactListViewItem(
        val name : String,
        val email : String,
        val number : String
    ) : ListItem()
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
        val isLoading: Boolean = false
    ) : ListItem()
}