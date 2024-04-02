package com.eemphasys.vitalconnect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.data.models.Contact
import com.eemphasys.vitalconnect.data.models.WebUser
import com.eemphasys.vitalconnect.ui.activity.ConversationListActivity
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class MainActivity() : AppCompatActivity() {

    val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    override fun onCreate(savedInstanceState: Bundle?) {

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

        mainViewModel.create()
//        mainViewModel.registerForFcm()

            super.onCreate(savedInstanceState)
        val intent = Intent(this, ConversationListActivity::class.java)
        startActivity(intent)

        }
    }
