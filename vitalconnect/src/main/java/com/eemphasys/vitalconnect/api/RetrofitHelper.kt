package com.eemphasys.vitalconnect.api

import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {

        fun getInstance(httpClient: OkHttpClient? = null): Retrofit {

            val builder = Retrofit.Builder().baseUrl(ChatAppModel.base_url)
                .addConverterFactory(GsonConverterFactory.create())

            if (httpClient != null) {
                    builder.client(httpClient)
                }
                return builder.build()
        }
}