package com.eemphasys.vitalconnect

import android.app.Application
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper

class ChatApp: Application() {

    override fun onCreate() {
        super.onCreate()

        ConversationsClientWrapper.createInstance(this)


    }
}