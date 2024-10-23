package com.eemphasys.vitalconnectdev.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.SendOtpReq
import com.eemphasys.vitalconnect.api.data.UpdatePasswordReq
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnectdev.common.SingleLiveEvent
import com.eemphasys.vitalconnectdev.common.call
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.data.LoginConstants
import kotlinx.coroutines.launch
import com.eemphasys.vitalconnectdev.manager.ConnectivityMonitor
import com.eemphasys.vitalconnectdev.manager.LoginManager
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.util.TwilioException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class LoginViewModel(
    private val loginManager: LoginManager,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    val isLoading = MutableLiveData(false)

    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onSignInError = SingleLiveEvent<ConversationsError>()

    val onSignInSuccess = SingleLiveEvent<Unit>()

    val isAADEnabled = MutableLiveData(false)
    val isPasswordUpdated = MutableLiveData(false)

    fun signIn(identity: String, password :String,applicationContext : Context) {
        if (isLoading.value == true) return

        if (isNetworkAvailable.value == false) {
            onSignInError.value = ConversationsError.NO_INTERNET_CONNECTION
            return
        }

        val credentialError = validateSignInDetails(identity, password)

        if (credentialError != ConversationsError.NO_ERROR) {
            onSignInError.value = credentialError
            return
        }

        isLoading.value = true
        viewModelScope.launch {
            try {
                LoginConstants.TIMESTAMP = getTimeStamp()
                loginManager.getAuthenticationToken(identity, password, getTimeStamp(),applicationContext)
                if(!LoginConstants.AUTH_TOKEN.isNullOrEmpty()) {
                    loginManager.getTwilioToken(applicationContext)
//                    loginManager.getTwilioclient()
//                    loginManager.registerForFcm()
                    onSignInSuccess.call()
                }else{
                    isLoading.value = false
                    onSignInError.value = ConversationsError.USERNAME_PASSWORD_INCORRECT
                }
            } catch (e: TwilioException) {
                isLoading.value = false
                onSignInError.value = ConversationsError.TOKEN_ACCESS_DENIED
                //e.printStackTrace()
                Log.d("Error",e.toString())
                EETLog.error(
                    AppContextHelper.appContext, LogTraceConstants.logDetails(
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
                    AppContextHelper.appContext, LogConstants.logDetails(
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
    }

    fun getTimeStamp(): String{
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(calendar.time)
    }
    private fun validateSignInDetails(identity: String, password: String): ConversationsError {
        return when {
            identity.isBlank() -> ConversationsError.EMPTY_USERNAME_AND_PASSWORD
            identity.isBlank() -> ConversationsError.EMPTY_USERNAME
            password.isBlank() -> ConversationsError.EMPTY_PASSWORD
            else -> ConversationsError.NO_ERROR
        }
    }

    fun isAzureADEnabled (tenant : String,applicationContext: Context){
        viewModelScope.launch {
            val result = RetrofitClient.retrofitWithToken.validateTenant(tenant)
            if(result.isSuccessful) {
                Log.d("isAzureAdenabled", result.body()!!.isAADEnabled.toString())
                LoginConstants.IS_AADENABLED = result.body()!!.isAADEnabled.toString()
                isAADEnabled.value = result.body()!!.isAADEnabled
            }
            else{
                isAADEnabled.value = false
            }

        }
    }

//    fun sendOtp() {
//        viewModelScope.launch {
//            val retrofithelper = RetrofitHelper.getInstance().create(TwilioApi::class.java)
//            val requestBody = SendOtpReq("hmahajan", "VitalConnectNonAzureAd")
//            val response = retrofithelper.sendOTP(requestBody)
//            Log.d("response", response.isSuccessful.toString())
//        }
//    }

    fun sendOtp(tenantCode: String,userName: String,applicationContext: Context,callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val requestBody = SendOtpReq(userName, tenantCode)
            val response = RetrofitClient.retrofitWithToken.sendOTP(requestBody)
            callback(response.isSuccessful)
        }
    }

    fun updatePassword(tenantCode : String, userName:String,password:String,otp:String,applicationContext: Context){
        viewModelScope.launch {
            val requestBody = UpdatePasswordReq(tenantCode, userName, password, otp)
            val response = RetrofitClient.retrofitWithToken.updatePassword(requestBody)
            if(response.isSuccessful) {
                isPasswordUpdated.value = response.isSuccessful
            }
            else{
                isPasswordUpdated.value = false
            }
        }
    }
}
