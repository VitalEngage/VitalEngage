package com.eemphasys.vitalconnect.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asConversationListViewItems
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.merge
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.manager.ConnectivityMonitor
import com.eemphasys.vitalconnect.manager.ConversationListManager
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus
import com.eemphasys.vitalconnect.manager.AutoParticipantListManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceHelper
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.Attributes
import kotlinx.coroutines.delay


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
                    "Activity Selected :",
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
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            )
        } finally {
            setDataLoading(false)
        }
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
                    "Activity Selected :",
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
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
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
                    "Activity Selected :",
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
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
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
                    SessionHelper.appContext, LogConstants.logDetails(
                        e,
                        LogConstants.LOG_LEVEL.ERROR.toString(),
                        LogConstants.LOG_SEVERITY.HIGH.toString()
                    ),
                    Constants.EX, LogTraceConstants.getUtilityData(
                        SessionHelper.appContext!!
                    )!!
                )
                // Optionally, inform the caller of the error
                callback("") // Passing an empty string or null depending on your error handling strategy
            }
        }
    }
}

typealias AttributesCallback = (String) -> Unit