package com.eemphasys.vitalconnect.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ConversationSidFromFriendlyNameRequest
import com.eemphasys.vitalconnect.api.data.ConversationSidFromFriendlyNameResponse
import com.eemphasys.vitalconnect.api.data.addParticipantToWebConversationRequest
import com.eemphasys.vitalconnect.api.data.webParticipant
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.manager.ConnectivityMonitor
import com.eemphasys.vitalconnect.manager.ConversationListManager
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch
import com.eemphasys.vitalconnect.manager.AutoParticipantListManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceHelper
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.Attributes
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit


class ContactListViewModel(
    private val applicationContext: Context,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    connectivityMonitor: ConnectivityMonitor,
    private val autoParticipantListManager: AutoParticipantListManager
): ViewModel() {
    private val unfilteredUserConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())


    private val isDataLoading = SingleLiveEvent<Boolean>()

    private val onConversationCreated = SingleLiveEvent<Unit>()
    private val onConversationError = SingleLiveEvent<ConversationsError>()

    private val isShowProgress = MutableLiveData<Boolean>()
    val onParticipantAdded = SingleLiveEvent<String>()
    private val onDetailsError = SingleLiveEvent<ConversationsError>()
    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    private fun setDataLoading(loading: Boolean) {
        if (isDataLoading.value != loading) {
            isDataLoading.value = loading
        }
    }

    private fun setShowProgress(show: Boolean) {
        if (isShowProgress.value != show) {
            isShowProgress.value = show
        }
    }

    private fun List<ConversationListViewItem>.filterByName(name: String): List<ConversationListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter { it.name.contains(name, ignoreCase = true) }
        }


    fun createConversation(friendlyName: String, identity : String, phoneNumber : String,attributes: Attributes) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ContactListViewModel createConversation Called")
        try {
            setDataLoading(true)

            if(phoneNumber != "")
            {
                val conversationSid = conversationListManager.createConversation(friendlyName,attributes)
                conversationListManager.joinConversation(conversationSid)
                addNonChatParticipant(phoneNumber, Constants.PROXY_NUMBER,friendlyName,conversationSid)
                MessageListActivity.startfromFragment(applicationContext,conversationSid)
                Log.d("nonchat participant","participant added")
            }
            else {
                val conversationSid = conversationListManager.createConversation(friendlyName,attributes)
                conversationListManager.joinConversation(conversationSid)
                addChatParticipant(identity, conversationSid)
                MessageListActivity.startfromFragment(applicationContext,conversationSid)

                onConversationCreated.call()
            }
            LogTraceHelper.trace(
                applicationContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "create Conversation",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(applicationContext)!!
            )

        } catch (e: TwilioException) {
            onConversationError.value = ConversationsError.CONVERSATION_CREATE_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        } finally {
            setDataLoading(false)
        }
    }

    fun createWebConversation(friendlyName: String,attributes: Attributes,contact: ContactListViewItem)= viewModelScope.launch {
        Log.d("attributes",attributes.toString())
        val conversationSid = conversationListManager.createConversation(friendlyName,attributes)
        //add participants to conversation
        addWebParticipants(contact,conversationSid)
//        conversationListManager.removeConversation("CH5c9ba2bb3cd04bd29033fc8661c3da6a")
//        conversationListManager.removeConversation("CHa4a05b44724b4e3a847b467fc381b809")

    }

    fun checkExistingconversation(contact : ContactListViewItem){
        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)
        var request = ConversationSidFromFriendlyNameRequest(Constants.TENANT_CODE,Constants.USERNAME,Constants.CONTEXT)
        val existingWebConversation = retrofitWithToken.getTwilioConversationSidFromFriendlyName(request)

        existingWebConversation.enqueue(object :
            Callback<List<ConversationSidFromFriendlyNameResponse>> {
            override fun onResponse(
                call: Call<List<ConversationSidFromFriendlyNameResponse>>,
                response: Response<List<ConversationSidFromFriendlyNameResponse>>
            ) {
                if (response.isSuccessful) {
                    val conversationList: List<ConversationSidFromFriendlyNameResponse>? =
                        response.body()
                    var conversationSid = ""
                    if (!conversationList.isNullOrEmpty()) {
                        for (conversation in conversationList) {
                            Log.d(
                                "Existing Web Conversation",
                                conversation.conversationSid
                            )
                            conversationSid = conversation.conversationSid
                        }
                        //add participants to conversation
                        addWebParticipants(contact,conversationSid)
                        //Redirect to existing conversation
                        MessageListActivity.startfromFragment(applicationContext,conversationSid)
                    }
                    else{
                        //Create new conversation and add participant to it
                        val attributes = mapOf(
                            "isWebChat" to true
                        )
                        val jsonObject = JSONObject(attributes)
                        createWebConversation(Constants.CONTEXT,Attributes(jsonObject),contact)
                    }
                }

            }

            override fun onFailure(
                call: Call<List<ConversationSidFromFriendlyNameResponse>>,
                t: Throwable
            ) {

            }

        })
    }

    fun addWebParticipants(contact:ContactListViewItem,conversationSid: String){
        val webUser = ArrayList<webParticipant>()
        webUser.add(webParticipant(Constants.USERNAME,Constants.FRIENDLY_NAME,""))
        webUser.add(webParticipant(contact.email,contact.name,""))
        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)
        val request = addParticipantToWebConversationRequest(Constants.TENANT_CODE,Constants.USERNAME,webUser,conversationSid,Constants.CONTEXT,true,Constants.PROXY_NUMBER)
        val participantDetails = retrofitWithToken.addParticipantToWebToWebConversation(request)

        participantDetails.enqueue(object: Callback<List<webParticipant>> {
            override fun onResponse(
                call: Call<List<webParticipant>>,
                response: Response<List<webParticipant>>
            ) {
                if(response.isSuccessful){
                    for (i in response.body()!!){
                        Log.d("ParticipantSID",i.fullName + i.participantSid)
                    }
                    //Redirect to messageList activity after adding participant to existing conversation
                    MessageListActivity.startfromFragment(applicationContext,conversationSid)
                }
            }

            override fun onFailure(
                call: Call<List<webParticipant>>,
                t: Throwable
            ) {

            }

        })
    }

    private fun addChatParticipant(identity: String, sid:String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ContactListViewModel addChatParticipant Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            autoParticipantListManager.addChatParticipant(identity,sid)
            onParticipantAdded.value = identity

            LogTraceHelper.trace(
                applicationContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Add chat Participant",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(applicationContext)!!
            )
        } catch (e: TwilioException) {
            onDetailsError.value = ConversationsError.PARTICIPANT_ADD_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        } finally {
            setShowProgress(false)
        }
    }

    private fun addNonChatParticipant(phone: String, proxyPhone: String, friendlyName: String, sid:String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ContactListViewModel addNonChatParticipant Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            Log.d("addnonchatparticipant", "$phone $proxyPhone $friendlyName $sid")
            autoParticipantListManager.addNonChatParticipant(phone, proxyPhone,friendlyName,sid)
            onParticipantAdded.value = phone
            LogTraceHelper.trace(
                applicationContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Add Non chat Participant",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(applicationContext)!!
            )
        } catch (e: TwilioException) {
            onDetailsError.value = ConversationsError.PARTICIPANT_ADD_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        } finally {
            setShowProgress(false)
        }
    }

    fun getSyncStatus(sid: String) {
        EETLog.saveUserJourney("vitaltext:  ContactListViewModel getSyncStatus Called")
        viewModelScope.launch {
            delay(3000)
            MessageListActivity.startfromFragment(applicationContext,sid) }
    }

    fun setAttributes(conversationSid : String, attributes: Attributes){
        EETLog.saveUserJourney("vitaltext:  ContactListViewModel setAttributes Called")
        viewModelScope.launch {
            conversationListManager.setAttributes(conversationSid,attributes)
        }
    }

    fun getAttributes(conversationSid: String, callback: AttributesCallback) {
        EETLog.saveUserJourney("vitaltext:  ContactListViewModel getAttributes Called")
        viewModelScope.launch {
            try {
                val attributes = conversationListManager.getAttributes(conversationSid)
                // Call the callback with the fetched attributes
                callback(attributes)
            } catch (e: Exception) {
                // Handle exception
                println("Error: ${e.message}")
                EETLog.error(
                    AppContextHelper.appContext!!, LogConstants.logDetails(
                        e,
                        LogConstants.LOG_LEVEL.ERROR.toString(),
                        LogConstants.LOG_SEVERITY.HIGH.toString()
                    ),
                    Constants.EX, LogTraceConstants.getUtilityData(
                        AppContextHelper.appContext!!
                    )!!
                )
                // Optionally, inform the caller of the error
                callback("") // Passing an empty string or null depending on your error handling strategy
            }
        }
    }
}

typealias AttributesCallback = (String) -> Unit