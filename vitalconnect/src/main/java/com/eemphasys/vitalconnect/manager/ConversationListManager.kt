package com.eemphasys.vitalconnect.manager

import android.util.Log
import com.eemphasys.vitalconnect.common.extensions.muteConversation
import com.eemphasys.vitalconnect.common.extensions.setFriendlyName
import com.eemphasys.vitalconnect.common.extensions.unmuteConversation
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.twilio.conversations.Attributes
import com.twilio.conversations.extensions.createConversation
import com.twilio.conversations.extensions.destroy
import com.twilio.conversations.extensions.getConversation
import com.twilio.conversations.extensions.join
import com.twilio.conversations.extensions.leave
import com.twilio.conversations.extensions.setAttributes

interface ConversationListManager {
    suspend fun createConversation(friendlyName: String, attributes: Attributes): String
    suspend fun joinConversation(conversationSid: String)
    suspend fun removeConversation(conversationSid: String)
    suspend fun leaveConversation(conversationSid: String)
    suspend fun muteConversation(conversationSid: String)
    suspend fun unmuteConversation(conversationSid: String)
    suspend fun renameConversation(conversationSid: String, friendlyName: String)

    suspend fun setAttributes(conversationSid: String, attributes: Attributes)

    suspend fun getAttributes(conversationSid: String) : String
}

class ConversationListManagerImpl(private val conversationsClient: ConversationsClientWrapper) : ConversationListManager {

    override suspend fun createConversation(friendlyName: String, attributes: Attributes): String {
        val sid = conversationsClient.getConversationsClient().createConversation(friendlyName).sid
        Log.d("attributes",attributes.toString())
        try {
            conversationsClient.getConversationsClient().getConversation(sid)
                .setAttributes(attributes)
        } catch (e:Exception){
            Log.d("Exception@setAttributes", e.message.toString())
        }
        return sid
    }

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

    override suspend fun setAttributes(conversationSid: String, attributes: Attributes){
        Log.d("attributes",attributes.toString())
        try {
            conversationsClient.getConversationsClient().getConversation(conversationSid)
                .setAttributes(attributes)
        } catch (e:Exception){
            Log.d("Exception@setAttributes", e.message.toString())
        }}

    override suspend fun getAttributes(conversationSid: String) : String  {
         return conversationsClient.getConversationsClient().getConversation(conversationSid)
                .attributes.jsonObject.toString()

    }
}