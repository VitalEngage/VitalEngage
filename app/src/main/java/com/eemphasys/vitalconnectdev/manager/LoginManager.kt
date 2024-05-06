package com.eemphasys.vitalconnectdev.manager

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.api.data.ValidateUserReq
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.common.FirebaseTokenManager
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.extensions.createTwilioException
import com.eemphasys.vitalconnectdev.data.CredentialStorage
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.extensions.registerFCMToken
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit


interface LoginManager {
    suspend fun getAuthenticationToken(identity: String, password: String)

    suspend fun getTwilioToken()
    suspend fun signInUsingStoredCredentials()
    suspend fun signOut()
    //    suspend fun registerForFcm()
//    suspend fun unregisterFromFcm()
    fun clearCredentials()
    fun isLoggedIn(): Boolean

    suspend fun getTwilioclient()
    suspend fun registerForFcm()

//    suspend fun isAzureAdEnabled()
}

class LoginManagerImpl(
    private val credentialStorage: CredentialStorage,
    private val firebaseTokenManager: FirebaseTokenManager,
    private val conversationsClient: ConversationsClientWrapper,
): LoginManager {
    override suspend fun getAuthenticationToken(identity: String, password: String) {
        val requestData = ValidateUserReq(
            identity,
            password,
            LoginConstants.TENANT_CODE,
            "hmahajan@e-emphasys.com",
            LoginConstants.TIMESTAMP,
            true,
            ""
        )

        val tokenApi = RetrofitHelper.getInstance().create(TwilioApi::class.java)
        val result = tokenApi.validateUser(requestData)
        if(result.isSuccessful) {
            Log.d("Authtoken: ", result.body()!!.jwtToken)
            LoginConstants.AUTH_TOKEN = result.body()!!.jwtToken
            LoginConstants.PROXY_NUMBER = result.body()!!.proxyNumber
        }
    }

    override suspend fun getTwilioToken() {
            val httpClientWithToken = OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(LoginConstants.AUTH_TOKEN))
                .addInterceptor(RetryInterceptor())
                .build()
            val retrofitWithToken =
                RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

        Log.d("username", LoginConstants.CURRENT_USER)
            val TwilioToken = retrofitWithToken.getTwilioToken(
                LoginConstants.TENANT_CODE,
                LoginConstants.CURRENT_USER,
                LoginConstants.FRIENDLY_NAME
            )
            if(TwilioToken.isSuccessful) {
                Log.d("twiliotoken", TwilioToken.body()!!.token)
                LoginConstants.TWILIO_TOKEN = TwilioToken.body()!!.token
                ChatAppModel.twilio_token = LoginConstants.TWILIO_TOKEN
            }
//        }
        credentialStorage.storeCredentials(LoginConstants.CURRENT_USER)
    }

    override suspend fun signInUsingStoredCredentials() {
        if (credentialStorage.isEmpty()) throw createTwilioException(ConversationsError.NO_STORED_CREDENTIALS)
        val identity = credentialStorage.identity
        val password = credentialStorage.password

//        try {
//            conversationsClient.create(identity, password)
//            conversationsRepository.subscribeToConversationsClientEvents()
//            registerForFcm()
//        } catch (e: TwilioException) {
//            handleError(e.toConversationsError())
//            throw e
//        }
    }

    override suspend fun signOut() {
        clearCredentials()
    }

    override fun isLoggedIn() = !credentialStorage.isEmpty()

    override fun clearCredentials() {
        credentialStorage.clearCredentials()
    }

    private fun handleError(error: ConversationsError) {
        if (error == ConversationsError.TOKEN_ACCESS_DENIED) {
            clearCredentials()
        }
    }

    override suspend fun getTwilioclient() {
        conversationsClient.getclient()
    }


    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()

            credentialStorage.fcmToken = token
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

//    override suspend fun isAzureAdEnabled() {
//        val tokenApi = RetrofitHelper.getInstance().create(TwilioApi::class.java)
//        val result = tokenApi.validateTenant(LoginConstants.TENANT_CODE)
//        Log.d("isAzureAdenabled", result.body()!!.isAADEnabled.toString())
//        LoginConstants.IS_AADENABLED = result.body()!!.isAADEnabled.toString()
//        isAADEnabled.value = result.body()!!.isAADEnabled
//    }

}