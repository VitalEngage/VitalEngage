package com.eemphasys.vitalconnect.manager

import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.common.extensions.setFriendlyName

interface UserManager {
    suspend fun setFriendlyName(friendlyName:String)
}

class UserManagerImpl(private val conversationsClient: ConversationsClientWrapper) : UserManager {

    override suspend fun setFriendlyName(friendlyName: String)
            = conversationsClient.getConversationsClient().myUser.setFriendlyName(friendlyName)

}