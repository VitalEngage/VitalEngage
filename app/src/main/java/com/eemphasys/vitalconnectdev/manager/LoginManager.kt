package com.eemphasys.vitalconnectdev.manager

import com.eemphasys.vitalconnectdev.common.FirebaseTokenManager
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.extensions.createTwilioException
import com.eemphasys.vitalconnectdev.data.CredentialStorage


interface LoginManager {
    suspend fun signIn(identity: String, password: String)
    suspend fun signInUsingStoredCredentials()
    suspend fun signOut()
    //    suspend fun registerForFcm()
//    suspend fun unregisterFromFcm()
    fun clearCredentials()
    fun isLoggedIn(): Boolean
}

class LoginManagerImpl(
    private val credentialStorage: CredentialStorage,
    private val firebaseTokenManager: FirebaseTokenManager,
): LoginManager {

    override suspend fun signIn(identity: String, password: String) {
        //Timber.d("signIn")
        //conversationsClient.create(identity, password)
        credentialStorage.storeCredentials(identity, password)
    }

    override suspend fun signInUsingStoredCredentials() {
        //Timber.d("signInUsingStoredCredentials")
        if (credentialStorage.isEmpty()) throw createTwilioException(ConversationsError.NO_STORED_CREDENTIALS)
        val identity = credentialStorage.identity
        val password = credentialStorage.password

//        try {
//            conversationsClient.create(identity, password)
//            conversationsRepository.subscribeToConversationsClientEvents()
//            registerForFcm()
//        } catch (e: TwilioException) {
//            handleError(e.toConversationsError())
//            throw e
//        }
    }

    override suspend fun signOut() {
        clearCredentials()
    }

    override fun isLoggedIn() = !credentialStorage.isEmpty()

    override fun clearCredentials() {
        credentialStorage.clearCredentials()
    }

    private fun handleError(error: ConversationsError) {
        //Timber.d("handleError")
        if (error == ConversationsError.TOKEN_ACCESS_DENIED) {
            clearCredentials()
        }
    }

}