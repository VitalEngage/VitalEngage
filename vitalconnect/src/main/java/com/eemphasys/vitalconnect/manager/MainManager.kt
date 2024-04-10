package com.eemphasys.vitalconnect.manager

import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.FirebaseTokenManager
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.CredentialStorage
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.extensions.registerFCMToken

interface MainManager {
    suspend fun getTwilioclient()
    suspend fun registerForFcm()

    suspend fun unregisterFromFcm()
}

class MainManagerImpl(
    private val conversationsClient: ConversationsClientWrapper,
    private val firebaseTokenManager: FirebaseTokenManager,
    private val credentialStorage: CredentialStorage,
    private val conversationsRepository: ConversationsRepository,
) : MainManager {
    override suspend fun getTwilioclient() {
        conversationsClient.getclient()
        conversationsRepository.subscribeToConversationsClientEvents()
    }


    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()

            credentialStorage.fcmToken = token

//Timber.d("Registering for FCM: $token")

            conversationsClient.getConversationsClient().registerFCMToken(

                ConversationsClient.FCMToken(token)

            )

        } catch (e: Exception) {
            e.printStackTrace()

            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }

    }
    override suspend fun unregisterFromFcm() {

// We don't call `conversationsClient.getConversationsClient().unregisterFCMToken(token)` here

// because it fails with commandTimeout (60s by default) if device is offline or token is expired.

// Instead we try to delete token on FCM async. Which leads to the same result if device is online,

// but we can shutdown `conversationsClient`immediately without waiting a result.

        firebaseTokenManager.deleteToken()

    }




}