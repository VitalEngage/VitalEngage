package com.eemphasys.vitalconnect.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asUserViewItem
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.eemphasys.vitalconnect.manager.UserManager

class ProfileViewModel(
    private val conversationsRepository: ConversationsRepository,
    private val userManager: UserManager,
    /*private val loginManager: LoginManager,*/
): ViewModel() {
    val selfUser = conversationsRepository.getSelfUser()
        .map { it.asUserViewItem() }
        .asLiveData(viewModelScope.coroutineContext)

    val onUserUpdated = SingleLiveEvent<Unit>()
    val onSignedOut = SingleLiveEvent<Unit>()
    val onError = SingleLiveEvent<ConversationsError>()

    fun setFriendlyName(friendlyName: String) = viewModelScope.launch {
        //Timber.d("Updating self user: $friendlyName")
        try {
            userManager.setFriendlyName(friendlyName)
            //Timber.d("Self user updated: $friendlyName")
            onUserUpdated.call()
        } catch (e: TwilioException) {
            //Timber.d("Failed to update self user")
            onError.value = ConversationsError.USER_UPDATE_FAILED
        }
    }

    /*fun signOut() = viewModelScope.launch {
        Timber.d("signOut")
        loginManager.signOut()
        onSignedOut.call()
    }*/
}