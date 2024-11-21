package com.eemphasys.vitalconnect.data.localCache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eemphasys.vitalconnect.data.localCache.dao.ConversationsDao
import com.eemphasys.vitalconnect.data.localCache.dao.MessagesDao
import com.eemphasys.vitalconnect.data.localCache.dao.ParticipantsDao
import com.eemphasys.vitalconnect.data.localCache.entity.ConversationDataItem
import com.eemphasys.vitalconnect.data.localCache.entity.MessageDataItem
import com.eemphasys.vitalconnect.data.localCache.entity.ParticipantDataItem

@Database(entities = [ConversationDataItem::class, MessageDataItem::class, ParticipantDataItem::class], version = 1, exportSchema = false)
abstract class LocalCacheProvider: RoomDatabase() {
    abstract fun conversationsDao(): ConversationsDao

    abstract fun messagesDao(): MessagesDao

    abstract fun participantsDao(): ParticipantsDao

    companion object {
        val INSTANCE get() = _instance ?: error("call LocalCacheProvider.createInstance() first")

        var _instance: LocalCacheProvider? = null

        fun createInstance(context: Context) {
            check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
            _instance = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LocalCacheProvider::class.java
            ).allowMainThreadQueries().build()
//            _instance = Room.databaseBuilder(
//                context.applicationContext,
//                LocalCacheProvider::class.java,
//                "vitaltext.db"
//            ).setJournalMode(JournalMode.AUTOMATIC)
//                .allowMainThreadQueries().build()
        }

        fun clearDatabase() {
            _instance?.clearAllTables()
        }

        fun destroyInstance() {
            _instance = null
        }
    }
}