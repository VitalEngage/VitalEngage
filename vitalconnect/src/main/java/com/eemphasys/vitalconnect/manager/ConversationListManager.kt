package com.eemphasys.vitalconnect.manager

import com.eemphasys.vitalconnect.common.extensions.muteConversation
import com.eemphasys.vitalconnect.common.extensions.setFriendlyName
import com.eemphasys.vitalconnect.common.extensions.unmuteConversation
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.twilio.conversations.extensions.createConversation
import com.twilio.conversations.extensions.destroy
import com.twilio.conversations.extensions.getConversation
import com.twilio.conversations.extensions.join
import com.twilio.conversations.extensions.leave

interface ConversationListManager {
    suspend fun createConversation(friendlyName: String): String
    suspend fun joinConversation(conversationSid: String)
    suspend fun removeConversation(conversationSid: String)
    suspend fun leaveConversation(conversationSid: String)
    suspend fun muteConversation(conversationSid: String)
    suspend fun unmuteConversation(conversationSid: String)
    suspend fun renameConversation(conversationSid: String, friendlyName: String)
}

class ConversationListManagerImpl(private val conversationsClient: ConversationsClientWrapper) : ConversationListManager {

    override suspend fun createConversation(friendlyName: String): String
            = conversationsClient.getConversationsClient().createConversation(friendlyName).sid

    override suspend fun joinConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).join()

    override suspend fun removeConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).destroy()

    override suspend fun leaveConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).leave()

    override suspend fun muteConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).muteConversation()

    override suspend fun unmuteConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).unmuteConversation()

    override suspend fun renameConversation(conversationSid: String, friendlyName: String)
            = conversationsClient.getConversationsClient().getConversation(conversationSid).setFriendlyName(friendlyName)

}