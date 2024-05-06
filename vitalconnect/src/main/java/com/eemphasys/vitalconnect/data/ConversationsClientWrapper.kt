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

import androidx.annotation.RestrictTo
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.createTwilioException
import com.twilio.conversations.extensions.addListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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

    suspend fun getclient(){

        val client = createAndSyncConversationsClient(applicationContext, ChatAppModel.twilio_token!!)
        this.deferredClient.complete(client)
        Log.d("client", client.myIdentity)
        client.addListener(
            onTokenAboutToExpire = { Log.d("OntokenAboutToExpire","OnTokenAboutToExpire")
                updateToken(client.myIdentity, notifyOnFailure = false) },
            onTokenExpired = { Log.d("onTokenExpired","onTokenExpired")
                updateToken(client.myIdentity, notifyOnFailure = true) },
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

    private fun updateToken(identity: String,notifyOnFailure: Boolean) = coroutineScope.launch {
        //Timber.d("updateToken notifyOnFailure: $notifyOnFailure")

        val result = runCatching {
            val twilioToken = getToken(identity)
            getConversationsClient().updateToken(twilioToken)
        }

        if (result.isFailure && notifyOnFailure) {
            //Timber.e(result.exceptionOrNull())
            notifyUpdateTokenFailure()
        }
    }

    private suspend fun getToken(username: String) = withContext(Dispatchers.IO) {
        try {
            /*val uri = Uri.parse(TOKEN_URL)
                .buildUpon()
                .appendQueryParameter(QUERY_IDENTITY, username)
                .appendQueryParameter(QUERY_PASSWORD, password)
                .build()
                .toString()

            return@withContext URL(uri).readText()*/
            if(Constants.AUTH_TOKEN.isNullOrEmpty()) {
                val requestData = RequestToken(
                    Constants.TENANT_CODE,
                    Constants.CLIENT_ID,
                    Constants.CLIENT_SECRET,
                    username,
                    Constants.PRODUCT,
                    "",
                    true,
                    Constants.FULL_NAME,
                    Constants.PROXY_NUMBER
                )

                val tokenApi = RetrofitHelper.getInstance().create(TwilioApi::class.java)
                val result = tokenApi.getAuthToken(requestData)
                Log.d("Authtoken in clientwrapper: ", result.body()!!.jwtToken)

                Constants.AUTH_TOKEN = result.body()!!.jwtToken
            }

            val httpClientWithToken = OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
                .addInterceptor(RetryInterceptor())
                .build()
            val retrofitWithToken =
                RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

            Log.d("username", Constants.USERNAME)
            val TwilioToken = retrofitWithToken.getTwilioToken(
                Constants.TENANT_CODE,
                username,
                Constants.FRIENDLY_NAME
            )

            Log.d("twiliotoken", TwilioToken.body()!!.token)
            Constants.TWILIO_TOKEN = TwilioToken.body()!!.token
            return@withContext TwilioToken.body()!!.token
        } catch (e: FileNotFoundException) {
            throw createTwilioException(ConversationsError.TOKEN_ACCESS_DENIED)
        } catch (e: Exception) {

            /*e.printStackTrace()*/

            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
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