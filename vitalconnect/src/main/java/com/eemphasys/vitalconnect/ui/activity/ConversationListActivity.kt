package com.eemphasys.vitalconnect.ui.activity
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.eemphasys.vitalconnect.MainActivity
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.ActivityConversationListBinding
import com.eemphasys.vitalconnect.ui.fragment.ContactListFragment
import com.eemphasys.vitalconnect.ui.fragment.ConversationListFragment
import com.eemphasys.vitalconnect.ui.fragment.ProfileFragment
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit


class ConversationListActivity:AppCompatActivity() {
    val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    val contactListViewModel by lazyViewModel { injector.createContactListViewModel(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        mainViewModel.create()
        val binding = ActivityConversationListBinding.inflate(layoutInflater)
/*

        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

        val existingConversation  = retrofitWithToken.fetchExistingConversation(
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
                    val conversationList: List<ParticipantExistingConversation>? = response.body()

                    // Check if the list is not null and not empty
                    if (!conversationList.isNullOrEmpty()) {
                        // Iterate through the list and access each Conversation object
                        for (conversation in conversationList) {
                            // Access properties of each Conversation object
                            println("Conversation SID: ${conversation.conversationSid}")

                            try{
                                val participantSid = retrofitWithToken.addParticipantToConversation(Constants.TENANT_CODE,conversation.conversationSid,Constants.USERNAME)

                                participantSid.enqueue(object : Callback<String> {
                                    override fun onResponse(call: Call<String>, response: Response<String>) {
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
                            }
                            catch (e: Exception){
                                println("Exception :  ${e.message}")
                            }
                            //Starting and redirecting to Existing conversation
//                                        delay(1000)
                            MessageListActivity.startfromFragment(applicationContext,conversation.conversationSid)
                            //binding?.progressBarID?.visibility = View.GONE
                        }
                    } else { //If there is no existing conversation with SMS user, create new
                        contactListViewModel.createConversation("Himanshu" + " " + "+919175346961" ,"mahajanhimanshu3@gmail.com","+919175346961")
                        // contactListViewModel.createConversation(contact.name + " " + contact.number ,contact.email,contact.number)
                        //binding?.progressBarID?.visibility = View.GONE
                    }
                } else {
                    println("Response was not successful: ${response.code()}")
//                    setContentView(binding.root)
                }
            }

            override fun onFailure(call: Call<List<ParticipantExistingConversation>>, t: Throwable) {
                println("Failed to fetch existing conversations: ${t.message}")
            }
        })
*/

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val menubottom : BottomNavigationView = findViewById(R.id.bottom_navigation)
        val menu = menubottom.menu
        val menuItem = menu.findItem(R.id.page_contact_list)
        menubottom.itemIconTintList = null
        val isDataAvailable = Constants.CONTACTS.isNullOrEmpty() || Constants.WEBUSERS.isNullOrEmpty()
        menuItem.isVisible = !isDataAvailable

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.page_contact_list -> replaceFragment(ContactListFragment())
                R.id.page_conversation_list -> replaceFragment(ConversationListFragment())
                R.id.page_profile -> replaceFragment(ProfileFragment())

            }
            return@setOnItemSelectedListener true
        }

        if (savedInstanceState == null) {
            if (!isDataAvailable) {
                replaceFragment(ContactListFragment())
            }
            else {
                replaceFragment(ConversationListFragment())
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    override fun onBackPressed() {
        super.onBackPressed()
    }
    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { currentFragment ->
            if (currentFragment::class == fragment::class) {
                return
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }
    companion object {

        fun start(context: Context) {
            val intent = getStartIntent(context)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun getStartIntent(context: Context) =
            Intent(context, ConversationListActivity::class.java)
    }
}