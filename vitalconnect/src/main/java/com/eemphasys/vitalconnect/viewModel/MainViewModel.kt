package com.eemphasys.vitalconnect.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.manager.MainManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch

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