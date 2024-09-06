package com.eemphasys.vitalconnect.common

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.twilio.conversations.Conversation.NotificationLevel
import androidx.core.net.toUri
import com.eemphasys.vitalconnect.common.enums.DownloadState
import com.eemphasys.vitalconnect.common.enums.Reaction
import com.eemphasys.vitalconnect.common.enums.SendStatus
import com.eemphasys.vitalconnect.common.enums.Direction
import com.eemphasys.vitalconnect.common.enums.MessageType
import com.eemphasys.vitalconnect.common.enums.Reactions
import com.eemphasys.vitalconnect.common.extensions.asDateString
import com.eemphasys.vitalconnect.common.extensions.asLastMesageStatusIcon
import com.eemphasys.vitalconnect.common.extensions.asLastMessageDateString
import com.eemphasys.vitalconnect.common.extensions.asLastMessageTextColor
import com.eemphasys.vitalconnect.common.extensions.asMessageCount
import com.eemphasys.vitalconnect.common.extensions.asMessageDateChangedString
import com.eemphasys.vitalconnect.common.extensions.asMessageDateString
import com.eemphasys.vitalconnect.common.extensions.firstMedia
import com.eemphasys.vitalconnect.data.models.ParticipantListViewItem
import com.eemphasys.vitalconnect.data.models.UserViewItem
import com.eemphasys.vitalconnect.data.localCache.entity.ConversationDataItem
import com.eemphasys.vitalconnect.data.localCache.entity.MessageDataItem
import com.eemphasys.vitalconnect.data.localCache.entity.ParticipantDataItem
import com.eemphasys.vitalconnect.data.models.ConversationDetailsViewItem
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.data.models.ReactionAttributes
import com.eemphasys.vitalconnect.manager.friendlyName
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.twilio.conversations.Conversation
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.User
import org.json.JSONObject

fun Conversation.toConversationDataItem(): ConversationDataItem {
    return ConversationDataItem(
        this.sid,
        this.friendlyName,
        this.attributes.toString(),
        this.uniqueName,
        this.dateUpdatedAsDate?.time ?: 0,
        this.dateCreatedAsDate?.time ?: 0,
        0,
        "",
        SendStatus.UNDEFINED.value,
        this.createdBy,
        0,
        0,
        0,
        this.status.value,
        this.notificationLevel.value
    )
}

fun Message.toMessageDataItem(currentUserIdentity: String = participant.identity, uuid: String = ""): MessageDataItem {
    val media = firstMedia  // @todo: support multiple media
    return MessageDataItem(
        this.sid,
        this.conversationSid,
        this.participantSid,
        if (media != null) MessageType.MEDIA.value else MessageType.TEXT.value,
        this.author,
        this.dateCreatedAsDate.time,
        this.body ?: "",
        this.messageIndex,
        this.attributes.toString(),
        if (this.author == currentUserIdentity) Direction.OUTGOING.value else Direction.INCOMING.value,
        if (this.author == currentUserIdentity) SendStatus.SENT.value else SendStatus.UNDEFINED.value,
        uuid,
        media?.sid,
        media?.filename,
        media?.contentType,
        media?.size,
    )
}

fun MessageDataItem.toMessageListViewItem(authorChanged: Boolean,datechanged: Boolean): MessageListViewItem {
    return MessageListViewItem(
        this.sid,
        this.uuid,
        this.index,
        Direction.fromInt(this.direction),
        this.author,
        authorChanged,
        this.body ?: "",
        this.dateCreated.asMessageDateString(),
        SendStatus.fromInt(sendStatus),
        sendStatusIcon = SendStatus.fromInt(this.sendStatus).asLastMesageStatusIcon(),
        getReactions(attributes).asReactionList(),
        MessageType.fromInt(this.type),
        this.mediaSid,
        this.mediaFileName,
        this.mediaType,
        this.mediaSize,
        this.mediaUri?.toUri(),
        this.mediaDownloadId,
        this.mediaDownloadedBytes,
        DownloadState.fromInt(this.mediaDownloadState),
        this.mediaUploading,
        this.mediaUploadedBytes,
        this.mediaUploadUri?.toUri(),
        this.errorCode,
        this.friendlyName ?: "",
        datechanged,
        this.dateCreated.asMessageDateChangedString()
    )
}

fun getReactions(attributes: String): Map<String, Set<String>> = try {
    Gson().fromJson(attributes, ReactionAttributes::class.java).reactions
} catch (e: Exception) {
    /*EETLog.error(
        AppContextHelper.appContext!!, LogConstants.logDetails(
            e,
            LogConstants.LOG_LEVEL.ERROR.toString(),
            LogConstants.LOG_SEVERITY.HIGH.toString()
        ),
        Constants.EX, LogTraceConstants.getUtilityData(
            AppContextHelper.appContext!!
        )!!
    )*/
    emptyMap()
}

@SuppressLint("NewApi")
fun Map<String, Set<String>>.asReactionList(): Reactions {
    val reactions: MutableMap<Reaction, Set<String>> = mutableMapOf()
    forEach {
        try {
            reactions[Reaction.fromString(it.key)] = it.value
        } catch (e: Exception) {
            EETLog.error(
                AppContextHelper.appContext, LogConstants.logDetails(
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
    return reactions
}

fun Participant.asParticipantDataItem(typing: Boolean = false, user: User? = null) = ParticipantDataItem(
    sid = this.sid,
    conversationSid = this.conversation.sid,
    identity = this.identity,
    friendlyName = user?.friendlyName?.takeIf { it.isNotEmpty() } ?: this.friendlyName ?: this.identity,
    isOnline = user?.isOnline ?: false,
    lastReadMessageIndex = this.lastReadMessageIndex,
    lastReadTimestamp = this.lastReadTimestamp,
    typing = typing
)

fun User.asUserViewItem() = UserViewItem(
    friendlyName = this.friendlyName,
    identity = this.identity
)

fun ConversationDataItem.asConversationListViewItem(
    context: Context,
) = ConversationListViewItem(
    this.sid,
    if (this.friendlyName.isNotEmpty()) this.friendlyName else this.sid,
    this.participantsCount.toInt(),
    this.unreadMessagesCount.asMessageCount(),
    showUnreadMessageCount = this.unreadMessagesCount > 0,
    this.participatingStatus,
    lastMessageStateIcon = SendStatus.fromInt(this.lastMessageSendStatus).asLastMesageStatusIcon(),
    this.lastMessageText,
    lastMessageColor = SendStatus.fromInt(this.lastMessageSendStatus).asLastMessageTextColor(context),
    this.lastMessageDate.asLastMessageDateString(context),
    isMuted = this.notificationLevel == NotificationLevel.MUTED.value,
    false,
    try {
        JSONObject(this.attributes).optString("Department", "")
    } catch (e: Exception) {
        ""
    },
    try {
        JSONObject(this.attributes).optString("Designation", "")
    } catch (e: Exception) {
        ""
    },
    try {
        JSONObject(this.attributes).optString("CustomerName", "")
    } catch (e: Exception) {
        ""
    },
    this.messagesCount,
    try {
        JSONObject(this.attributes).optString("isWebChat", " ")
    } catch (e: Exception) {
        " "
    }
)

fun ConversationDataItem.asConversationDetailsViewItem() = ConversationDetailsViewItem(
    this.sid,
    this.friendlyName,
    this.createdBy,
    this.dateCreated.asDateString(),
    this.notificationLevel == NotificationLevel.MUTED.value,
    ConversationsRepositoryImpl.INSTANCE.getFriendlyName(this.createdBy)
)

fun ParticipantDataItem.toParticipantListViewItem() = ParticipantListViewItem(
    conversationSid = this.conversationSid,
    sid = this.sid,
    identity = this.identity,
    friendlyName = this.friendlyName,
    isOnline = this.isOnline
)

fun List<ConversationDataItem>.asConversationListViewItems(context: Context) =
    map { it.asConversationListViewItem(context) }

fun List<Message>.asMessageDataItems(identity: String) = map { it.toMessageDataItem(identity) }

fun List<MessageDataItem>.asMessageListViewItems() =
    mapIndexed { index, item -> item.toMessageListViewItem(isAuthorChanged(index),isDateChanged(index)) }

private fun List<MessageDataItem>.isAuthorChanged(index: Int): Boolean {
    if (index == 0) return true
    return this[index].author != this[index - 1].author
}

private fun List<MessageDataItem>.isDateChanged(index: Int): Boolean {
    if (index == 0) return true
    return this[index].dateCreated.asMessageDateChangedString() != this[index - 1].dateCreated.asMessageDateChangedString()
}

fun List<ParticipantDataItem>.asParticipantListViewItems() = map { it.toParticipantListViewItem() }

fun List<ConversationListViewItem>.merge(oldConversationList: List<ConversationListViewItem>?): List<ConversationListViewItem> {
    val oldConversationMap = oldConversationList?.associate { it.sid to it } ?: return this
    return map { item ->
        val oldItem = oldConversationMap[item.sid] ?: return@map item
        item.copy(isLoading = oldItem.isLoading)
    }
}
