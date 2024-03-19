package com.eemphasys.vitalconnectdev.manager

import android.util.Log
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.manager.MainManager
import com.eemphasys.vitalconnect.manager.MainManagerImpl
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.common.FirebaseTokenManager
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.extensions.createTwilioException
import com.eemphasys.vitalconnectdev.data.CredentialStorage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient


interface LoginManager {
    suspend fun signIn(identity: String)
    suspend fun signInUsingStoredCredentials()
    suspend fun signOut()
    //    suspend fun registerForFcm()
//    suspend fun unregisterFromFcm()
    fun clearCredentials()
    fun isLoggedIn(): Boolean
}

class LoginManagerImpl(
    private val credentialStorage: CredentialStorage,
    private val firebaseTokenManager: FirebaseTokenManager,
): LoginManager {

    override suspend fun signIn(identity: String) {
        //Timber.d("signIn")
        //conversationsClient.create


        val requestData = RequestToken(
            LoginConstants.TENANT_CODE,
            LoginConstants.CLIENT_ID,
            LoginConstants.CLIENT_SECRET,
            LoginConstants.CURRENT_USER,
            LoginConstants.PRODUCT
        )

            val tokenApi = RetrofitHelper.getInstance().create(TwilioApi::class.java)
            val result = tokenApi.getAuthToken(requestData)
            Log.d("Authtoken: ", result.body()!!.token)

            LoginConstants.AUTH_TOKEN = result.body()!!.token

            val httpClientWithToken = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(result.body()!!.token))
                .build()
            val retrofitWithToken =
                RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

        Log.d("username", LoginConstants.CURRENT_USER)
            val TwilioToken = retrofitWithToken.getTwilioToken(
                LoginConstants.TENANT_CODE,
                LoginConstants.CURRENT_USER,
                LoginConstants.FRIENDLY_NAME
            )

            Log.d("twiliotoken", TwilioToken.body()!!.token)
            LoginConstants.TWILIO_TOKEN = TwilioToken.body()!!.token



//        }
        credentialStorage.storeCredentials(identity)
    }

    override suspend fun signInUsingStoredCredentials() {
        //Timber.d("signInUsingStoredCredentials")
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
        //Timber.d("handleError")
        if (error == ConversationsError.TOKEN_ACCESS_DENIED) {
            clearCredentials()
        }
    }

}