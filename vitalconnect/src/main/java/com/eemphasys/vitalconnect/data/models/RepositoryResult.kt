package com.eemphasys.vitalconnect.data.models

data class RepositoryResult<T> (
    val data: T,
    val requestStatus: RepositoryRequestStatus
)