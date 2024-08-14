package com.eemphasys.vitalconnect.viewModel

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.repository.ConversationsRepository
import com.eemphasys.vitalconnect.common.SingleLiveEvent
import com.eemphasys.vitalconnect.common.asParticipantListViewItems
import com.eemphasys.vitalconnect.common.call
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.enums.DownloadState
import com.eemphasys.vitalconnect.common.enums.Reactions
import com.eemphasys.vitalconnect.common.enums.SendStatus
import com.eemphasys.vitalconnect.common.extensions.getInt
import com.eemphasys.vitalconnect.common.extensions.getLong
import com.eemphasys.vitalconnect.common.extensions.getString
import com.eemphasys.vitalconnect.common.extensions.queryById
import com.eemphasys.vitalconnect.data.localCache.entity.ParticipantDataItem
import com.eemphasys.vitalconnect.data.models.MediaMessagePreviewItem
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.data.models.RepositoryRequestStatus
import com.eemphasys.vitalconnect.manager.ConnectivityMonitor
import com.eemphasys.vitalconnect.manager.MessageListManager
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceHelper
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.util.TwilioException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.*

const val MESSAGE_COUNT = 50

class MessageListViewModel(
    private val appContext: Context,
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository,
    private val messageListManager: MessageListManager,
    connectivityMonitor : ConnectivityMonitor
) : ViewModel() {

    val conversationName = MutableLiveData<String>()

    val selfUser = conversationsRepository.getSelfUser().asLiveData(viewModelScope.coroutineContext)

    val messageItems = conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT)
        .onEach { repositoryResult ->
            if (repositoryResult.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.postValue(ConversationsError.MESSAGE_FETCH_FAILED)
            }
        }
        .asLiveData(viewModelScope.coroutineContext)
        .map { it.data }

    val onMessageError = SingleLiveEvent<ConversationsError>()
    val mediaMessagePreview = SingleLiveEvent<MediaMessagePreviewItem>()
    private val onMessageSent = SingleLiveEvent<Unit>()

    val onShowRemoveMessageDialog = SingleLiveEvent<Unit>()

    val onMessageRemoved = SingleLiveEvent<Unit>()

    val onMessageCopied = SingleLiveEvent<Unit>()

    var selectedMessageIndex: Long = -1

    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val selectedMessage: MessageListViewItem? get() = messageItems.value?.firstOrNull { it.index == selectedMessageIndex }

    val typingParticipantsList = conversationsRepository.getTypingParticipants(conversationSid)
        .map { participants -> participants.map { it.typingIndicatorName } }
        .distinctUntilChanged()
        .asLiveData(viewModelScope.coroutineContext)

    private val messagesObserver: Observer<PagedList<MessageListViewItem>> =
        Observer { list ->
            list.forEach { message ->
                if (message?.mediaDownloadState == DownloadState.DOWNLOADING && message.mediaDownloadId != null) {
                    if (updateMessageMediaDownloadState(message.index, message.mediaDownloadId)) {
                        observeMessageMediaDownload(message.index, message.mediaDownloadId)
                    }
                }
            }
        }

    init {
        viewModelScope.launch {
            getConversationResult()
        }
        messageItems.observeForever(messagesObserver)
    }

    override fun onCleared() {
        messageItems.removeObserver(messagesObserver)
    }

    private suspend fun getConversationResult() {
        EETLog.saveUserJourney("vitaltext:  MessagelistViewModel getConversationResult Called")
        conversationsRepository.getConversation(conversationSid).collect { result ->
            if (result.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.value = ConversationsError.CONVERSATION_GET_FAILED
                return@collect
            }
            conversationName.value = result.data?.friendlyName?.takeIf { it.isNotEmpty() } ?: result.data?.sid
        }
    }

    fun sendTextMessage(message: String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  MessagelistViewModel sendTextMessage Called")
        val messageUuid = UUID.randomUUID().toString()
        try {
            messageListManager.sendTextMessage(message, messageUuid)
            onMessageSent.call()
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Send Text Message",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: TwilioException) {
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo.code)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            );
        }
    }

    fun resendTextMessage(messageUuid: String) = viewModelScope.launch {
        try {
            messageListManager.retrySendTextMessage(messageUuid)
            onMessageSent.call()
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Resend Text Message",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: TwilioException) {
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo.code)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            );
        }
    }

    fun sendMediaMessage(uri: String, inputStream: InputStream, fileName: String?, mimeType: String?) =
        viewModelScope.launch {
            val messageUuid = UUID.randomUUID().toString()
            try {
                messageListManager.sendMediaMessage(uri, inputStream, fileName, mimeType, messageUuid)
                onMessageSent.call()
                LogTraceHelper.trace(
                    appContext,
                    LogTraceConstants.traceDetails(
                        Thread.currentThread().stackTrace,
                        "Send Media message",
                        LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                        LogConstants.LOG_SEVERITY.NORMAL.toString()
                    ),
                    LogTraceConstants.chatappmodel,
                    LogTraceConstants.getUtilityData(appContext)!!
                )
            } catch (e: TwilioException) {
                messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo.code)
                onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
                e.printStackTrace()

                EETLog.error(
                    AppContextHelper.appContext!!, LogConstants.logDetails(
                        e,
                        LogConstants.LOG_LEVEL.ERROR.toString(),
                        LogConstants.LOG_SEVERITY.HIGH.toString()
                    ),
                    Constants.EX, LogTraceConstants.getUtilityData(
                        AppContextHelper.appContext!!
                    )!!
                )
            }
            catch (e: Exception) {
                e.printStackTrace()
                EETLog.error(
                    AppContextHelper.appContext!!, LogConstants.logDetails(
                        e,
                        LogConstants.LOG_LEVEL.ERROR.toString(),
                        LogConstants.LOG_SEVERITY.HIGH.toString()
                    ),
                    Constants.EX, LogTraceConstants.getUtilityData(
                        AppContextHelper.appContext!!
                    )!!
                );
            }
        }

    fun resendMediaMessage(inputStream: InputStream, messageUuid: String) = viewModelScope.launch {
        try {
            messageListManager.retrySendMediaMessage(inputStream, messageUuid)
            onMessageSent.call()
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Resend Media Message",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: TwilioException) {
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo.code)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            );
        }
    }

    fun handleMessageDisplayed(messageIndex: Long) = viewModelScope.launch {
        try {
            messageListManager.notifyMessageRead(messageIndex)
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Handle Message display",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: TwilioException) {
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            );
        }
    }

    fun typing() = viewModelScope.launch {
        messageListManager.typing()
    }

    fun setReactions(reactions: Reactions) = viewModelScope.launch {
        try {
            messageListManager.setReactions(selectedMessageIndex, reactions)
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Set Reactions",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: TwilioException) {
            onMessageError.value = ConversationsError.REACTION_UPDATE_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
    }

    fun copyMessageToClipboard() {
        try {
            val message = selectedMessage ?: error("No message selected")
            val clip = ClipData.newPlainText("Message text", message.body)
            val clipboard = ContextCompat.getSystemService(appContext, ClipboardManager::class.java)
            clipboard!!.setPrimaryClip(clip)
            onMessageCopied.call()
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Copy Message to clipboard",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: Exception) {
            onMessageError.value = ConversationsError.MESSAGE_COPY_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
    }

    fun removeMessage() = viewModelScope.launch {
        try {
            messageListManager.removeMessage(selectedMessageIndex)
            onMessageRemoved.call()
            LogTraceHelper.trace(
                appContext,
                LogTraceConstants.traceDetails(
                    Thread.currentThread().stackTrace,
                    "Remove message",
                    LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                    LogConstants.LOG_SEVERITY.NORMAL.toString()
                ),
                LogTraceConstants.chatappmodel,
                LogTraceConstants.getUtilityData(appContext)!!
            )
        } catch (e: TwilioException) {
            onMessageError.value = ConversationsError.MESSAGE_REMOVE_FAILED
            e.printStackTrace()

            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            );
        }
    }

    private fun updateMessageMediaDownloadStatus(
        messageIndex: Long,
        downloadState: DownloadState,
        downloadedBytes: Long = 0,
        downloadedLocation: String? = null
    ) = viewModelScope.launch {
        messageListManager.updateMessageMediaDownloadState(
            messageIndex,
            downloadState,
            downloadedBytes,
            downloadedLocation
        )
    }

    fun startMessageMediaDownload(messageIndex: Long, fileName: String?) = viewModelScope.launch {
        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING)

        val sourceUriResult = runCatching { Uri.parse(messageListManager.getMediaContentTemporaryUrl(messageIndex)) }
        val sourceUri = sourceUriResult.getOrElse { e ->
            updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR)
            return@launch
        }

        val downloadManager =
            appContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadRequest = DownloadManager.Request(sourceUri).apply {
            setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                fileName ?: sourceUri.pathSegments.last()
            )
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        val downloadId = downloadManager.enqueue(downloadRequest)

        messageListManager.setMessageMediaDownloadId(messageIndex, downloadId)
        observeMessageMediaDownload(messageIndex, downloadId)
        LogTraceHelper.trace(
            appContext,
            LogTraceConstants.traceDetails(
                Thread.currentThread().stackTrace,
                "State Media message donwloading",
                LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                LogConstants.LOG_SEVERITY.NORMAL.toString()
            ),
            LogTraceConstants.chatappmodel,
            LogTraceConstants.getUtilityData(appContext)!!
        )
    }

    private fun observeMessageMediaDownload(messageIndex: Long, downloadId: Long) {
        val downloadManager = appContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadCursor = downloadManager.queryById(downloadId)
        val downloadObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (!updateMessageMediaDownloadState(messageIndex, downloadId)) {
                    downloadCursor.unregisterContentObserver(this)
                    downloadCursor.close()
                }
            }
        }
        downloadCursor.registerContentObserver(downloadObserver)
        LogTraceHelper.trace(
            appContext,
            LogTraceConstants.traceDetails(
                Thread.currentThread().stackTrace,
                "Observe Media Message download",
                LogConstants.TRACE_LEVEL.UI_TRACE.toString(),
                LogConstants.LOG_SEVERITY.NORMAL.toString()
            ),
            LogTraceConstants.chatappmodel,
            LogTraceConstants.getUtilityData(appContext)!!
        )
    }

    /**
     * Notifies the view model of the current download state
     * @return true if the download is still in progress
     */
    private fun updateMessageMediaDownloadState(messageIndex: Long, downloadId: Long): Boolean {
        val downloadManager = appContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.queryById(downloadId)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }

        val status = cursor.getInt(DownloadManager.COLUMN_STATUS)
        val downloadInProgress = status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL
        val downloadedBytes = cursor.getLong(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING, downloadedBytes)

        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                val downloadedFile = cursor.getString(DownloadManager.COLUMN_LOCAL_URI).toUri().toFile()
                val downloadedLocation =
                    FileProvider.getUriForFile(appContext, ChatAppModel.appId + ".provider", downloadedFile)
                        .toString()
                updateMessageMediaDownloadStatus(
                    messageIndex,
                    DownloadState.COMPLETED,
                    downloadedBytes,
                    downloadedLocation
                )
            }
            DownloadManager.STATUS_FAILED -> {
                onMessageError.value = ConversationsError.MESSAGE_MEDIA_DOWNLOAD_FAILED
                updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR, downloadedBytes)
            }
        }

        cursor.close()
        return downloadInProgress
    }

    fun initParticipant(channelSid : String) = viewModelScope.launch {
        EETLog.saveUserJourney("vitaltext:  MessagelistViewModel initParticipant Called")
        conversationsRepository.getConversationParticipants(channelSid).collect{(list)->
            Constants.PARTICIPANTS = list.asParticipantListViewItems()
            conversationsRepository.updateFriendlyName()
                }
        }
    private val ParticipantDataItem.typingIndicatorName get() = if (friendlyName.isNotEmpty()) friendlyName else identity
}
