package com.eemphasys.vitalconnect.common

import android.app.Application
import android.content.Context
import androidx.annotation.RestrictTo
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.manager.ConnectivityMonitorImpl
import com.eemphasys.vitalconnect.manager.ConversationListManagerImpl
import com.eemphasys.vitalconnect.manager.MainManager
import com.eemphasys.vitalconnect.manager.MainManagerImpl
import com.eemphasys.vitalconnect.manager.MessageListManagerImpl
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys.vitalconnect.viewModel.ConversationListViewModel
import com.eemphasys.vitalconnect.viewModel.MainViewModel
import com.eemphasys.vitalconnect.viewModel.MessageListViewModel

var injector = Injector()
    private set

@RestrictTo(RestrictTo.Scope.TESTS)
fun setupTestInjector(testInjector: Injector) {
    injector = testInjector
}
    open class Injector {
        open fun createConversationListViewModel(applicationContext: Context): ConversationListViewModel {
            val conversationListManager = ConversationListManagerImpl(ConversationsClientWrapper.INSTANCE)
            val connectivityMonitor = ConnectivityMonitorImpl(applicationContext)

            return ConversationListViewModel(
                applicationContext,
                ConversationsRepositoryImpl.INSTANCE,
                conversationListManager,
                connectivityMonitor,
            )
        }

        open fun createMainManager(applicationContext: Context): MainManager = MainManagerImpl(
            ConversationsClientWrapper.INSTANCE
        )

        open fun createMainViewModel(application: Application): MainViewModel {
            val mainManager = createMainManager(application)

            return MainViewModel(mainManager)
        }

        open fun createMessageListViewModel(appContext: Context, conversationSid: String): MessageListViewModel {
            val messageListManager = MessageListManagerImpl(
                conversationSid,
                ConversationsClientWrapper.INSTANCE,
                ConversationsRepositoryImpl.INSTANCE
            )
            return MessageListViewModel(
                appContext,
                conversationSid,
                ConversationsRepositoryImpl.INSTANCE,
                messageListManager
            )
        }
    }