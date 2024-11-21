package com.eemphasys.vitalconnect.api

import android.util.Log
import com.eemphasys.vitalconnect.common.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val session = SessionManager()

        if (session.isAccessTokenExpired()) {
            Log.d("istokenExpired","true")
            val renewedToken = session.getRenewedToken()
            // Create a new request with the refreshed access token
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $renewedToken")
                .build()
            Log.d("renewedtokeninAuthInterceptor",renewedToken)

            // Retry the request with the new access token
            return chain.proceed(newRequest)
        } else {
            Log.d("istokenExpired","false")
            // Add the access token to the request header
            val authorizedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(authorizedRequest)
        }
    }
}