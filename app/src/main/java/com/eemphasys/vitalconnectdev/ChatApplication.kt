package com.eemphasys.vitalconnectdev

import android.app.Application
import android.content.Context
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.room.Room
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.localCache.LocalCacheProvider
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl

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
    appContext = applicationContext
    //LocalCacheProvider.createInstance(ChatApplication.appContext!!)
    ConversationsClientWrapper.createInstance(this)
    LocalCacheProvider.createInstance(this)
    ConversationsRepositoryImpl.createInstance(ConversationsClientWrapper.INSTANCE, LocalCacheProvider.INSTANCE)
    EmojiCompat.init(BundledEmojiCompatConfig(this))

}

}