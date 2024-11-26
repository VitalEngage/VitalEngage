package com.eemphasys.vitalconnect.common

import android.util.Log
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RenewTokenRequest
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class SessionManager {

    fun isAccessTokenExpired(): Boolean {
        val authTokenTimeStamp = Constants.getStringFromVitalTextSharedPreferences(
            AppContextHelper.appContext,
            "authTokenTimeStamp"
        )
        val expirationDuration = Constants.getStringFromVitalTextSharedPreferences(
            AppContextHelper.appContext,
            "expirationDuration"
        )!!.toInt()
        val authtokenExpiresAt = getTokenExpiry(authTokenTimeStamp!!,expirationDuration)

        val comparisonResult = compareTimestamps(Constants.getTimeStamp(),authtokenExpiresAt)

        return when {
            comparisonResult < 0 -> false  // Token has not expired (current time is before expiration)
            comparisonResult > 0 -> true   // Token has expired (current time is after expiration)
            else -> false
        }
    }

    fun getRenewedToken(): String {
        return runBlocking {
            val request = RenewTokenRequest(
                Constants.getStringFromVitalTextSharedPreferences(
                    AppContextHelper.appContext,
                    "refreshToken"
                )!!,
                Constants.getStringFromVitalTextSharedPreferences(
                    AppContextHelper.appContext,
                    "currentUser"
                )!!,
                Constants.getStringFromVitalTextSharedPreferences(
                    AppContextHelper.appContext,
                    "tenantCode"
                )!!
            )
            val apiService = RetrofitHelper.getInstance(AppContextHelper.appContext).create(
                TwilioApi::class.java
            )

            val response = apiService.getRenewedAuthToken(request)

            if(response.isSuccessful) {
                Log.d("renewedTokeninsessionmanager", response.body()!!.token)
                Log.d("refreshToken", response.body()!!.refreshToken)
                // Update the refreshed access token and its expiration time in the session
                updateAccessToken(response.body()!!.token, response.body()!!.refreshToken)
            }
            response.body()!!.token
        }
    }

    fun updateAccessToken(newtoken: String, refreshToken: String) {
        Constants.saveStringToVitalTextSharedPreferences(
            AppContextHelper.appContext,
            "authToken",
            newtoken
        )
        Constants.saveStringToVitalTextSharedPreferences(
            AppContextHelper.appContext,
            "refreshToken",
            refreshToken
        )
        Constants.saveStringToVitalTextSharedPreferences(AppContextHelper.appContext,"authTokenTimeStamp",Constants.getTimeStamp())

    }

    fun getTokenExpiry(currentTimestamp: String, minutesToAdd: Int): String {
        // Define the date format for parsing and formatting
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        // Parse the current timestamp into a Date object
        val currentDate: Date = sdf.parse(currentTimestamp) ?: return "" // Handle parsing error

        // Create a Calendar instance and set it to the current date
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = currentDate

        // Add hours to the calendar
        calendar.add(Calendar.MINUTE, minutesToAdd)

        // Return the formatted expiry time
        return sdf.format(calendar.time)
    }

    fun compareTimestamps(currentTimeStamp: String, expiryTimeStamp: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val date1 = sdf.parse(currentTimeStamp) ?: return -1
        val date2 = sdf.parse(expiryTimeStamp) ?: return -1

        Log.d("currentTimestamp",date1.toString())
        Log.d("expiryTimestamp",date2.toString())

        return date1.compareTo(date2)
    }
}