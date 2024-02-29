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

import android.net.Uri
import androidx.annotation.RestrictTo
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.createTwilioException
import com.twilio.conversations.extensions.addListener
import com.twilio.conversations.extensions.createAndSyncConversationsClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.net.URL
import com.eemphasys.vitalconnect.common.extensions.updateToken
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

    val onUpdateTokenFailure = mutableListOf<() -> Unit>()

    private fun notifyUpdateTokenFailure() = onUpdateTokenFailure.forEach { it() }


    suspend fun shutdown() {
        //Timber.d("shutdown")
        getConversationsClient().shutdown()
        deferredClient = CompletableDeferred()
    }

    /**
     * Fetch Twilio access token and return it, if token is non-null, otherwise return error
     */

//    private fun updateToken(identity: String, password: String, notifyOnFailure: Boolean) = coroutineScope.launch {
//        //Timber.d("updateToken notifyOnFailure: $notifyOnFailure")
//
//        val result = runCatching {
//            val twilioToken = getToken(identity, password)
//            getConversationsClient().updateToken(twilioToken)
//        }
//
//        if (result.isFailure && notifyOnFailure) {
//            //Timber.e(result.exceptionOrNull())
//            notifyUpdateTokenFailure()
//        }
//    }

    companion object {

        val INSTANCE get() = _instance ?: error("call ConversationsClientWrapper.createInstance() first")

        private var _instance: ConversationsClientWrapper? = null

        fun createInstance(applicationContext: Context) {
            check(_instance == null) { "ConversationsClientWrapper singleton instance has been already created" }
            _instance = ConversationsClientWrapper(applicationContext)
        }

        @DelicateCoroutinesApi
        @RestrictTo(RestrictTo.Scope.TESTS)
        fun recreateInstance(applicationContext: Context) {
            _instance?.let { instance ->
                // Shutdown old client if it will ever be created
                GlobalScope.launch { instance.getConversationsClient().shutdown() }
            }

            _instance = null
            createInstance(applicationContext)
        }
    }
}