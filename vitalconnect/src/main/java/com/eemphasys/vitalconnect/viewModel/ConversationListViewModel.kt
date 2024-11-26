package com.eemphasys.vitalconnect.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.adapters.ConversationListAdapter
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.SavePinnedConversationRequest
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asConversationListViewItems
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.merge
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.manager.ConnectivityMonitor
import com.eemphasys.vitalconnect.manager.ConversationListManager
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceHelper
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ConversationListViewModel(
    private val applicationContext: Context,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    connectivityMonitor: ConnectivityMonitor
): ViewModel() {
    private val unfilteredUserConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    val userConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    private val isDataLoading = SingleLiveEvent<Boolean>()
    val isNoResultsFoundVisible = MutableLiveData(false)
    val isNoConversationsVisible = MutableLiveData(false)
    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onConversationCreated = SingleLiveEvent<Unit>()
    val onConversationLeft = SingleLiveEvent<Unit>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationError = SingleLiveEvent<ConversationsError>()
    val pinConversation = SingleLiveEvent<Boolean>()
    val maxLimitPin = SingleLiveEvent<Unit>()

    val type = object : TypeToken<ArrayList<String>>() {}.type
    var jsonString = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"pinnedConvo") ?: ""
//    var pinnedConvo : ArrayList<String> = if(jsonString.isNullOrEmpty()){
//        arrayListOf()
//    }
//    else Gson().fromJson(jsonString, type)

    var conversationFilter by Delegates.observable("") { _, _, _ -> updateUserConversationItems() }

    init {
        getUserConversations()

        unfilteredUserConversationItems.observeForever { updateUserConversationItems() }
    }

    private fun updateUserConversationItems() {
        val filteredItems = unfilteredUserConversationItems.value?.filterByName(conversationFilter) ?: emptyList()
        userConversationItems.value = filteredItems

        isNoResultsFoundVisible.value = conversationFilter.isNotEmpty() && filteredItems.isEmpty()
        isNoConversationsVisible.value = conversationFilter.isEmpty() && filteredItems.isEmpty()
    }

    fun getUserConversations() = viewModelScope.launch {
        try
        {
        EETLog.saveUserJourney("vitaltext:  ConversationListViewModel getUserConversations Called")
        conversationsRepository.getUserConversations().collect { (list, status) ->

            unfilteredUserConversationItems.value = list
                .asConversationListViewItems(applicationContext)
                .merge(unfilteredUserConversationItems.value)

            if (status is RepositoryRequestStatus.Error) {
                onConversationError.value = ConversationsError.CONVERSATION_FETCH_USER_FAILED
            }
        }
        }
        catch(e:Exception)
        {
            Log.d("ExceptionAtgetUserConversations",e.message.toString())
//            EETLog.error(
//                AppContextHelper.appContext!!, LogConstants.logDetails(
//                    e,
//                    LogConstants.LOG_LEVEL.ERROR.toString(),
//                    LogConstants.LOG_SEVERITY.HIGH.toString()
//                ),
//                Constants.EX, LogTraceConstants.getUtilityData(
//                    AppContextHelper.appContext!!
//                )!!
//            )
        }
    }

    private fun setDataLoading(loading: Boolean) {
        if (isDataLoading.value != loading) {
            isDataLoading.value = loading
        }
    }

    private fun setConversationLoading(conversationSid: String, loading: Boolean) {
        fun ConversationListViewItem.transform() = if (sid == conversationSid) copy(isLoading = loading) else this
        unfilteredUserConversationItems.value = unfilteredUserConversationItems.value?.map { it.transform() }
    }

    private fun isConversationLoading(conversationSid: String): Boolean =
        unfilteredUserConversationItems.value?.find { it.sid == conversationSid }?.isLoading == true

    private fun List<ConversationListViewItem>.filterByName(name: String): List<ConversationListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter {
                it.name.contains(name, ignoreCase = true) ||
                        it.customer.contains(name, ignoreCase = true) ||
                        it.department.contains(name, ignoreCase = true) ||
                        it.designation.contains(name, ignoreCase = true)
            }
        }

    fun muteConversation(conversationSid: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationListViewModel muteConversation Called")
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.muteConversation(conversationSid)
            onConversationMuted.value = true

            LogTraceHelper.trace(
                applicationContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Mute Conversation",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(applicationContext)!!
            )
        } catch (e: TwilioException) {
            onConversationError.value = ConversationsError.CONVERSATION_MUTE_FAILED
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
        }
        catch(e:Exception)
        {
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
        }
        finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun unmuteConversation(conversationSid: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationListViewModel unmuteConversation Called")
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.unmuteConversation(conversationSid)
            onConversationMuted.value = false

            LogTraceHelper.trace(
                applicationContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Unmute Conversation",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(applicationContext)!!
            )
        } catch (e: TwilioException) {
            onConversationError.value = ConversationsError.CONVERSATION_UNMUTE_FAILED
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
        }
        catch(e:Exception)
        {
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
        }finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun leaveConversation(conversationSid: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationListViewModel leaveConversations Called")
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        try {
            if(Constants.PINNED_CONVO.contains(conversationSid)){
                Constants.PINNED_CONVO.remove(conversationSid)
                savePinnedConversationToDB{}
            }
            setConversationLoading(conversationSid, true)
            conversationListManager.leaveConversation(conversationSid)
            onConversationLeft.call()

            LogTraceHelper.trace(
                applicationContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Leave Conversation",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(applicationContext)!!
            )
        } catch (e: TwilioException) {
            onConversationError.value = ConversationsError.CONVERSATION_LEAVE_FAILED
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
        }
        catch(e:Exception)
        {
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
        }
        finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun savePinnedConversation(conversation: ConversationListViewItem,add:Boolean,adapter: ConversationListAdapter) = viewModelScope.launch{
        EETLog.saveUserJourney("vitaltext:  ConversationListViewModel savePinnedConversation Called")
        if(add){
//            pinnedConvo.add(conversation.sid)
//            Constants.PINNED_CONVO.add(conversation.sid)
            var added = Constants.addToPinnedConvo(conversation.sid)
            if(added) {
                conversation.isPinned = add
                pinConversation.value = true
                adapter.notifyDataSetChanged()
                getUserConversations()
            }
            else{
                maxLimitPin.call()
            }

        }
        else{
//            pinnedConvo.remove(conversation.sid)
            Constants.PINNED_CONVO.remove(conversation.sid)
            conversation.isPinned= !add
            pinConversation.value = false
            adapter.notifyDataSetChanged()
            getUserConversations()
        }
//        Constants.saveStringToVitalTextSharedPreferences(applicationContext,"pinnedConvo",Gson().toJson(pinnedConvo!!))
            savePinnedConversationToDB{}
    }

    fun savePinnedConversationToDB(callback: (() -> Unit)){
        viewModelScope.launch {
                val request = SavePinnedConversationRequest(
                Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,
//                pinnedConvo,
                Constants.PINNED_CONVO,
                Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!
            )

            var response = RetrofitClient.getRetrofitWithToken().savePinnedConversation(request)
            callback.invoke()
        }
    }
}