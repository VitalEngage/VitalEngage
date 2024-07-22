package com.eemphasys.vitalconnect.manager

import com.eemphasys.vitalconnect.common.FirebaseTokenManager
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.common.extensions.setFriendlyName
import com.eemphasys.vitalconnect.repository.ConversationsRepository

interface UserManager {
    suspend fun setFriendlyName(friendlyName:String)
    suspend fun signOut()
}

class UserManagerImpl(private val conversationsClient: ConversationsClientWrapper,
                      private val conversationsRepository: ConversationsRepository,
                      private val firebaseTokenManager: FirebaseTokenManager
) : UserManager {

    override suspend fun setFriendlyName(friendlyName: String)
            = conversationsClient.getConversationsClient().myUser.setFriendlyName(friendlyName)

    override suspend fun signOut() {
        firebaseTokenManager.deleteToken()
        conversationsRepository.unsubscribeFromConversationsClientEvents()
        conversationsRepository.clear()
        conversationsClient.shutdown()
    }
}