package com.eemphasys.vitalconnect.manager

import android.util.Log
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.twilio.conversations.Attributes
import com.twilio.conversations.Participant
import org.json.JSONObject
import com.twilio.conversations.extensions.addParticipantByAddress
import com.twilio.conversations.extensions.addParticipantByIdentity
import com.twilio.conversations.extensions.getConversation
import com.twilio.conversations.extensions.removeParticipant
import com.twilio.conversations.extensions.waitForSynchronization

interface AutoParticipantListManager {
    suspend fun addChatParticipant(identity: String,conversationSid:String)
    suspend fun addNonChatParticipant(phone: String, proxyPhone: String, friendlyName: String,conversationSid:String)
}
private const val FRIENDLY_NAME_ATTRIBUTE = "friendlyName"
// Non-chat participants don't have associated user object by design, but we still need
// a friendlyName to display in UI
val Participant.friendlyName get() =
    runCatching { attributes.jsonObject?.getString(FRIENDLY_NAME_ATTRIBUTE) }.getOrNull()

class AutoParticipantListManagerImpl(
    private val conversationsClient: ConversationsClientWrapper
) : AutoParticipantListManager {

    override suspend fun addChatParticipant(identity: String, conversationSid:String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()
        conversation.addParticipantByIdentity(identity)
    }

    override suspend fun addNonChatParticipant(phone: String, proxyPhone: String, friendlyName: String,conversationSid: String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()
        Log.d("Conversationsid",conversationSid)
        val json = JSONObject("{ \"$FRIENDLY_NAME_ATTRIBUTE\": \"$friendlyName\" }")
        Log.d("phone",phone)
        Log.d("proxy",proxyPhone)
        try {
            conversation.addParticipantByAddress(phone, proxyPhone, Attributes(json))
        }
        catch ( e: Exception){
            Log.d("exception", e.localizedMessage)
        }

    }

}