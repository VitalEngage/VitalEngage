package com.eemphasys.vitalconnect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.databinding.ActivityMainBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.ui.activity.ConversationListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.gson.Gson
import com.twilio.conversations.Attributes
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    val contactListViewModel by lazyViewModel { injector.createContactListViewModel(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        val username = intent.getStringExtra("username")
        val friendlyName = intent.getStringExtra("friendlyName")
        val clientID = intent.getStringExtra("clientID")
        val clientSecret = intent.getStringExtra("clientSecret")
        val tenantcode = intent.getStringExtra("tenantcode")
        val baseurl = intent.getStringExtra("baseurl")
        val parentApp = intent.getStringExtra("parentApp")
        val contacts = intent.getStringExtra("contacts")
        val twilioToken = intent.getStringExtra("twilioToken")
        val webusers = intent.getStringExtra("webusers")
        val authToken = intent.getStringExtra("authToken")
        val proxyNumber = intent.getStringExtra("proxyNumber")
        val fullName = intent.getStringExtra("fullName")
        val showContacts = intent.getStringExtra("showContacts")
        val isStandalone = intent.getStringExtra("isStandalone")
        val customerNumber = intent.getStringExtra("customerNumber")
        val customerName = intent.getStringExtra("customerName")
        val showConversations = intent.getStringExtra("showConversations")
        val userSMSAlert = intent.getStringExtra("userSMSAlert")
        val showDepartment = intent.getStringExtra("showDepartment")
        val showDesignation = intent.getStringExtra("showDesignation")
        val department = intent.getStringExtra("department")
        val designation = intent.getStringExtra("designation")
        val customer = intent.getStringExtra("customer")
        val countryCode = intent.getStringExtra("countryCode")
        val email = intent.getStringExtra("email")
        val mobileNumber = intent.getStringExtra("mobileNumber")


        Constants.AUTH_TOKEN = authToken!!
        Constants.CONTACTS = contacts!!
        Constants.WEBUSERS = webusers!!
        Constants.BASE_URL = baseurl!!
        Constants.TENANT_CODE = tenantcode!!
        Constants.CLIENT_ID = clientID!!
        Constants.CLIENT_SECRET = clientSecret!!
        Constants.FRIENDLY_NAME = friendlyName!!
        Constants.PRODUCT = parentApp!!
        Constants.USERNAME = username!!
        Constants.TWILIO_TOKEN = twilioToken!!
        Constants.PROXY_NUMBER = proxyNumber!!
        Constants.FULL_NAME = fullName!!
        Constants.SHOW_CONTACTS = showContacts!!
        Constants.IS_STANDALONE = isStandalone!!
        Constants.CUSTOMER_NUMBER = Constants.formatPhoneNumber(customerNumber!!,countryCode!!)
        Constants.CUSTOMER_NAME = customerName!!
        Constants.SHOW_CONVERSATIONS = showConversations!!
        Constants.USER_SMS_ALERT = userSMSAlert!!
        Constants.SHOW_DEPARTMENT = showDepartment!!
        Constants.SHOW_DESIGNATION =showDesignation!!
        Constants.DEPARTMENT= department!!
        Constants.DESIGNATION=designation!!
        Constants.CUSTOMER=customer!!
        Constants.COUNTRYCODE=countryCode
        Constants.EMAIL = email!!
        Constants.MOBILENUMBER = mobileNumber!!

        mainViewModel.create()
        super.onCreate(savedInstanceState)
//        mainViewModel.registerForFcm()
        if(Constants.SHOW_CONTACTS == "false" && Constants.IS_STANDALONE == "false" && Constants.SHOW_CONVERSATIONS == "false") {
            val httpClientWithToken = OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
                .addInterceptor(RetryInterceptor())
                .build()
            val retrofitWithToken =
                RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

            val existingConversation = retrofitWithToken.fetchExistingConversation(
                Constants.TENANT_CODE,
                Constants.cleanedNumber(Constants.formatPhoneNumber(customerNumber!!,countryCode)),
                false,
                1,
                Constants.PROXY_NUMBER
            )
            existingConversation.enqueue(object : Callback<List<ParticipantExistingConversation>> {
                @SuppressLint("SuspiciousIndentation")
                override fun onResponse(
                    call: Call<List<ParticipantExistingConversation>>,
                    response: Response<List<ParticipantExistingConversation>>
                ) {
                    if (response.isSuccessful) {
                        val conversationList: List<ParticipantExistingConversation>? =
                            response.body()

                        // Check if the list is not null and not empty
                        if (!conversationList.isNullOrEmpty()) {
                            // Iterate through the list and access each Conversation object
                            for (conversation in conversationList) {
                                // Access properties of each Conversation object
                                println("Conversation SID: ${conversation.conversationSid}")
                                //when conversation exists
                                if(!conversation.conversationSid.isNullOrEmpty()) {
                                    try {
                                        val participantSid =
                                            retrofitWithToken.addParticipantToConversation(
                                                Constants.TENANT_CODE,
                                                conversation.conversationSid,
                                                Constants.USERNAME
                                            )

                                        participantSid.enqueue(object : Callback<String> {
                                            override fun onResponse(
                                                call: Call<String>,
                                                response: Response<String>
                                            ) {
                                                if (response.isSuccessful) {
                                                    val responseBody: String? = response.body()
                                                    // Handle the string value as needed
                                                    println("Response body: $responseBody")
                                                } else {
                                                    println("Response was not successful: ${response.code()}")
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<String>,
                                                t: Throwable
                                            ) {
                                                println("Failed to execute request: ${t.message}")
                                            }
                                        })
                                    } catch (e: Exception) {
                                        println("Exception :  ${e.message}")
                                        EETLog.error(
                                            SessionHelper.appContext, LogConstants.logDetails(
                                                e,
                                                LogConstants.LOG_LEVEL.ERROR.toString(),
                                                LogConstants.LOG_SEVERITY.HIGH.toString()
                                            ),
                                            Constants.EX, LogTraceConstants.getUtilityData(
                                                SessionHelper.appContext!!
                                            )!!
                                        )
                                    }
                                    //set attrtibute fetched from parent if blank in response
                                    if(conversation.attributes.Department.isNullOrEmpty() &&
                                        conversation.attributes.Designation.isNullOrEmpty() &&
                                        conversation.attributes.CustomerName.isNullOrEmpty()){
                                        //add attributes from parent
                                        var customer = Constants.CUSTOMER
                                        var department = Constants.DEPARTMENT
                                        var designation = Constants.DESIGNATION

                                        val attributes = mapOf(
                                            "Designation" to designation,
                                            "Department" to department,
                                            "CustomerName" to customer
                                        )

                                        val jsonObject = JSONObject(attributes)
                                        Log.d("setting attributes", jsonObject.toString())
                                        contactListViewModel.setAttributes(conversation.conversationSid,Attributes(jsonObject))
                                    }
                                    //Starting and redirecting to Existing conversation
                                    contactListViewModel.getSyncStatus(conversation.conversationSid)
                                }
                                //conversation doesnt exist
                                else {
                                    var customer = ""
                                    var department =""
                                    var designation = ""

                                    if (conversation.attributes.Department.isNullOrEmpty() &&
                                        conversation.attributes.Designation.isNullOrEmpty() &&
                                        conversation.attributes.CustomerName.isNullOrEmpty()
                                    ) {
                                         customer = Constants.CUSTOMER
                                         department = Constants.DEPARTMENT
                                         designation = Constants.DESIGNATION
                                    }
                                    else{
                                        customer = conversation.attributes.CustomerName
                                        department = conversation.attributes.Department
                                        designation = conversation.attributes.Designation
                                    }
                                    val attributes = mapOf(
                                        "Designation" to designation,
                                        "Department" to department,
                                        "CustomerName" to customer
                                    )
                                    val jsonObject = JSONObject(attributes)
                                    Log.d("setting attributes", jsonObject.toString())
                                    contactListViewModel.createConversation(
                                        "$customerName ${
                                            Constants.formatPhoneNumber(
                                                customerNumber!!,
                                                countryCode
                                            )
                                        }",
                                        " ",
                                        "${
                                            Constants.cleanedNumber(
                                                Constants.formatPhoneNumber(
                                                    customerNumber!!,
                                                    countryCode
                                                )
                                            )
                                        }",
                                        Attributes(jsonObject)
                                    )

                                }
                            }
                        } else {
                            try {

                                //If there is no existing conversation with SMS user, create new
                                //set attributes fetched from parent
                                var customer = Constants.CUSTOMER
                                var department = Constants.DEPARTMENT
                                var designation = Constants.DESIGNATION

                                val attributes = mapOf(
                                    "Designation" to designation,
                                    "Department" to department,
                                    "CustomerName" to customer
                                )

                                val jsonObject = JSONObject(attributes)
                                Log.d("setting attributes", jsonObject.toString())
                                contactListViewModel.createConversation(
                                    "$customerName ${Constants.formatPhoneNumber(customerNumber!!,countryCode)}",
                                    " ",
                                    "${Constants.cleanedNumber(Constants.formatPhoneNumber(customerNumber!!,countryCode))}",
                                    Attributes(jsonObject)
                                )

                            } catch(e:Exception){
                                println("Exception :  ${e.message}")
                                EETLog.error(
                                    SessionHelper.appContext, LogConstants.logDetails(
                                        e,
                                        LogConstants.LOG_LEVEL.ERROR.toString(),
                                        LogConstants.LOG_SEVERITY.HIGH.toString()
                                    ),
                                    Constants.EX, LogTraceConstants.getUtilityData(
                                        SessionHelper.appContext!!
                                    )!!
                                )
                            }
                        }
                    } else {
                        println("Response was not successful: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<ParticipantExistingConversation>>,
                    t: Throwable
                ) {
                    println("Failed to fetch existing conversations: ${t.message}")
                }
            })

        }
        else{
                    val intent = Intent(this, ConversationListActivity::class.java)
                    startActivity(intent)
        }
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        }
    }
