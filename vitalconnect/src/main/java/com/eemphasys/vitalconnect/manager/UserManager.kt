package com.eemphasys.vitalconnect.manager

import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.common.extensions.setFriendlyName
import com.eemphasys.vitalconnect.repository.ConversationsRepository

interface UserManager {
    suspend fun setFriendlyName(friendlyName:String)
    suspend fun signOut()
}

class UserManagerImpl(private val conversationsClient: ConversationsClientWrapper,
                      private val conversationsRepository: ConversationsRepository
) : UserManager {

    override suspend fun setFriendlyName(friendlyName: String)
            = conversationsClient.getConversationsClient().myUser.setFriendlyName(friendlyName)

    override suspend fun signOut() {
        conversationsRepository.unsubscribeFromConversationsClientEvents()
        conversationsRepository.clear()
        conversationsClient.shutdown()
    }
}