package com.eemphasys.vitalconnectdev.common

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eemphasys.vitalconnect.manager.FCMManager
import com.eemphasys.vitalconnectdev.data.CredentialStorage
import com.eemphasys.vitalconnectdev.manager.ConnectivityMonitorImpl
import com.eemphasys.vitalconnectdev.manager.LoginManager
import com.eemphasys.vitalconnectdev.manager.LoginManagerImpl
import com.eemphasys.vitalconnectdev.viewmodel.LoginViewModel


var injector = Injector()
    private set

open class Injector{

    open fun createLoginManager(applicationContext: Context): LoginManager = LoginManagerImpl(
        CredentialStorage(applicationContext),
        FirebaseTokenManager(),
    )

    open fun createLoginViewModel(application: Application): LoginViewModel {
        val loginManager = createLoginManager(application)
        val connectivityMonitor = ConnectivityMonitorImpl(application)

        return LoginViewModel(loginManager, connectivityMonitor)
    }

}