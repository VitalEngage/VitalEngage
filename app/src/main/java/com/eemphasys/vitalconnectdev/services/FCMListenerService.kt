package com.eemphasys.vitalconnectdev.services

import android.content.Intent
import android.util.Log
import com.eemphasys.vitalconnect.common.injector
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twilio.conversations.NotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FCMListenerService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private fun launch(block: suspend CoroutineScope.() -> Unit) = serviceScope.launch(
//Commented by Hardik
//context = CoroutineExceptionHandler { _, e -> Timber.e(e, "Coroutine failed ${e.localizedMessage}") },
        block = block
    )
    private val fcmManager by lazy { injector.createFCMManager(application) }
    private val credentialStorage by lazy { injector.createCredentialStorage(applicationContext) }
    override fun onNewToken(token: String) {
        Log.d("fcmtoken in parent",token)
        super.onNewToken(token)
        launch {
            fcmManager.onNewToken(token)
        }
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("onmessage",remoteMessage.data.toString())
        super.onMessageReceived(remoteMessage)
        launch {
// Check if message contains a data payload and we have saved FCM token
// If we don't have FCM token - probably we receive this message because of invalidating the token
// on logout has failed (probably device has been offline). We will invalidate the token on
// next login then.
            if (remoteMessage.data.isNotEmpty() && credentialStorage.fcmToken.isNotEmpty()) {
                fcmManager.onMessageReceived(NotificationPayload(remoteMessage.data))
            }
        }
    }

    override fun
            handleIntent
                (intent: Intent) {
        try
        {
            if
                    (intent.
                extras
                !=
                null
            ) {
                val
                        builder = RemoteMessage.Builder(
                    "MessagingService"
                )
                for
                        (key
                in
                intent.
                extras
                !!.keySet()) {
                    builder.addData(key!!
                        ,
                        intent.
                        extras
                        !![key].
                        toString
                            ())
                }
                onMessageReceived(builder.build())
            }
            else
            {
                super
                    .handleIntent(intent)
            }
        }
        catch
            (e: Exception) {
            super
                .handleIntent(intent)
        }
    }

}