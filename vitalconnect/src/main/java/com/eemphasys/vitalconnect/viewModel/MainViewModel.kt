package com.eemphasys.vitalconnect.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.GetUserAlertStatusRequest
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.manager.MainManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainViewModel(private val mainManager: MainManager) : ViewModel() {

        fun create(){
            EETLog.saveUserJourney("vitaltext:  MainViewModel create Called")
            viewModelScope.launch {
                try {
                    mainManager.getTwilioclient()
                } catch (e: TwilioException) {
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
                catch (e:Exception)
                {
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
        }

    fun getUserAlertStatus() = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  Mainviewmodel getUserAlertStatus Called")
        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)
        val request = GetUserAlertStatusRequest(Constants.TENANT_CODE,Constants.USERNAME)
        val response =retrofitWithToken.getUserAlertStatus(request)
        if(response.isSuccessful) {
            Constants.USER_SMS_ALERT = response.body()!!.status.lowercase()
        }
        else{
            Log.d("useralertstatus", response.code().toString() + " " + response.message())
        }
    }

    fun registerForFcm(){
        viewModelScope.launch {
            try {
                mainManager.registerForFcm()
            } catch (e: TwilioException) {
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
    }
}