package com.eemphasys.vitalconnect.manager

import android.content.Context
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.FirebaseTokenManager
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.CredentialStorage
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.extensions.registerFCMToken

interface MainManager {
    suspend fun getTwilioclient(applicationContext: Context)
    suspend fun registerForFcm()

    suspend fun unregisterFromFcm()
}

class MainManagerImpl(
    private val conversationsClient: ConversationsClientWrapper,
    private val firebaseTokenManager: FirebaseTokenManager,
    private val credentialStorage: CredentialStorage,
    private val conversationsRepository: ConversationsRepository,
) : MainManager {
    override suspend fun getTwilioclient(applicationContext: Context) {
        conversationsClient.getclient(applicationContext)
        conversationsRepository.subscribeToConversationsClientEvents()
    }


    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()

            conversationsClient.getConversationsClient().registerFCMToken(

                ConversationsClient.FCMToken(token)

            )

        } catch (e: Exception) {
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
    override suspend fun unregisterFromFcm() {

        firebaseTokenManager.deleteToken()

    }
}