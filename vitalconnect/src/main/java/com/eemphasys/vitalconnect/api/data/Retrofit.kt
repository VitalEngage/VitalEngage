package com.eemphasys.vitalconnect.api.data

import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


val token = "YOUR_AUTH_TOKEN"
val authInterceptor = AuthInterceptor(token)


val retrofit = Retrofit.Builder()
    .baseUrl("https://xappsweb.e-emphasys.com/VitalConnect/WebApi/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(TwilioApi::class.java)