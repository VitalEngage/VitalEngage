package com.eemphasys.vitalconnect.api

import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://your.api.url/" // Replace with your actual base URL

    private val httpClientWithToken: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.getStringFromVitalTextSharedPreferences(AppContextHelper.appContext, "authToken")!!))
            .addInterceptor(RetryInterceptor())
            .build()
    }

    val retrofitWithToken: TwilioApi by lazy {
        RetrofitHelper.getInstance(AppContextHelper.appContext, httpClientWithToken)
            .create(TwilioApi::class.java)
    }
}
