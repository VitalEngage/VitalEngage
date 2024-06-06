package com.eemphasys.vitalconnect.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.delay


class ContactListViewModel(
    private val applicationContext: Context,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    connectivityMonitor: ConnectivityMonitor,
    private val autoParticipantListManager: AutoParticipantListManager
): ViewModel() {
    private val unfilteredUserConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())
    private val userConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    private val isDataLoading = SingleLiveEvent<Boolean>()
    private val isNoResultsFoundVisible = MutableLiveData(false)
    private val isNoConversationsVisible = MutableLiveData(false)

    private val onConversationCreated = SingleLiveEvent<Unit>()
    private val onConversationError = SingleLiveEvent<ConversationsError>()

    private var conversationFilter by Delegates.observable("") { _, _, _ -> updateUserConversationItems() }
    private val isShowProgress = MutableLiveData<Boolean>()
    val onParticipantAdded = SingleLiveEvent<String>()
    private val onDetailsError = SingleLiveEvent<ConversationsError>()

    init {
        getUserConversations()

        unfilteredUserConversationItems.observeForever { updateUserConversationItems() }
    }

    private fun updateUserConversationItems() {
        val filteredItems = unfilteredUserConversationItems.value?.filterByName(conversationFilter) ?: emptyList()
        userConversationItems.value = filteredItems

        isNoResultsFoundVisible.value = conversationFilter.isNotEmpty() && filteredItems.isEmpty()
        isNoConversationsVisible.value = conversationFilter.isEmpty() && filteredItems.isEmpty()

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
    }

    private fun getUserConversations() = viewModelScope.launch {
        conversationsRepository.getUserConversations().collect { (list, status) ->

            unfilteredUserConversationItems.value = list
                .asConversationListViewItems(applicationContext)
                .merge(unfilteredUserConversationItems.value)

            if (status is RepositoryRequestStatus.Error) {
                onConversationError.value = ConversationsError.CONVERSATION_FETCH_USER_FAILED
            }
        }
    }

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


   /* private fun setConversationLoading(conversationSid: String, loading: Boolean) {
        fun ConversationListViewItem.transform() = if (sid == conversationSid) copy(isLoading = loading) else this
        unfilteredUserConversationItems.value = unfilteredUserConversationItems.value?.map { it.transform() }
    }

    private fun isConversationLoading(conversationSid: String): Boolean =
        unfilteredUserConversationItems.value?.find { it.sid == conversationSid }?.isLoading == true*/

    private fun List<ConversationListViewItem>.filterByName(name: String): List<ConversationListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter { it.name.contains(name, ignoreCase = true) }
        }


    fun createConversation(friendlyName: String, identity : String, phoneNumber : String) = viewModelScope.launch {
        try {
            setDataLoading(true)

            if(phoneNumber != "")
            {
                val conversationSid = conversationListManager.createConversation(friendlyName)
                conversationListManager.joinConversation(conversationSid)
                addNonChatParticipant(phoneNumber, Constants.PROXY_NUMBER,friendlyName,conversationSid)
                MessageListActivity.startfromFragment(applicationContext,conversationSid)
                Log.d("nonchat participant","participant added")
            }
            else {
                val conversationSid = conversationListManager.createConversation(friendlyName)
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
        viewModelScope.launch {
            delay(3000)
            MessageListActivity.startfromFragment(applicationContext,sid) }
    }
}