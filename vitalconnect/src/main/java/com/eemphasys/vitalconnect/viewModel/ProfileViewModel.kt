package com.eemphasys.vitalconnect.viewModel

import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.UserAlertRequest
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asUserViewItem
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.eemphasys.vitalconnect.manager.UserManager

class ProfileViewModel(
    private val conversationsRepository: ConversationsRepository,
    private val userManager: UserManager,
    /*private val loginManager: LoginManager,*/
): ViewModel() {
    val selfUser = conversationsRepository.getSelfUser()
        .map { it.asUserViewItem() }
        .asLiveData(viewModelScope.coroutineContext)

    val onUserUpdated = SingleLiveEvent<Unit>()
    val onSignedOut = SingleLiveEvent<Unit>()
    val onError = SingleLiveEvent<ConversationsError>()

    fun setFriendlyName(friendlyName: String) = viewModelScope.launch {
        try {
            userManager.setFriendlyName(friendlyName)
            onUserUpdated.call()
        } catch (e: TwilioException) {
            onError.value = ConversationsError.USER_UPDATE_FAILED
        }
    }

    fun signOut() = viewModelScope.launch {
        userManager.signOut()
        onSignedOut.call()
    }

    fun changeUserAlertStatus(isChecked : Boolean) = viewModelScope.launch {
        val apicall = RetrofitHelper.getInstance().create(TwilioApi::class.java)
        if(isChecked){
            val requestData = UserAlertRequest(Constants.USERNAME,true, Constants.TENANT_CODE)
            apicall.updateUserAlertStatus(requestData)
            Log.d("apicall", "true")
        }
        else{
            val requestData = UserAlertRequest(Constants.USERNAME,false, Constants.TENANT_CODE)
            apicall.updateUserAlertStatus(requestData)
            Log.d("apicall", "false")
        }
    }

}