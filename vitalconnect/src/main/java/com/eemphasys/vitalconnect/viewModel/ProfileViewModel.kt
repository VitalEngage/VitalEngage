package com.eemphasys.vitalconnect.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.UserAlertRequest
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asUserViewItem
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.manager.ConnectivityMonitor
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.eemphasys.vitalconnect.manager.UserManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants

class ProfileViewModel(
    private val conversationsRepository: ConversationsRepository,
    private val userManager: UserManager,
    connectivityMonitor: ConnectivityMonitor
    /*private val loginManager: LoginManager,*/
): ViewModel() {
    val selfUser = conversationsRepository.getSelfUser()
        .map { it.asUserViewItem() }
        .asLiveData(viewModelScope.coroutineContext)

    val onUserUpdated = SingleLiveEvent<Unit>()
    val onSignedOut = SingleLiveEvent<Unit>()
    val onError = SingleLiveEvent<ConversationsError>()
    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    fun setFriendlyName(friendlyName: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ProfileViewModel setFriendlyName Called")
        try {
            userManager.setFriendlyName(friendlyName)
            onUserUpdated.call()
        } catch (e: TwilioException) {
            onError.value = ConversationsError.USER_UPDATE_FAILED
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
            );
        }
        catch (e: Exception) {
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
            );
        }
    }

    fun signOut() = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ProfileViewModel signOut Called")
        userManager.signOut()
        onSignedOut.call()
    }

    fun changeUserAlertStatus(isChecked : Boolean) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  ProfileViewModel changeUserAlertStatus Called")
        val apicall = RetrofitHelper.getInstance().create(TwilioApi::class.java)
        if(isChecked){
            val requestData = UserAlertRequest(Constants.USERNAME,"true", Constants.TENANT_CODE)
            val response = apicall.updateUserAlertStatus(requestData)
            Constants.USER_SMS_ALERT = response.body()!!.status
        }
        else{
            val requestData = UserAlertRequest(Constants.USERNAME,"false", Constants.TENANT_CODE)
            val response= apicall.updateUserAlertStatus(requestData)
            Constants.USER_SMS_ALERT = response.body()!!.status
        }
    }

}