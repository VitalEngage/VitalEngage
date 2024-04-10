package com.eemphasys.vitalconnect.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RetryInterceptor: Interceptor{
private val MAX_RETRIES = 3
private val RETRY_DELAY = 10000 // Delay in milliseconds

@Throws(IOException::class)
override fun intercept(chain: Interceptor.Chain): Response {
    val request: Request = chain.request()
    var response: Response? = null

    // Retry loop
    for (retryCount in 0 until MAX_RETRIES) {
        try {
            response = chain.proceed(request)
            break // If successful response received, break the loop
        } catch (e: IOException) {
            // Wait before retrying
            try {
                Thread.sleep(RETRY_DELAY.toLong())
            } catch (ignored: InterruptedException) {
            }
        }
    }

    // If response is still null, throw an IOException
    if (response == null) {
        throw IOException("Failed to connect to the server after max retries.")
    }

    return response
}
}