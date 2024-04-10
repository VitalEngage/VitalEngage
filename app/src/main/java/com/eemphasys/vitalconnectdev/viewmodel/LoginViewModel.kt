package com.eemphasys.vitalconnectdev.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.common.extensions.toConversationsError
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnectdev.common.SingleLiveEvent
import com.eemphasys.vitalconnectdev.common.call
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.extensions.toConversationsError
import com.eemphasys.vitalconnectdev.data.LoginConstants
import kotlinx.coroutines.launch

import com.eemphasys.vitalconnectdev.manager.ConnectivityMonitor
import com.eemphasys.vitalconnectdev.manager.LoginManager
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.util.TwilioException

class LoginViewModel(
    private val loginManager: LoginManager,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    val isLoading = MutableLiveData(false)

    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onSignInError = SingleLiveEvent<ConversationsError>()

    val onSignInSuccess = SingleLiveEvent<Unit>()

    fun signIn(identity: String) {
        if (isLoading.value == true) return

        if (isNetworkAvailable.value == false) {
            onSignInError.value = ConversationsError.NO_INTERNET_CONNECTION
            return
        }

        val credentialError = validateSignInDetails(identity)

        if (credentialError != ConversationsError.NO_ERROR) {
            onSignInError.value = credentialError
            return
        }

        isLoading.value = true
        viewModelScope.launch {
            try {
                loginManager.signIn(identity)
                initializeChatAppModel()
                loginManager.getTwilioclient()
                loginManager.registerForFcm()
                onSignInSuccess.call()
            } catch (e: TwilioException) {
                isLoading.value = false
                //onSignInError.value = e.toConversationsError()
                //e.printStackTrace()
                Log.d("Error",e.toString())
                /*EETLog.error(
                    SessionHelper.appContext, LogConstants.logDetails(
                        e,
                        LogConstants.LOG_LEVEL.ERROR.toString(),
                        LogConstants.LOG_SEVERITY.HIGH.toString()
                    ),
                    Constants.EX, LogTraceConstants.getUtilityData(
                        SessionHelper.appContext!!
                    )!!
                );*/
            }
        }
    }

    private fun initializeChatAppModel(){
        ChatAppModel.init(
            LoginConstants.BASE_URL,
            LoginConstants.TWILIO_TOKEN
        )
    }

    private fun validateSignInDetails(identity: String): ConversationsError {
        return when {
            identity.isBlank() -> ConversationsError.EMPTY_USERNAME_AND_PASSWORD
            identity.isBlank() -> ConversationsError.EMPTY_USERNAME
//            password.isBlank() -> ConversationsError.EMPTY_PASSWORD
            else -> ConversationsError.NO_ERROR
        }
    }
}
