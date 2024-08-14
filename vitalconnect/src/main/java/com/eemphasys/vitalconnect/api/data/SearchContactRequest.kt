package com.eemphasys.vitalconnect.api.data

data class SearchContactRequest(
    val currentUser : String,
    val TenantCode : String,
    val SearchCriteria : String
)
