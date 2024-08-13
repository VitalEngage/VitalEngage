package com.eemphasys.vitalconnect.common

import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.createTwilioException
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseTokenManager {
    suspend fun retrieveToken(): String {
        deleteToken().await()
        return suspendCoroutine { continuation ->
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener { task ->
                try {
                    task.result?.let { continuation.resume(it) }
                        ?: continuation.resumeWithException(createTwilioException(ConversationsError.TOKEN_ERROR))
                } catch (e: Exception) {
// TOO_MANY_REGISTRATIONS thrown on devices with too many Firebase instances
                    continuation.resumeWithException(createTwilioException(ConversationsError.TOKEN_ERROR))
                    e.printStackTrace()

                    EETLog.error(
                        AppContextHelper.appContext, LogConstants.logDetails(
                            e,
                            LogConstants.LOG_LEVEL.ERROR.toString(),
                            LogConstants.LOG_SEVERITY.HIGH.toString()
                        ),
                        Constants.EX, LogTraceConstants.getUtilityData(
                            AppContextHelper.appContext!!
                        )!!
                    );
                }
            }
        }
    }
    fun deleteToken() = CompletableDeferred<Boolean>().apply {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            complete(task.isSuccessful)
        }
    }

    companion object {

        val INSTANCE get() = _instance ?: error("call FirebaseTokenManager.createInstance() first")

        private var _instance: FirebaseTokenManager? = null

        fun createInstance() {
            check(_instance == null) { "FirebaseTokenManager singleton instance has been already created" }
            _instance = FirebaseTokenManager()
        }
    }
}