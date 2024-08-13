package com.eemphasys.vitalconnectdev

import android.app.Application
import android.content.Context
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.room.Room
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.FirebaseTokenManager
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.localCache.LocalCacheProvider
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.firebase.FirebaseApp

class ChatApplication : Application() {


    companion object {
        private var _instance: LocalCacheProvider? = null
        var appContext: Context? = null
    }

//    fun createInstance(context: Context) {
//        check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
//        _instance = Room.inMemoryDatabaseBuilder(
//            context.applicationContext,
//            LocalCacheProvider::class.java
//        ).build()
//    }
    override fun onCreate() {
        super.onCreate()
    EETLog.saveUserJourney("!!VitalConnect App Init launched!!")
    try {
        appContext = applicationContext
        AppContextHelper.init(this)
        FirebaseApp.initializeApp(this)
        ConversationsClientWrapper.createInstance(this)
        FirebaseTokenManager.createInstance()
        LocalCacheProvider.createInstance(this)
        ConversationsRepositoryImpl.createInstance(ConversationsClientWrapper.INSTANCE, LocalCacheProvider.INSTANCE)
        EmojiCompat.init(BundledEmojiCompatConfig(this))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

}