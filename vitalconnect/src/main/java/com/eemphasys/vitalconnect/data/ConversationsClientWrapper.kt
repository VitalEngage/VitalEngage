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

import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.createTwilioException
import com.twilio.conversations.extensions.addListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import com.eemphasys.vitalconnect.common.extensions.updateToken
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ConversationsClientWrapper(private val applicationContext: Context) {
    private var deferredClient = CompletableDeferred<ConversationsClient>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val isClientCreated get() = deferredClient.isCompleted && !deferredClient.isCancelled

    suspend fun getConversationsClient() = deferredClient.await()

    suspend fun getclient(applicationContext: Context){
//        ConversationsClient.setLogLevel(ConversationsClient.LogLevel.VERBOSE);
        val client = createAndSyncConversationsClient(applicationContext, Constants.getStringFromVitalTextSharedPreferences(applicationContext,"twilioToken")!!)
        this.deferredClient.complete(client)
        Log.d("client", client.myIdentity)
        client.addListener(
            onTokenAboutToExpire = { Log.d("OntokenAboutToExpire","OnTokenAboutToExpire")
                updateToken(applicationContext,client.myIdentity, notifyOnFailure = false) },
            onTokenExpired = { Log.d("onTokenExpired","onTokenExpired")
                updateToken(applicationContext,client.myIdentity, notifyOnFailure = true) },
        )
    }

    val onUpdateTokenFailure = mutableListOf<() -> Unit>()

    private fun notifyUpdateTokenFailure() = onUpdateTokenFailure.forEach { it() }


    suspend fun shutdown() {
        getConversationsClient().shutdown()
        deferredClient = CompletableDeferred()
    }

    /**
     * Fetch Twilio access token and return it, if token is non-null, otherwise return error
     */

    private fun updateToken(applicationContext: Context,identity: String,notifyOnFailure: Boolean) = coroutineScope.launch {

        val result = runCatching {
            val twilioToken = getToken(identity,applicationContext)
            Constants.saveStringToVitalTextSharedPreferences(applicationContext,"twilioToken",twilioToken)
            getConversationsClient().updateToken(twilioToken)
        }

        if (result.isFailure && notifyOnFailure) {
            notifyUpdateTokenFailure()
        }
    }

    private suspend fun getToken(username: String,applicationContext: Context) = withContext(Dispatchers.IO) {
        try {
//            if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"authToken")!!.isNullOrEmpty()) {
//                val requestData = RequestToken(
//                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
//                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"clientId")!!,
//                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"clientSecret")!!,
//                    username,
//                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"product")!!,
//                    "",
//                    true,
//                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"friendlyName")!!,
//                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"proxyNumber")!!
//                )
//
//                val tokenApi = RetrofitHelper.getInstance(applicationContext).create(TwilioApi::class.java)
//                val result = tokenApi.getAuthToken(requestData)
//                Log.d("Authtoken in clientwrapper: ", result.body()!!.jwtToken)
//
//                Constants.saveStringToVitalTextSharedPreferences(applicationContext,"authToken",result.body()!!.jwtToken)
//            }

            val TwilioToken = RetrofitClient.getRetrofitWithToken().getTwilioToken(
                Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                username,
                Constants.getStringFromVitalTextSharedPreferences(applicationContext,"friendlyName")!!
            )
//            Constants.TWILIO_TOKEN = TwilioToken.body()!!.token
            return@withContext TwilioToken.body()!!.token
        } catch (e: FileNotFoundException) {
            throw createTwilioException(ConversationsError.TOKEN_ACCESS_DENIED)
        } catch (e: Exception) {
            EETLog.error(
                AppContextHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
            throw createTwilioException(ConversationsError.TOKEN_ERROR);
        }
    }

    companion object {

        val INSTANCE get() = _instance ?: error("call ConversationsClientWrapper.createInstance() first")

        private var _instance: ConversationsClientWrapper? = null

        fun createInstance(applicationContext: Context) {
            check(_instance == null) { "ConversationsClientWrapper singleton instance has been already created" }
            _instance = ConversationsClientWrapper(applicationContext)
        }

    }
}