package com.eemphasys.vitalconnect.data.models

import com.eemphasys.vitalconnect.common.enums.ConversationsError

sealed class RepositoryRequestStatus {
    object FETCHING : RepositoryRequestStatus()
    object SUBSCRIBING : RepositoryRequestStatus()
    object COMPLETE : RepositoryRequestStatus()
    class Error(val error: ConversationsError) : RepositoryRequestStatus()
}