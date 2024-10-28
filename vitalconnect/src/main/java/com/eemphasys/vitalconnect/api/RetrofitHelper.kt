package com.eemphasys.vitalconnect.api

import android.content.Context
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {

        fun getInstance(applicationContext: Context, httpClient: OkHttpClient? = null): Retrofit {

            val builder = Retrofit.Builder().baseUrl(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"baseUrl")!!)
                .addConverterFactory(GsonConverterFactory.create())

            if (httpClient != null) {
                    builder.client(httpClient)
                }
                return builder.build()
        }
}