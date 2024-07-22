package com.eemphasys.vitalconnect.api.data

data class TenantDetails(
    val isAADEnabled : Boolean,
    val azureAdClientID : String,
    val azureAdTenantId : String
)
