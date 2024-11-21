package com.eemphasys.vitalconnect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.Constants.Companion.saveStringToVitalTextSharedPreferences
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.ActivityMainBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.ui.activity.ConversationListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.gson.Gson
import com.twilio.conversations.Attributes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    val contactListViewModel by lazyViewModel { injector.createContactListViewModel(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        val username = intent.getStringExtra("username")
        val friendlyName = intent.getStringExtra("friendlyName")
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
        val showDepartment = intent.getStringExtra("showDepartment")
        val showDesignation = intent.getStringExtra("showDesignation")
        val department = intent.getStringExtra("department")
        val designation = intent.getStringExtra("designation")
        val customer = intent.getStringExtra("customer")
        val countryCode = intent.getStringExtra("countryCode")
        val email = intent.getStringExtra("email")
        val mobileNumber = intent.getStringExtra("mobileNumber")
        val defaultcountryCode = intent.getStringExtra("defaultcountryCode")
        val timeoffset = intent.getStringExtra("timeoffset")
        val withContext = intent.getStringExtra("withContext")
        val openChat = intent.getStringExtra("openChat")
        val context = intent.getStringExtra("context")
        val dealerName = intent.getStringExtra("dealerName")
        val pinnedConvo = intent.getStringArrayListExtra("pinnedConvo")
        val showInternalContacts = intent.getStringExtra("showInternalContacts")
        val showExternalContacts = intent.getStringExtra("showExternalContacts")
        val role = intent.getStringExtra("role")
        val bpId = intent.getStringExtra("bpId")
        val isAutoRegistrationEnabled = intent.getStringExtra("isAutoRegistrationEnabled")
        val refreshToken = intent.getStringExtra("refreshToken")
        val expirationDuration = intent.getIntExtra("expirationDuration",0)

        Constants.PINNED_CONVO = pinnedConvo!!
        Log.d("pinned",pinnedConvo.toString())

        saveStringToVitalTextSharedPreferences(this,"pinnedConvo",Gson().toJson(pinnedConvo!!))
        saveStringToVitalTextSharedPreferences(this,"baseUrl",baseurl!!)
//        saveStringToVitalTextSharedPreferences(this,"packageName",packageName.toString())
        saveStringToVitalTextSharedPreferences(this,"currentUser",username!!)
        saveStringToVitalTextSharedPreferences(this,"friendlyName",fullName!!)
        saveStringToVitalTextSharedPreferences(this,"authToken",authToken!!)
        saveStringToVitalTextSharedPreferences(this,"proxyNumber",proxyNumber!!)
        saveStringToVitalTextSharedPreferences(this,"showDepartment",showDepartment!!)
        saveStringToVitalTextSharedPreferences(this,"showDesignation",showDesignation!!)
        saveStringToVitalTextSharedPreferences(this,"email",email!!)
        saveStringToVitalTextSharedPreferences(this,"mobileNumber",mobileNumber!!)
        saveStringToVitalTextSharedPreferences(this,"defaultCountryCode",defaultcountryCode!!)
//        saveStringToVitalTextSharedPreferences(this,"userSMSAlert",userSMSAlert!!)
        saveStringToVitalTextSharedPreferences(this,"twilioToken",twilioToken!!)
//        saveStringToVitalTextSharedPreferences(this,"customerContactList",list)
        saveStringToVitalTextSharedPreferences(this,"contacts",contacts!!)
        saveStringToVitalTextSharedPreferences(this,"webUsers",webusers!!)
//        saveStringToVitalTextSharedPreferences(this,"phoneNumberList",listOfPhoneNumbers)
//        saveStringToVitalTextSharedPreferences(this,"unreadCountResponse",unreadCountResponse)
        saveStringToVitalTextSharedPreferences(this,"department",department!!)
        saveStringToVitalTextSharedPreferences(this,"designation",designation!!)
        saveStringToVitalTextSharedPreferences(this,"countryCode",countryCode!!)
        saveStringToVitalTextSharedPreferences(this,"offset",timeoffset!!)
        saveStringToVitalTextSharedPreferences(this,"customerName",customerName!!)
        saveStringToVitalTextSharedPreferences(this,"customerNumber",Constants.formatPhoneNumber(applicationContext,customerNumber!!,countryCode!!))
        saveStringToVitalTextSharedPreferences(this,"isStandalone",isStandalone!!)
        saveStringToVitalTextSharedPreferences(this,"customer",customer!!)
        saveStringToVitalTextSharedPreferences(this, "showConversations", showConversations!!)
        saveStringToVitalTextSharedPreferences(this, "showContacts", showContacts!!)
        saveStringToVitalTextSharedPreferences(this,"product",parentApp!!)
        saveStringToVitalTextSharedPreferences(this,"tenantCode", tenantcode!!)
        saveStringToVitalTextSharedPreferences(this,"withContext", withContext!!)
        saveStringToVitalTextSharedPreferences(this,"openChat", openChat!!)
        saveStringToVitalTextSharedPreferences(this,"context", context!!)
        saveStringToVitalTextSharedPreferences(this,"dealerName",dealerName!!)
        saveStringToVitalTextSharedPreferences(this,"showInternalContacts",showInternalContacts!!)
        saveStringToVitalTextSharedPreferences(this,"showExternalContacts",showExternalContacts!!)
        saveStringToVitalTextSharedPreferences(this,"role",role!!)
        saveStringToVitalTextSharedPreferences(this,"bpId",bpId!!)
        saveStringToVitalTextSharedPreferences(this,"isAutoRegistrationEnabled",isAutoRegistrationEnabled!!)
        saveStringToVitalTextSharedPreferences(this,"refreshToken",refreshToken!!)
        saveStringToVitalTextSharedPreferences(this,"expirationDuration",expirationDuration!!.toString())
//        Log.d("timezoneoffset", timeoffset!!)

//        mainViewModel.create()
        super.onCreate(savedInstanceState)
        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"openChat")!!.lowercase() == "true") {
            val existingConversation = RetrofitClient.getRetrofitWithToken().fetchExistingConversation(
                Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                Constants.cleanedNumber(Constants.formatPhoneNumber(applicationContext,customerNumber!!,countryCode!!)),
                false,
                1,
                Constants.getStringFromVitalTextSharedPreferences(this,"proxyNumber")!!
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
                                            RetrofitClient.getRetrofitWithToken().addParticipantToConversation(
                                                Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                                                conversation.conversationSid,
                                                Constants.getStringFromVitalTextSharedPreferences(this@MainActivity,"currentUser")!!
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
                                    //set attrtibute fetched from parent if blank in response
                                    if(conversation.attributes.Department.isNullOrEmpty() &&
                                        conversation.attributes.Designation.isNullOrEmpty() &&
                                        conversation.attributes.CustomerName.isNullOrEmpty()){
                                        //add attributes from parent
                                        var customer = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"customer")
                                        var department = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"department")
                                        var designation = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"designation")
                                        var role = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"role")
                                        var bpId = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"bpId")

                                        val attributes = mapOf(
                                            "Designation" to designation,
                                            "Department" to department,
                                            "CustomerName" to customer,
                                            "Role" to role,
                                            "BpId" to bpId,
                                            "isWebChat" to "false"
                                        )

                                        val jsonObject = JSONObject(attributes)
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
                                    var role = ""
                                    var bpId = ""

                                    if (conversation.attributes.Department.isNullOrEmpty() &&
                                        conversation.attributes.Designation.isNullOrEmpty() &&
                                        conversation.attributes.CustomerName.isNullOrEmpty()
                                    ) {
                                         customer = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"customer")!!
                                         department = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"department")!!
                                         designation = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"designation")!!
                                        role = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"role")!!
                                        bpId = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"bpId")!!
                                    }
                                    else{
                                        customer = conversation.attributes.CustomerName
                                        department = conversation.attributes.Department
                                        designation = conversation.attributes.Designation
                                    }
                                    val attributes = mapOf(
                                        "Designation" to designation,
                                        "Department" to department,
                                        "CustomerName" to customer,
                                        "Role" to role,
                                        "BpId" to bpId,
                                        "isWebChat" to "false"
                                    )
                                    val jsonObject = JSONObject(attributes)
                                    contactListViewModel.createConversation(
                                        "$customerName ${
                                            Constants.formatPhoneNumber(applicationContext,
                                                customerNumber!!,
                                                countryCode
                                            )
                                        }",
                                        " ",
                                        "${
                                            Constants.cleanedNumber(
                                                Constants.formatPhoneNumber(applicationContext,
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
                                var customer = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"customer")
                                var department = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"department")
                                var designation = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"designation")
                                var role = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"role")
                                var bpId = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"bpId")

                                val attributes = mapOf(
                                    "Designation" to designation,
                                    "Department" to department,
                                    "CustomerName" to customer,
                                    "Role" to role,
                                    "BpId" to bpId,
                                    "isWebChat" to "false"
                                )

                                val jsonObject = JSONObject(attributes)
                                contactListViewModel.createConversation(
                                    "$customerName ${Constants.formatPhoneNumber(applicationContext,customerNumber!!,countryCode)}",
                                    " ",
                                    "${Constants.cleanedNumber(Constants.formatPhoneNumber(applicationContext,customerNumber!!,countryCode))}",
                                    Attributes(jsonObject)
                                )

                            } catch(e:Exception){
                                println("Exception :  ${e.message}")
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
                    } else {
                        this@MainActivity.finish()
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
        else
        {
            try {
                val intent = Intent(this, ConversationListActivity::class.java)
                startActivity(intent)
            }
            catch(e:Exception)
            {
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
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        }
    }
