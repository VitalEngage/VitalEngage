package com.eemphasys.vitalconnect.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asParticipantListViewItems
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.data.models.ParticipantListViewItem
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus
import com.eemphasys.vitalconnect.manager.ParticipantListManager
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.twilio.util.TwilioException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
//import timber.log.Timber
import kotlin.properties.Delegates

@ExperimentalCoroutinesApi
@FlowPreview
class ParticipantListViewModel(
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository,
    private val participantListManager: ParticipantListManager
) : ViewModel() {

    private var unfilteredParticipantsList by Delegates.observable(listOf<ParticipantListViewItem>()) { _, _, _ ->
        updateParticipantList()
    }
    val participantsList = MutableLiveData<List<ParticipantListViewItem>>(emptyList())
    var participantFilter by Delegates.observable("") { _, _, _ ->
        updateParticipantList()
    }
    val onParticipantError = SingleLiveEvent<ConversationsError>()
    val onParticipantRemoved = SingleLiveEvent<Unit>()
    var selectedParticipant: ParticipantListViewItem? = null

    init {
        //Timber.d("init")
        getConversationParticipants()
    }

    private fun updateParticipantList() {
        participantsList.value = unfilteredParticipantsList.filterByName(participantFilter)
    }

    private fun List<ParticipantListViewItem>.filterByName(name: String): List<ParticipantListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter {
                it.friendlyName.contains(name, ignoreCase = true)
            }
        }

    fun getConversationParticipants() = viewModelScope.launch {
        conversationsRepository.getConversationParticipants(conversationSid).collect { (list, status) ->
            unfilteredParticipantsList = list.asParticipantListViewItems()
            if (status is RepositoryRequestStatus.Error) {
                onParticipantError.value = ConversationsError.PARTICIPANTS_FETCH_FAILED
            }
        }
    }

    fun removeSelectedParticipant() = viewModelScope.launch {
        val participant = selectedParticipant ?: return@launch

        try {
            participantListManager.removeParticipant(participant.sid)
            onParticipantRemoved.call()
        } catch (e: TwilioException) {
            //Timber.d("Failed to remove participant")
            onParticipantError.value = ConversationsError.PARTICIPANT_REMOVE_FAILED
        }
    }
}
