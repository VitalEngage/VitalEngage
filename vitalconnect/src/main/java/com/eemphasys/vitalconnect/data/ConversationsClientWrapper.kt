package com.eemphasys.vitalconnect.data

import android.content.Context
import android.util.Log
import com.eemphasys.vitalconnect.common.Constants
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.extensions.createAndSyncConversationsClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ConversationsClientWrapper(private val applicationContext: Context) {

    private var deferredClient = CompletableDeferred<ConversationsClient>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val isClientCreated get() = deferredClient.isCompleted && !deferredClient.isCancelled

    suspend fun getConversationsClient() = deferredClient.await()

    suspend fun getclient(){

        val client = createAndSyncConversationsClient(applicationContext, Constants.TWILIO_TOKEN)
        this.deferredClient.complete(client)

        Log.d("client", client.myIdentity)

    }

    companion object{
        val INSTANCE get() = _instance ?: error("call ConversationsClientWrapper.createInstance() first")

        private var _instance: ConversationsClientWrapper? = null

        fun createInstance(applicationContext: Context) {
            check(_instance == null) { "ConversationsClientWrapper singleton instance has been already created" }
            _instance = ConversationsClientWrapper(applicationContext)


        }
    }
}