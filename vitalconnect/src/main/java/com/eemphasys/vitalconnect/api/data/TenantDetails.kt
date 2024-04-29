package com.eemphasys.vitalconnect.api.data

data class TenantDetails(
    val isAADEnabled : Boolean,
    val AzureAdClientID : String,
    val AzureAdTenantId : String
)
