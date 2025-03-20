package com.eemphasys.vitalconnect.common

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.data.models.ParticipantListViewItem
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Random
import java.util.TimeZone

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
            isGlobal = false,
            "",
            "",
            isGroup = null,
            objectId = null
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
        var SHOW_INTERNAL_CONTACTS = ""
        var SHOW_EXTERNAL_CONTACTS = ""
        var PAGE_SIZE = 10000.0 //keep this floating
        var ROLE = ""
        var BPID = ""
        var ALLUSERS : MutableList<ContactListViewItem> = arrayListOf()
        var ALLCONTACTS : MutableList<ContactListViewItem> = arrayListOf()


        const val MyPREFERENCES = "MyVitaltextPrefs"
        var sharedpreferences: SharedPreferences? = null
        @JvmStatic
        fun getStringFromVitalTextSharedPreferences(context: Context?, key: String): String? {
            context?.let {
                sharedpreferences = context.getSharedPreferences(MyPREFERENCES,Context.MODE_PRIVATE)
                val value = sharedpreferences?.getString(key, null) // Use null as default value to check if key exists
//                Log.d("SharedPreferencesUtilVT", "Retrieved value: $value for key: $key")
                return value
            } ?: Log.e("SharedPreferencesUtil", "Context is null")
            return null
        }

        @JvmStatic
        fun saveStringToVitalTextSharedPreferences(context: Context?, key: String, value: String) {
            context?.let {
                sharedpreferences = context.getSharedPreferences(MyPREFERENCES,Context.MODE_PRIVATE)
                val editor = sharedpreferences?.edit()
                editor?.putString(key, value)
                editor?.apply()
//                Log.d("SharedPreferencesUtilVT", "Saved value: $value with key: $key")
            } ?: Log.e("SharedPreferencesUtil", "Context is null")
        }

        @JvmStatic
        fun clearVitalTextSharedPreferences(context: Context?) {
            context?.let {
                val sharedPreferences = it.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // Clear all data
                editor.apply()
                Log.d("SharedPreferencesUtil", "All shared preferences cleared for vitaltext.")
            } ?: Log.e("SharedPreferencesUtil", "Context is null")
        }

        @JvmStatic
        fun addToPinnedConvo(item: String): Boolean {
            return if (PINNED_CONVO.size < 5) {
                PINNED_CONVO.add(item)
                true // Successfully added
            } else {
                println("Cannot add more items. Maximum limit of 5 reached.") // Notification
                false // Addition failed
            }
        }

        @JvmStatic
        fun getTimeStamp(): String{
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(calendar.time)
        }
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
         fun getSearchViewEditText(searchView: SearchView): EditText? {
            try {
                val searchEditTextField: Field = searchView.javaClass.getDeclaredField("mSearchSrcTextView")
                searchEditTextField.isAccessible = true
                return searchEditTextField.get(searchView) as? EditText
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
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
        fun formatPhoneNumber(applicationContext: Context,phoneNumber: String,countryCode: String): String {
             if (phoneNumber.startsWith("+")) {
               return phoneNumber // If the string starts with '+', keep it as it is
            }else if(!countryCode.isNullOrEmpty()) {
                return countryCode + phoneNumber
            }
            else {
                 return getStringFromVitalTextSharedPreferences(applicationContext,"defaultCountryCode") + phoneNumber // Prepend Default countrycode if the string doesn't start with '+'
            }
        }

        @JvmStatic
         fun showPopup(layoutInflater: LayoutInflater, activity : Activity) {
            // Inflate the popup layout
            val inflater = layoutInflater
            val popupView = inflater.inflate(R.layout.error_popup_layout,null)
            val errorText = popupView.findViewById<TextView>(R.id.error_text)

            errorText.text = "You appear to be offline. While offline you cannot access chat."


            // Create the PopupWindow
            val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // Create a dimmed background view
            val dimBackground = View(AppContextHelper.appContext).apply {
                setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                visibility = View.VISIBLE
            }

            // Get the root view
            val rootView = activity.window.decorView.findViewById<View>(android.R.id.content) as ViewGroup
            rootView.addView(dimBackground)
            // Close the popup when the button is clicked
            val closeButton: TextView = popupView.findViewById(R.id.ok_button)
            closeButton.setOnClickListener {
                popupWindow.dismiss()
                rootView.removeView(dimBackground)
                activity.finish()
            }

            // Show the popup
            popupWindow.isFocusable = true
            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

            // Set OnDismissListener to remove the dim background
            popupWindow.setOnDismissListener {
                rootView.removeView(dimBackground)
            }
        }

        @JvmStatic
        fun cleanedNumber(phoneNumber:String): String{
            // Remove spaces, brackets, and hyphens
            return  phoneNumber.replace("[\\s()\\-]".toRegex(), "")

        }
        @JvmStatic
        fun isExternalContact(name : String):Boolean{
            val phoneNumberRegex = Regex(
                """^\+[0-9]{5,16}$""")

            return phoneNumberRegex.matches(name)
        }

        @JvmStatic
        fun isValidEmail(email: String): Boolean {
            val emailRegex = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
            return email.matches(emailRegex.toRegex())
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

        @JvmStatic
         fun changeButtonBackgroundColor(textView: TextView?, colorid: Int,coloridText:Int) {
            try {
                val background = textView!!.background
                if (background is ShapeDrawable) {
                    background.paint.color = colorid
                    textView.setTextColor(coloridText)
                } else if (background is GradientDrawable) {
                    background.setColor(colorid)
                    textView.setTextColor(coloridText)
                } else if (background is ColorDrawable) {
                    background.color = colorid
                    textView.setTextColor(coloridText)
                }
            } catch (e: Exception) {
                Log.e("Catchmessage", Log.getStackTraceString(e))
                EETLog.error(
                    AppContextHelper.appContext, LogConstants.logDetails(
                        e,
                        LogConstants.LOG_LEVEL.ERROR.toString(),
                        LogConstants.LOG_SEVERITY.HIGH.toString()
                    ),
                    Constants.EX, LogTraceConstants.getUtilityData(
                        AppContextHelper.appContext!!
                    )!!
                )
            }
        }
    }
}