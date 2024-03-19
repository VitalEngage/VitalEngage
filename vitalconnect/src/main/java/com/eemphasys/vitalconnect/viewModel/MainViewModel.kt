package com.eemphasys.vitalconnect.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.manager.MainManager
import com.twilio.util.TwilioException
import kotlinx.coroutines.launch

class MainViewModel(private val mainManager: MainManager) : ViewModel() {

        fun create(){

            viewModelScope.launch {
                try {
                    mainManager.getTwilioclient()
                } catch (e: TwilioException) {

                }
            }
        }

    fun registerForFcm(){
        viewModelScope.launch {
            try {
                mainManager.registerForFcm()
            } catch (e: TwilioException) {
            }
        }
    }
}