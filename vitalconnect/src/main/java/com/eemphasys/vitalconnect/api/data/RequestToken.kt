package com.eemphasys.vitalconnect.api.data

import com.google.gson.JsonObject

data class RequestToken(
    val tenantCode : String,
    val clientId : String,
    val clientSecret :String,
    val currentUser :String,
    val productCode: String
    //val jsonObject: JsonObject
)
