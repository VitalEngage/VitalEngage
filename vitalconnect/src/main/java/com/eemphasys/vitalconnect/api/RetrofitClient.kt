package com.eemphasys.vitalconnect.api

import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://your.api.url/" // Replace with your actual base URL

    private fun getHttpClient(): OkHttpClient {
        val authToken = Constants.getStringFromVitalTextSharedPreferences(AppContextHelper.appContext, "authToken") ?: ""
        return OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(authToken))
            .addInterceptor(RetryInterceptor())
            .build()
    }

    fun getRetrofitWithToken(): TwilioApi {
        return RetrofitHelper.getInstance(AppContextHelper.appContext, getHttpClient())
            .create(TwilioApi::class.java)
    }
}
