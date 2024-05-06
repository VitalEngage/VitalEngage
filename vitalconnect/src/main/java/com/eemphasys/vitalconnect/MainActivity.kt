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
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.data.models.Contact
import com.eemphasys.vitalconnect.data.models.WebUser
import com.eemphasys.vitalconnect.ui.activity.ConversationListActivity
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class MainActivity() : AppCompatActivity() {

    val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    val contactListViewModel by lazyViewModel { injector.createContactListViewModel(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
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

        mainViewModel.create()
        super.onCreate(savedInstanceState)
//        mainViewModel.registerForFcm()
        if(Constants.SHOW_CONTACTS == "false") {
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
                "+919175346961",
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

                                        override fun onFailure(call: Call<String>, t: Throwable) {
                                            println("Failed to execute request: ${t.message}")
                                        }
                                    })
                                } catch (e: Exception) {
                                    println("Exception :  ${e.message}")
                                }
                                //Starting and redirecting to Existing conversation
//                                        delay(1000)
                                contactListViewModel.getSyncStatus(conversation.conversationSid)
                                //MessageListActivity.startfromFragment(applicationContext,conversation.conversationSid)
                                //binding?.progressBarID?.visibility = View.GONE
                            }
                        } else { //If there is no existing conversation with SMS user, create new
                            contactListViewModel.createConversation(
                                "Himanshu" + " " + "+919175346961",
                                "mahajanhimanshu3@gmail.com",
                                "+919175346961"
                            )
                            //contactListViewModel.createConversation(contact.name + " " + contact.number ,contact.email,contact.number)
                            //binding?.progressBarID?.visibility = View.GONE
                        }
                    } else {
                        println("Response was not successful: ${response.code()}")
//                    setContentView(binding.root)
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
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }


        }
    }
