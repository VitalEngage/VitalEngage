package com.eemphasys.vitalconnect.common

import android.app.Application
import android.content.Context
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.manager.MainManager
import com.eemphasys.vitalconnect.manager.MainManagerImpl
import com.eemphasys.vitalconnect.viewModel.MainViewModel

var injector = Injector()
    private set

open class Injector {

    open fun createMainManager(applicationContext: Context): MainManager = MainManagerImpl(
        ConversationsClientWrapper.INSTANCE
        )

    open fun createMainViewModel(application: Application): MainViewModel {
        val loginManager = createMainManager(application)

        return MainViewModel(loginManager)
    }

}