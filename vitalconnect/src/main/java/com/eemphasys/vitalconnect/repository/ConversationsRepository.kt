package com.eemphasys.vitalconnect.repository

import android.util.Log
import com.eemphasys.vitalconnect.data.localCache.entity.ConversationDataItem
import com.eemphasys.vitalconnect.data.models.RepositoryResult
import com.twilio.conversations.Conversation
import com.twilio.conversations.Message
import com.twilio.conversations.User
import com.twilio.conversations.extensions.ConversationListener
import com.twilio.conversations.extensions.ConversationsClientListener
import com.twilio.conversations.extensions.getLastMessages
import com.twilio.conversations.extensions.getMessagesBefore
import com.twilio.util.TwilioException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import androidx.paging.PagedList
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.DefaultDispatcherProvider
import com.eemphasys.vitalconnect.common.DispatcherProvider
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.common.asMessageDataItems
import com.eemphasys.vitalconnect.common.asMessageListViewItems
import com.eemphasys.vitalconnect.common.asParticipantDataItem
import com.eemphasys.vitalconnect.common.enums.CrashIn
import com.eemphasys.vitalconnect.common.toMessageDataItem

import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.localCache.LocalCacheProvider
import com.eemphasys.vitalconnect.data.localCache.entity.MessageDataItem
import com.eemphasys.vitalconnect.data.localCache.entity.ParticipantDataItem
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus.COMPLETE
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus.Error
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus.FETCHING
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus.SUBSCRIBING
import kotlinx.coroutines.flow.flowOn
import com.eemphasys.vitalconnect.common.extensions.getAndSubscribeUser
import com.eemphasys.vitalconnect.common.extensions.getMessageCount
import com.eemphasys.vitalconnect.common.extensions.getParticipantCount
import com.eemphasys.vitalconnect.common.extensions.getUnreadMessageCount
import com.eemphasys.vitalconnect.common.extensions.simulateCrash
import com.eemphasys.vitalconnect.common.extensions.toConversationsError
import com.eemphasys.vitalconnect.common.toConversationDataItem
import com.twilio.conversations.extensions.getConversation
import com.twilio.conversations.extensions.waitForSynchronization
import com.eemphasys.vitalconnect.common.toFlow
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.ConversationsClient
import kotlinx.coroutines.flow.onStart

import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.awaitClose

import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
interface ConversationsRepository {
    fun getUserConversations(): Flow<RepositoryResult<List<ConversationDataItem>>>
    fun getConversation(conversationSid: String): Flow<RepositoryResult<ConversationDataItem?>>
    fun getSelfUser(): Flow<User>
    fun getMessageByUuid(messageUuid: String): MessageDataItem?
    // Interim solution till paging v3.0 is available as an alpha version.
    // It has support for converting PagedList types
    fun getMessages(conversationSid: String, pageSize: Int): Flow<RepositoryResult<PagedList<MessageListViewItem>>>
    fun insertMessage(message: MessageDataItem)
    fun updateMessageByUuid(message: MessageDataItem)
    fun updateMessageStatus(messageUuid: String, sendStatus: Int, errorCode: Int)
    fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>>
    fun getConversationParticipants(conversationSid: String): Flow<RepositoryResult<List<ParticipantDataItem>>>
    fun updateMessageMediaDownloadStatus(
        messageSid: String,
        downloadId: Long? = null,
        downloadLocation: String? = null,
        downloadState: Int? = null,
        downloadedBytes: Long? = null
    )
    fun updateMessageMediaUploadStatus(
        messageUuid: String,
        uploading: Boolean? = null,
        uploadedBytes: Long? = null
    )
    fun simulateCrash(where: CrashIn)
    fun clear()
    fun subscribeToConversationsClientEvents()
    fun unsubscribeFromConversationsClientEvents()
}

class ConversationsRepositoryImpl(
    private val conversationsClientWrapper: ConversationsClientWrapper,
    private val localCache: LocalCacheProvider,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ConversationsRepository {

    private val repositoryScope = CoroutineScope(dispatchers.io() + SupervisorJob())

    private val clientListener = ConversationsClientListener(
        onConversationDeleted = { conversation ->
            launch {
                localCache.conversationsDao().delete(conversation.sid)
            }
        },
        onConversationUpdated = { conversation, _ ->
            launch{ insertOrUpdateConversation(conversation.sid) }
        },
        onConversationAdded = { conversation ->
            launch {
                insertOrUpdateConversation(conversation.sid)
            }
        },
        onConversationSynchronizationChange = { conversation ->
            launch { insertOrUpdateConversation(conversation.sid) }
        },

    onClientSynchronization = { synchronizationstatus->
        Log.d("inside listener", synchronizationstatus.toString())
        launch{
            if (synchronizationstatus == ConversationsClient.SynchronizationStatus.COMPLETED) {
        Log.d("inside listener and if", "insidelistener")
                getUserConversations()

            }
        }
    }
    )

    private val conversationListener = ConversationListener(
        onTypingStarted = { conversation, participant ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(typing = true, user))
            }
        },
        onTypingEnded = { conversation, participant ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(typing = false, user))
            }
        },
        onParticipantAdded = { participant ->
            this@ConversationsRepositoryImpl.launch {
                val user = if (participant.channel == "chat") participant.getAndSubscribeUser() else null
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(user = user))
            }
        },
        onParticipantUpdated = { participant, reason ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(user = user))
            }
        },
        onParticipantDeleted = { participant ->
            this@ConversationsRepositoryImpl.launch {
                localCache.participantsDao().delete(participant.asParticipantDataItem())
            }
        },
        onMessageDeleted = { message ->
            deleteMessage(message)
        },
        onMessageUpdated = { message, reason ->
            updateMessage(message, reason)
        },
        onMessageAdded = { message ->
            addMessage(message)
        }
    )

    private fun launch(block: suspend CoroutineScope.() -> Unit) = repositoryScope.launch(
        //context = CoroutineExceptionHandler { _, e -> Timber.e(e, "Coroutine failed ${e.localizedMessage}") },
        block = block
    )

    override fun getUserConversations(): Flow<RepositoryResult<List<ConversationDataItem>>> {
        val localDataFlow = localCache.conversationsDao().getUserConversations()
        val fetchStatusFlow = fetchConversations().flowOn(dispatchers.io())

        return combine(localDataFlow, fetchStatusFlow) { data, status -> RepositoryResult(data, status) }
    }

    override fun getConversation(conversationSid: String): Flow<RepositoryResult<ConversationDataItem?>> {
        val localDataFlow = localCache.conversationsDao().getConversation(conversationSid)
        val fetchStatusFlow = fetchConversation(conversationSid).flowOn(dispatchers.io())

        return combine(localDataFlow, fetchStatusFlow) { data, status -> RepositoryResult(data, status) }
    }

    override fun getMessageByUuid(messageUuid: String) = localCache.messagesDao().getMessageByUuid(messageUuid)

    override fun getMessages(conversationSid: String, pageSize: Int): Flow<RepositoryResult<PagedList<MessageListViewItem>>> {
        val requestStatusConversation = Channel<RepositoryRequestStatus>(Channel.BUFFERED)
        val boundaryCallback = object : PagedList.BoundaryCallback<MessageListViewItem>() {
            override fun onZeroItemsLoaded() {
                launch {
                    fetchMessages(conversationSid) { getLastMessages(pageSize) }
                        .flowOn(dispatchers.io())
                        .collect {it: RepositoryRequestStatus->
                            requestStatusConversation.send(it)
                        }
                }
            }

            override fun onItemAtEndLoaded(itemAtEnd: MessageListViewItem) {
            }

            override fun onItemAtFrontLoaded(itemAtFront: MessageListViewItem) {
                if (itemAtFront.index > 0) {
                    launch {
                        fetchMessages(conversationSid) { getMessagesBefore(itemAtFront.index - 1, pageSize) }
                            .flowOn(dispatchers.io())
                            .collect {it:RepositoryRequestStatus->
                                requestStatusConversation.send(it)
                            }
                    }
                }
            }
        }

        val pagedListFlow = localCache.messagesDao().getMessagesSorted(conversationSid)
            //.mapByPage { it?.asMessageListViewItems() } //Commeted by Hardik
            .mapByPage { it.asMessageListViewItems() }
            .toFlow(
                pageSize = pageSize,
                boundaryCallback = boundaryCallback
            )
            .onStart {
                requestStatusConversation.send(FETCHING)
            }
            .onEach {
                requestStatusConversation.send(COMPLETE)
            }

        return combine(pagedListFlow, requestStatusConversation.consumeAsFlow().distinctUntilChanged()) { data, status ->
            RepositoryResult(data, status)
        }
    }

    override fun insertMessage(message: MessageDataItem) {
        launch {
            localCache.messagesDao().insertOrReplace(message)
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun updateMessageByUuid(message: MessageDataItem) {
        launch {
            localCache.messagesDao().updateByUuidOrInsert(message)
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun updateMessageStatus(messageUuid: String, sendStatus: Int, errorCode: Int) {
        launch {
            localCache.messagesDao().updateMessageStatus(messageUuid, sendStatus, errorCode)

            val message = localCache.messagesDao().getMessageByUuid(messageUuid) ?: return@launch
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>> =
        localCache.participantsDao().getTypingParticipants(conversationSid)

    override fun getConversationParticipants(conversationSid: String): Flow<RepositoryResult<List<ParticipantDataItem>>> {
        val localDataFlow = localCache.participantsDao().getAllParticipants(conversationSid)
        val fetchStatusFlow = fetchParticipants(conversationSid).flowOn(dispatchers.io())

        return combine(localDataFlow, fetchStatusFlow) { data, status -> RepositoryResult(data, status) }
    }

    override fun updateMessageMediaDownloadStatus(
        messageSid: String,
        downloadId: Long?,
        downloadLocation: String?,
        downloadState: Int?,
        downloadedBytes: Long?
    ) {
        launch {
            if (downloadId != null) {
                localCache.messagesDao().updateMediaDownloadId(messageSid, downloadId)
            }
            if (downloadLocation != null) {
                localCache.messagesDao().updateMediaDownloadLocation(messageSid, downloadLocation)
            }
            if (downloadState != null) {
                localCache.messagesDao().updateMediaDownloadState(messageSid, downloadState)
            }
            if (downloadedBytes != null) {
                localCache.messagesDao().updateMediaDownloadedBytes(messageSid, downloadedBytes)
            }
        }
    }

    override fun updateMessageMediaUploadStatus(
        messageUuid: String,
        uploading: Boolean?,
        uploadedBytes: Long?
    ) {
        launch {
            if (uploading != null) {
                localCache.messagesDao().updateMediaUploadStatus(messageUuid, uploading)
            }
            if (uploadedBytes != null) {
                localCache.messagesDao().updateMediaUploadedBytes(messageUuid, uploadedBytes)
            }
        }
    }

    override fun simulateCrash(where: CrashIn) {
        launch {
            conversationsClientWrapper.getConversationsClient().simulateCrash(where)
        }
    }

    override fun clear() {
        launch {
            localCache.clearAllTables()
        }
    }

    override fun getSelfUser(): Flow<User> = callbackFlow {
        val client = conversationsClientWrapper.getConversationsClient()
        val listener = ConversationsClientListener(
            onUserUpdated = { user, _ ->
                user.takeIf { it.identity == client.myIdentity }
                    ?.let { trySend(it).isSuccess }
            }
        )
        client.addListener(listener)
        send(client.myUser)
        awaitClose { client.removeListener(listener) }
    }

    private fun fetchMessages(conversationSid: String, fetch: suspend Conversation.() -> List<Message>) = flow {
        emit(FETCHING)
        try {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            val messages = conversationsClientWrapper
                .getConversationsClient()
                .getConversation(conversationSid)
                .apply { waitForSynchronization() }
                .fetch()
                .asMessageDataItems(identity)
            localCache.messagesDao().insert(messages)
            if (messages.isNotEmpty()) {
                updateConversationLastMessage(conversationSid)
            }
            emit(COMPLETE)
        } catch (e: TwilioException) {
            emit(Error(e.toConversationsError()))
            e.printStackTrace()

//            EETLog.error(
//                SessionHelper.appContext, LogConstants.logDetails(
//                    e,
//                    LogConstants.LOG_LEVEL.ERROR.toString(),
//                    LogConstants.LOG_SEVERITY.HIGH.toString()
//                ),
//                Constants.EX, LogTraceConstants.getUtilityData(
//                    SessionHelper.appContext!!
//                )!!
//            );
        }
    }

    private fun fetchConversation(conversationSid: String) = flow {
        emit(FETCHING)
        try {
            insertOrUpdateConversation(conversationSid)
            emit(COMPLETE)
        } catch (e: TwilioException) {
            emit(Error(e.toConversationsError()))
            e.printStackTrace()

//            EETLog.error(
//                SessionHelper.appContext, LogConstants.logDetails(
//                    e,
//                    LogConstants.LOG_LEVEL.ERROR.toString(),
//                    LogConstants.LOG_SEVERITY.HIGH.toString()
//                ),
//                Constants.EX, LogTraceConstants.getUtilityData(
//                    SessionHelper.appContext!!
//                )!!
//            );
        }
    }

    private fun fetchParticipants(conversationSid: String) = flow {
        emit(FETCHING)
        try {
            val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
            conversation.waitForSynchronization()
            conversation.participantsList.forEach { participant ->
                // Getting user is currently supported for chat participants only
                Log.d("userabove", "insideit")
                val user = if (participant.channel == "chat") participant.getAndSubscribeUser() else null
                Log.d("user", user.toString())
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(user = user))
            }
            emit(COMPLETE)
        } catch (e: TwilioException) {
            emit(Error(e.toConversationsError()))
            e.printStackTrace()

            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }

    private fun fetchConversations() = channelFlow {
        send(FETCHING)

        try {
            // get items from client
            val dataItems = conversationsClientWrapper
                .getConversationsClient()
                .myConversations
                .map { it.toConversationDataItem() }

            localCache.conversationsDao().deleteGoneUserConversations(dataItems)
            send(SUBSCRIBING)

            var status: RepositoryRequestStatus = COMPLETE
            supervisorScope {
                // get all conversations and update conversation data in local cache
                dataItems.forEach {
                    launch {
                        try {
                            insertOrUpdateConversation(it.sid)
                        } catch (e: TwilioException) {
                            status = Error(e.toConversationsError())
                        }
                    }
                }
            }
            send(status)
        } catch (e: TwilioException) {
            send(Error(e.toConversationsError()))
            e.printStackTrace()

            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }

    override fun subscribeToConversationsClientEvents() {
        launch {
            conversationsClientWrapper.getConversationsClient().addListener(clientListener)
        }
    }

    override fun unsubscribeFromConversationsClientEvents() {
        launch {
            conversationsClientWrapper.getConversationsClient().removeListener(clientListener)
        }
    }

    private suspend fun insertOrUpdateConversation(conversationSid: String) {
        val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
        conversation.addListener(conversationListener)
        localCache.conversationsDao().insert(conversation.toConversationDataItem())
        localCache.conversationsDao().update(conversation.sid,
            conversation.status.value, conversation.notificationLevel.value, conversation.friendlyName)
        launch {
            localCache.conversationsDao().updateParticipantCount(conversationSid, conversation.getParticipantCount())
        }
        launch {
            localCache.conversationsDao().updateMessagesCount(conversationSid, conversation.getMessageCount())
        }
        launch {
            localCache.conversationsDao().updateUnreadMessagesCount(conversationSid, conversation.getUnreadMessageCount() ?: return@launch)
        }
        launch {
            updateConversationLastMessage(conversationSid)
        }
    }

    private suspend fun updateConversationLastMessage(conversationSid: String) {
        val lastMessage = localCache.messagesDao().getLastMessage(conversationSid)
        if (lastMessage != null) {
            localCache.conversationsDao().updateLastMessage(
                conversationSid, lastMessage.body ?: "", lastMessage.sendStatus, lastMessage.dateCreated)
        } else {
            fetchMessages(conversationSid) { getLastMessages(10) }.collect()
        }
    }

    private fun deleteMessage(message: Message) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            localCache.messagesDao().delete(message.toMessageDataItem(identity))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    private fun updateMessage(message: Message, updateReason: Message.UpdateReason? = null) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            val uuid = localCache.messagesDao().getMessageBySid(message.sid)?.uuid ?: ""
            localCache.messagesDao().insertOrReplace(message.toMessageDataItem(identity, uuid))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    private fun addMessage(message: Message) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            localCache.messagesDao().updateByUuidOrInsert(message.toMessageDataItem(identity, message.attributes.string ?: ""))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    companion object {
        val INSTANCE get() = _instance ?: error("call ConversationsRepository.createInstance() first")

        private var _instance: ConversationsRepository? = null

        fun createInstance(conversationsClientWrapper: ConversationsClientWrapper, localCache: LocalCacheProvider) {
            check(_instance == null) { "ConversationsRepository singleton instance has been already created" }
            _instance = ConversationsRepositoryImpl(conversationsClientWrapper, localCache)
        }
    }
}