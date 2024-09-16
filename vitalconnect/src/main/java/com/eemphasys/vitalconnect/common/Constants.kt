package com.eemphasys.vitalconnect.common

import android.graphics.Color
import android.os.Build
import android.util.Log
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ContactListResponse
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.data.models.ParticipantListViewItem
import com.google.gson.Gson
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Random
import java.util.concurrent.TimeUnit

class Constants   {
    companion object{

        var BASE_URL : String = ""
        var TENANT_CODE : String = ""
        var TWILIO_TOKEN : String = ""
        var CLIENT_ID : String = ""
        var CLIENT_SECRET : String = ""
        var USERNAME :String = ""
        var PRODUCT : String = ""
        var FRIENDLY_NAME : String =""
        var CONTACTS : String = ""
        var WEBUSERS : String = ""
        var AUTH_TOKEN : String = ""
        var PROXY_NUMBER : String = ""
        var EX = "EX" //Exception
        var FULL_NAME : String = ""
        var SHOW_CONTACTS : String = ""
        var IS_STANDALONE : String = ""
        var CUSTOMER_NUMBER :String = ""
        var CUSTOMER_NAME :String = ""
        var SHOW_CONVERSATIONS :String=""
        var USER_SMS_ALERT : String= ""
        var PARTICIPANTS : List<ParticipantListViewItem> = listOf()
        var SHOW_DEPARTMENT :String = ""
        var SHOW_DESIGNATION :String = ""
        var DEPARTMENT: String = ""
        var DESIGNATION: String = ""
        var CUSTOMER : String= ""
        var COUNTRYCODE :String=""
        var EMAIL : String = ""
        var MOBILENUMBER :String = ""
        var DEFAULT_COUNTRYCODE :String = ""
        var CURRENT_CONVERSATION_ISWEBCHAT : String = ""
        var CURRENT_CONTACT = ContactListViewItem(
            name = "",
            email = "",
            number = "",
            type = "",
            initials = "",
            designation = null,
            department = null,
            customerName = null,
            countryCode = null,
            isGlobal = false
        )

        var URI : String = ""
        var INPUTSTREAM : InputStream = ByteArrayInputStream(ByteArray(0))
        var MEDIA_NAME : String? = ""
        var MEDIA_TYPE : String? = ""
        var TIME_OFFSET : Int? = 0
        var WITH_CONTEXT = ""
        var OPEN_CHAT = ""
        var CONTEXT = ""
        var DEALER_NAME =""
        var PINNED_CONVO : ArrayList<String> = arrayListOf()
        var SHOW_INTERNAL_CONTACTS = false
        var SHOW_EXTERNAL_CONTACTS = false


        @JvmStatic
        fun getInitials(name: String): String {
            val nameInitials = StringBuilder()
            try {
                if (name.isEmpty()) {
                    return ""
                }

                // Split the string using space and filter out empty words
                val words = name.split(" ".toRegex()).filter { it.isNotEmpty() }

                // Collect only the first character of each word
                for (i in words.indices) {
                    if (i >= 2) break // Stop if we've already added 2 initials
                    nameInitials.append(words[i][0].uppercaseChar())
                }
            } catch (e: Exception) {
                // Handle exceptions (though it's unlikely one would occur here)
                e.printStackTrace()
            }
            return nameInitials.toString()
        }
        @JvmStatic
        val randomColor: Int
            get() {
                var color = 0
                try {
                    val random = Random()
                    color =
                        Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
                } catch (e: Exception) {
                    Log.e("Catchmessage", Log.getStackTraceString(e))
                }
                return color
            }

        @JvmStatic
        val notificationIcon: Int
            get() {
                val useWhiteIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                return if (useWhiteIcon) R.drawable.ic_eservicetech else R.drawable.icon_chat
            }

        @JvmStatic
        fun formatPhoneNumber(phoneNumber: String,countryCode: String): String {
             if (phoneNumber.startsWith("+")) {
               return phoneNumber // If the string starts with '+', keep it as it is
            }else if(!countryCode.isNullOrEmpty()) {
                return countryCode + phoneNumber
            }
            else {
                 return DEFAULT_COUNTRYCODE + phoneNumber // Prepend Default countrycode if the string doesn't start with '+'
            }
        }

        @JvmStatic
        fun cleanedNumber(phoneNumber:String): String{
            // Remove spaces, brackets, and hyphens
            return  phoneNumber.replace("[\\s()\\-]".toRegex(), "")

        }
        @JvmStatic
        fun isValidPhoneNumber(phoneNumberStr: String, defaultRegion: String): Boolean {
            // Check for empty or null phone number string
            if (phoneNumberStr.isBlank()) {
                println("Phone number is blank.")
                return false
            }

            // Initialize PhoneNumberUtil instance
            val phoneNumberUtil = PhoneNumberUtil.getInstance()

            return try {
                // Parse the phone number
                val phoneNumber = phoneNumberUtil.parse(phoneNumberStr, defaultRegion)

                // Check if the phone number is valid
                phoneNumberUtil.isValidNumber(phoneNumber)
            } catch (e: NumberParseException) {
                // Handle different types of exceptions
                when (e.errorType) {
                    NumberParseException.ErrorType.NOT_A_NUMBER -> {
                        println("Phone number contains invalid characters or format.")
                    }
                    NumberParseException.ErrorType.INVALID_COUNTRY_CODE -> {
                        println("Phone number has an invalid country code.")
                    }
                    NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD -> {
                        println("Phone number is too short after IDD (International Direct Dial).")
                    }
                    NumberParseException.ErrorType.TOO_LONG -> {
                        println("Phone number is too long.")
                    }
                    else -> {
                        println("Error parsing phone number: ${e.message}")
                    }
                }
                false
            }
        }
    }
}