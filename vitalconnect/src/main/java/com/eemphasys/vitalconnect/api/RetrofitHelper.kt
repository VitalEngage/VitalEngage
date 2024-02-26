package com.eemphasys.vitalconnect.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {

    val baseUrl = "https://xappsweb.e-emphasys.com/VitalConnect/WebApi/"

//    fun getInstance(): Retrofit {
        fun getInstance(httpClient: OkHttpClient? = null): Retrofit {

            val builder = Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())

            if (httpClient != null) {
                    builder.client(httpClient)
                }

                return builder.build()

//            return Retrofit.Builder().baseUrl(baseUrl)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
        }


}