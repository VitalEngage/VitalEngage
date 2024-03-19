package com.eemphasys.vitalconnect.common

import android.app.Application
import android.content.Context
import androidx.annotation.RestrictTo
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.CredentialStorage
import com.eemphasys.vitalconnect.manager.AutoParticipantListManager
import com.eemphasys.vitalconnect.manager.AutoParticipantListManagerImpl
import com.eemphasys.vitalconnect.manager.ConnectivityMonitorImpl
import com.eemphasys.vitalconnect.manager.ConversationListManagerImpl
import com.eemphasys.vitalconnect.manager.FCMManager
import com.eemphasys.vitalconnect.manager.FCMManagerImpl
import com.eemphasys.vitalconnect.manager.MainManager
import com.eemphasys.vitalconnect.manager.MainManagerImpl
import com.eemphasys.vitalconnect.manager.MessageListManagerImpl
import com.eemphasys.vitalconnect.manager.ParticipantListManagerImpl
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys.vitalconnect.viewModel.ContactListViewModel
import com.eemphasys.vitalconnect.viewModel.ConversationDetailsViewModel
import com.eemphasys.vitalconnect.viewModel.ConversationListViewModel
import com.eemphasys.vitalconnect.viewModel.MainViewModel
import com.eemphasys.vitalconnect.viewModel.MessageListViewModel
import com.eemphasys.vitalconnect.viewModel.ParticipantListViewModel

var injector = Injector()
    private set

@RestrictTo(RestrictTo.Scope.TESTS)
fun setupTestInjector(testInjector: Injector) {
    injector = testInjector
}
    open class Injector {

        private var fcmManagerImpl: FCMManagerImpl? = null

        open fun createCredentialStorage(applicationContext: Context) = CredentialStorage(applicationContext)

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
            ConversationsClientWrapper.INSTANCE,
            FirebaseTokenManager(),
            CredentialStorage(applicationContext)
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

        open fun createConversationDetailsViewModel(conversationSid: String): ConversationDetailsViewModel {
            val conversationListManager = ConversationListManagerImpl(ConversationsClientWrapper.INSTANCE)
            val participantListManager = ParticipantListManagerImpl(conversationSid, ConversationsClientWrapper.INSTANCE)
            return ConversationDetailsViewModel(
                conversationSid,
                ConversationsRepositoryImpl.INSTANCE,
                conversationListManager,
                participantListManager
            )
        }

        open fun createParticipantListViewModel(conversationSid: String): ParticipantListViewModel {
            val participantListManager = ParticipantListManagerImpl(conversationSid, ConversationsClientWrapper.INSTANCE)
            return ParticipantListViewModel(conversationSid, ConversationsRepositoryImpl.INSTANCE, participantListManager)
        }

        open fun createFCMManager(context: Context): FCMManager {
            val credentialStorage = createCredentialStorage(context.applicationContext)
            if (fcmManagerImpl == null) {
                fcmManagerImpl = FCMManagerImpl(context, ConversationsClientWrapper.INSTANCE, credentialStorage)
            }
            return fcmManagerImpl!!
        }

        open fun createContactListViewModel(applicationContext: Context): ContactListViewModel {
            val conversationListManager = ConversationListManagerImpl(ConversationsClientWrapper.INSTANCE)
            val connectivityMonitor = ConnectivityMonitorImpl(applicationContext)
            val autoparticipantListManager = AutoParticipantListManagerImpl(ConversationsClientWrapper.INSTANCE)

            return ContactListViewModel(
                applicationContext,
                ConversationsRepositoryImpl.INSTANCE,
                conversationListManager,
                connectivityMonitor,
                autoparticipantListManager

            )
        }

    }