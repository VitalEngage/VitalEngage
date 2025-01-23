package com.eemphasys.vitalconnectdev.manager

import android.content.Context
import android.util.Log
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.api.data.ValidateUserReq
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.FirebaseTokenManager
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.extensions.createTwilioException
import com.eemphasys.vitalconnectdev.data.CredentialStorage
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.extensions.registerFCMToken


interface LoginManager {
    suspend fun getAuthenticationToken(identity: String,clientId : String,clientSecret: String, password: String, timeStamp:String,applicationContext: Context)

    suspend fun getTwilioToken(applicationContext : Context)
    suspend fun signInUsingStoredCredentials()
    suspend fun signOut()
    fun clearCredentials()
    fun isLoggedIn(): Boolean

    suspend fun getTwilioclient(applicationContext: Context)
    suspend fun registerForFcm()

//    suspend fun isAzureAdEnabled()
}

class LoginManagerImpl(
    private val credentialStorage: CredentialStorage,
    private val firebaseTokenManager: FirebaseTokenManager,
    private val conversationsClient: ConversationsClientWrapper,
): LoginManager {
    override suspend fun getAuthenticationToken(identity: String,clientId : String,clientSecret: String, password: String, timeStamp: String,applicationContext: Context) {
//        val requestData = ValidateUserReq(
//            identity,
//            password,
//            LoginConstants.TENANT_CODE,
//            "",
//            timeStamp,
//            true,
//            "",
//            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"reCaptchaToken") ?: ""
//        )
        val apiInstance = RetrofitHelper.getInstance(applicationContext).create(
            TwilioApi::class.java
        )
//        val result = apiInstance.validateUser(requestData)

        val request = RequestToken(LoginConstants.TENANT_CODE,clientId,clientSecret,identity,"","",false,"",LoginConstants.PROXY_NUMBER)
        val result = apiInstance.getAuthToken(request)
        if(result.isSuccessful) {
            Log.d("Authtoken: ", result.body()!!.jwtToken)
            Constants.saveStringToVitalTextSharedPreferences(applicationContext, "authToken", result.body()!!.jwtToken)
            Constants.saveStringToVitalTextSharedPreferences(applicationContext,"authTokenTimeStamp",Constants.getTimeStamp())
            Constants.saveStringToVitalTextSharedPreferences(applicationContext,"expirationDuration",result.body()!!.expirationTime.toString())
            Constants.saveStringToVitalTextSharedPreferences(applicationContext,"userSMSAlert",result.body()!!.enableNotification.toString())
            LoginConstants.AUTH_TOKEN = result.body()!!.jwtToken
            LoginConstants.PROXY_NUMBER = result.body()!!.proxyNumber
//            LoginConstants.USER_SMS_ALERT = result.body()!!.enableNotification.toString()
            LoginConstants.SHOW_DEPARTMENT = result.body()!!.showDepartment.toString()
            LoginConstants.SHOW_DESIGNATION = result.body()!!.showDesignation.toString()
            LoginConstants.EMAIL = result.body()!!.email
            LoginConstants.MOBILENUMBER = result.body()!!.mobileNumber
            LoginConstants.DEALER_NAME = result.body()!!.dealerName
            LoginConstants.PINNED_CONVO = result.body()?.pinedConversation ?: arrayListOf()
            LoginConstants.EXPIRATION_DURATION =result.body()!!.expirationTime
            LoginConstants.REFRESH_TOKEN=result.body()!!.refreshToken
            Log.d("pinnedconvo",result.body()!!.pinedConversation.toString())
            credentialStorage.storeCredentials(identity,password)
        }
    }

    override suspend fun getTwilioToken(applicationContext : Context) {
        Log.d("username", LoginConstants.CURRENT_USER)
            val twilioToken = RetrofitClient.getRetrofitWithToken().getTwilioToken(
                LoginConstants.TENANT_CODE,
                LoginConstants.CURRENT_USER,
                LoginConstants.FRIENDLY_NAME,
                "Android",
                "VC"
            )
            if(twilioToken.isSuccessful) {
                Log.d("twiliotoken", twilioToken.body()!!.token)
                LoginConstants.TWILIO_TOKEN = twilioToken.body()!!.token
//                ChatAppModel.twilio_token = LoginConstants.TWILIO_TOKEN
                Constants.saveStringToVitalTextSharedPreferences(applicationContext,"twilioToken",twilioToken.body()!!.token)
            }
//        }
        getTwilioclient(applicationContext)

    }

    override suspend fun signInUsingStoredCredentials() {
        if (credentialStorage.isEmpty()) throw createTwilioException(ConversationsError.NO_STORED_CREDENTIALS)
        val identity = credentialStorage.identity
        val password = credentialStorage.password
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

    override suspend fun getTwilioclient(applicationContext: Context) {
        conversationsClient.getclient(applicationContext)
        ConversationsRepositoryImpl.INSTANCE.subscribeToConversationsClientEvents()
        registerForFcm()
        conversationsClient.shutdown()
    }


    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()

//            credentialStorage.fcmToken = token
            conversationsClient.getConversationsClient().registerFCMToken(

                ConversationsClient.FCMToken(token)
            )

        } catch (e: Exception) {
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext, LogTraceConstants.logDetails(
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

//    override suspend fun isAzureAdEnabled() {
//        val tokenApi = RetrofitHelper.getInstance().create(TwilioApi::class.java)
//        val result = tokenApi.validateTenant(LoginConstants.TENANT_CODE)
//        Log.d("isAzureAdenabled", result.body()!!.isAADEnabled.toString())
//        LoginConstants.IS_AADENABLED = result.body()!!.isAADEnabled.toString()
//        isAADEnabled.value = result.body()!!.isAADEnabled
//    }

}