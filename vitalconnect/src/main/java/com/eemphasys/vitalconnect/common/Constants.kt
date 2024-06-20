package com.eemphasys.vitalconnect.common

import android.graphics.Color
import android.os.Build
import android.util.Log
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.data.models.ParticipantListViewItem
import java.util.Random

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


        @JvmStatic
        fun getInitials(name: String): String {
            val nameInitials = StringBuilder()
            try {
                if (name.isEmpty()) {
                    return ""
                }

                //split the string using 'space'
                //and print the first character of every word
                val words = name.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (word in words) {
                    nameInitials.append(word[0].uppercaseChar())
                    //                System.out.print(Character.toUpperCase(word.charAt(0)));
                }
            } catch (e: Exception) {
                nameInitials.append("")
            }
            return nameInitials.toString().trim { it <= ' ' }
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
               return "+1$phoneNumber" // Prepend '+1' if the string doesn't start with '+'
            }
        }

        @JvmStatic
        fun cleanedNumber(phoneNumber:String): String{
            // Remove spaces, brackets, and hyphens
            return  phoneNumber.replace("[\\s()\\-]".toRegex(), "")

        }
    }
}