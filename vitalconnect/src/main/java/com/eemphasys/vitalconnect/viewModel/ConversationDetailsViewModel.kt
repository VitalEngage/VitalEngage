package com.eemphasys.vitalconnect.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.data.AddAzureAdParticipantConversationRequest
import com.eemphasys.vitalconnect.api.data.webParticipant
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asConversationDetailsViewItem
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.data.models.ConversationDetailsViewItem
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus
import com.eemphasys.vitalconnect.manager.ConnectivityMonitor
import com.eemphasys.vitalconnect.manager.ConversationListManager
import com.eemphasys.vitalconnect.manager.ParticipantListManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConversationDetailsViewModel(
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    private val participantListManager: ParticipantListManager,
    connectivityMonitor : ConnectivityMonitor,
    private val applicationContext: Context
) : ViewModel() {

    val conversationDetails = MutableLiveData<ConversationDetailsViewItem>()
    val isShowProgress = MutableLiveData<Boolean>()
    val onDetailsError = SingleLiveEvent<ConversationsError>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationLeft = SingleLiveEvent<Unit>()
    private val onConversationRenamed = SingleLiveEvent<Unit>()
    val onParticipantAdded = SingleLiveEvent<String>()
    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)
    var isPinned = MutableLiveData<Boolean>()
    init {
        viewModelScope.launch {
            getConversationResult()
        }
    }

    suspend fun getConversationResult() {
        EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel getConversationResult Called")
        conversationsRepository.getConversation(conversationSid).collect { result ->
            if (result.requestStatus is RepositoryRequestStatus.Error) {
                onDetailsError.value = ConversationsError.CONVERSATION_GET_FAILED
                return@collect
            }
           try{ result.data?.let { conversationDetails.value = it.asConversationDetailsViewItem(applicationContext) }}catch(e:Exception){}
        }
    }

    fun onPinConversationClicked(pinValue : Boolean) {
        Log.d("onpinclick","clciked")
//        isPinned.value = !(isPinned.value ?: false)
        isPinned.value = !pinValue
    }

    private fun setShowProgress(show: Boolean) {
        if (isShowProgress.value != show) {
            isShowProgress.value = show
        }
    }

    fun renameConversation(friendlyName: String) = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            conversationListManager.renameConversation(conversationSid, friendlyName)
            onConversationRenamed.call()
        } catch (e: TwilioException) {
            onDetailsError.value = ConversationsError.CONVERSATION_RENAME_FAILED
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

    fun muteConversation() = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel muteconversation Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            conversationListManager.muteConversation(conversationSid)
            onConversationMuted.value = true
        } catch (e: TwilioException) {
            onDetailsError.value = ConversationsError.CONVERSATION_MUTE_FAILED
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

    fun unmuteConversation() = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel unmuteConversation Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            conversationListManager.unmuteConversation(conversationSid)
            onConversationMuted.value = false
        } catch (e: TwilioException) {
            onDetailsError.value = ConversationsError.CONVERSATION_UNMUTE_FAILED
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

    fun leaveConversation() = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel Leave Conversation Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            if(Constants.PINNED_CONVO.contains(conversationSid)){
                Constants.PINNED_CONVO.remove(conversationSid)
            }
            conversationListManager.leaveConversation(conversationSid)
            onConversationLeft.call()
        } catch (e: TwilioException) {
            onDetailsError.value = ConversationsError.CONVERSATION_REMOVE_FAILED
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

    fun addChatParticipant(identity: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel addChatParticipant Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            participantListManager.addChatParticipant(identity)
            onParticipantAdded.value = identity
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

    fun addNonChatParticipant(phone: String, proxyPhone: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel addNonChatParticipant Called")
        if (isShowProgress.value == true) {
            return@launch
        }
        try {
            setShowProgress(true)
            participantListManager.addNonChatParticipant(phone, proxyPhone, friendlyName = phone)
            onParticipantAdded.value = phone
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

fun addAzureAdParticipant(details: ConversationDetailsViewItem?) {
    EETLog.saveUserJourney("vitaltext:  ConversationDetailsViewModel addAzureAdParticipant Called")
    try {
        setShowProgress(true)
        val isWebChat =
            Constants.getStringFromVitalTextSharedPreferences(applicationContext, "isWebChat")
        val tenantCode =
            Constants.getStringFromVitalTextSharedPreferences(applicationContext, "tenantCode")
                ?: ""
        val currentUser =
            Constants.getStringFromVitalTextSharedPreferences(applicationContext, "currentUser")
                ?: ""
        val isAutoRegistrationEnabled = Constants.getStringFromVitalTextSharedPreferences(
            applicationContext,
            "isAutoRegistrationEnabled"
        ) ?: ""
        val proxyNumber =
            Constants.getStringFromVitalTextSharedPreferences(applicationContext, "proxyNumber")
                ?: ""

        val email = if (Constants.CURRENT_CONTACT.isGroup.toString()
                .equals("true", ignoreCase = true)
        ) "" else Constants.CURRENT_CONTACT.email
        val name = if (Constants.CURRENT_CONTACT.isGroup.toString()
                .equals("true", ignoreCase = true)
        ) "" else Constants.CURRENT_CONTACT.name
        val groupID = if (Constants.CURRENT_CONTACT.isGroup.toString()
                .equals("true", ignoreCase = true)
        ) Constants.CURRENT_CONTACT.objectId else ""

        val request = AddAzureAdParticipantConversationRequest(
            tenantCode,
            currentUser,
            email,
            name,
            groupID,
            isAutoRegistrationEnabled,
            proxyNumber,
            details?.conversationSid,
            details?.conversationName,
            isWebChat
        )

        val response =
            RetrofitClient.getRetrofitWithToken().addAzureAdParticipantConversation(request)

        response.enqueue(object : Callback<List<webParticipant>> {
            override fun onResponse(
                call: Call<List<webParticipant>>,
                response: Response<List<webParticipant>>
            ) {
                if (response.isSuccessful) {
                    Log.d("response", response.body().toString())
                    if (response.body()!!.size > 0) {
                        onParticipantAdded.value = ""
                    } else {
                        onParticipantAdded.value = "failed"
                    }
                    setShowProgress(false)
                }
                else{
                    onParticipantAdded.value = "failed"
                }
            }

            override fun onFailure(call: Call<List<webParticipant>>, t: Throwable) {
                onParticipantAdded.value = "failed"
                setShowProgress(false)
            }
        })
    }catch (e: Exception){
        setShowProgress(false)
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
}


    fun getFriendlyName(identity : String): String {
        return conversationsRepository.getFriendlyName(identity)
    }

    fun isConversationMuted() = conversationDetails.value?.isMuted == true
}
