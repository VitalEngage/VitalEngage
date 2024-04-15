package com.eemphasys.vitalconnect.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.common.ChatAppModel
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
    private val EXTRA_CONVERSATION_SID = "ExtraConversationSid"
    val userConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    val isDataLoading = SingleLiveEvent<Boolean>()
    val isNoResultsFoundVisible = MutableLiveData(false)
    val isNoConversationsVisible = MutableLiveData(false)
    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onConversationCreated = SingleLiveEvent<Unit>()
    val onConversationLeft = SingleLiveEvent<Unit>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationError = SingleLiveEvent<ConversationsError>()

    var conversationFilter by Delegates.observable("") { _, _, _ -> updateUserConversationItems() }
    val isShowProgress = MutableLiveData<Boolean>()
    val onParticipantAdded = SingleLiveEvent<String>()
    val onDetailsError = SingleLiveEvent<ConversationsError>()

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

    fun getUserConversations() = viewModelScope.launch {
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
                delay(1000)
                MessageListActivity.startfromFragment(applicationContext,conversationSid)

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
            );
        } finally {
            setDataLoading(false)
        }
    }

    fun addChatParticipant(identity: String,sid:String) = viewModelScope.launch {
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
            );
        } finally {
            setShowProgress(false)
        }
    }

    fun addNonChatParticipant(phone: String, proxyPhone: String,friendlyName: String,sid:String) = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
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
            );
        } finally {
            setShowProgress(false)
        }
    }
}