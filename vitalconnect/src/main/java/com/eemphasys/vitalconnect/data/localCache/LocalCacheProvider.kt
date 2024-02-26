package com.eemphasys.vitalconnect.data.localCache

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eemphasys.vitalconnect.data.localCache.dao.ConversationsDao
import com.eemphasys.vitalconnect.data.localCache.dao.MessagesDao
import com.eemphasys.vitalconnect.data.localCache.dao.ParticipantsDao

abstract class LocalCacheProvider: RoomDatabase() {
    abstract fun conversationsDao(): ConversationsDao

    abstract fun messagesDao(): MessagesDao

    abstract fun participantsDao(): ParticipantsDao

    companion object {
        val INSTANCE get() = _instance ?: error("call LocalCacheProvider.createInstance() first")

        private var _instance: LocalCacheProvider? = null

        fun createInstance(context: Context) {
            check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
            _instance = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LocalCacheProvider::class.java
            ).build()
        }
    }
}