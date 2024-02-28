package com.eemphasys.vitalconnect.common

import android.content.Context
import androidx.annotation.RestrictTo
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.manager.ConnectivityMonitorImpl
import com.eemphasys.vitalconnect.manager.ConversationListManagerImpl
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys.vitalconnect.viewModel.ConversationListViewModel

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
    }