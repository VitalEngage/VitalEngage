package com.eemphasys.vitalconnect.common.extensions

import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.enums.toErrorInfo
import com.twilio.util.TwilioException

fun TwilioException.toConversationsError(): ConversationsError =
    ConversationsError.fromErrorInfo(errorInfo)

fun createTwilioException(error: ConversationsError): TwilioException =
    TwilioException(error.toErrorInfo())
