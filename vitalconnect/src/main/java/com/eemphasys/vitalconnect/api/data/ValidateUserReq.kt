package com.eemphasys.vitalconnect.api.data

data class ValidateUserReq(
    val username : String,
    val password : String,
    val tenantCode : String,
    val email : String,
    val lastActivityDate : String,
    val rememberMe : Boolean,
    val azureAdToken : String,
    val reCaptchaToken : String
)
