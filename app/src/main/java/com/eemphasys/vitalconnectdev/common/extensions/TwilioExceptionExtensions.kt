package com.eemphasys.vitalconnectdev.common.extensions

import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.enums.toErrorInfo
import com.twilio.util.TwilioException

fun TwilioException.toConversationsError(): ConversationsError =
    ConversationsError.fromErrorInfo(errorInfo)

fun createTwilioException(error: ConversationsError): TwilioException =
    TwilioException(error.toErrorInfo())