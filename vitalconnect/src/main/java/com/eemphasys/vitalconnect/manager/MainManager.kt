package com.eemphasys.vitalconnect.manager

import com.eemphasys.vitalconnect.data.ConversationsClientWrapper

interface MainManager {
    suspend fun getTwilioclient()
}

class MainManagerImpl(
    private val conversationsClient: ConversationsClientWrapper
) : MainManager {
    override suspend fun getTwilioclient() {
        conversationsClient.getclient()
    }





}