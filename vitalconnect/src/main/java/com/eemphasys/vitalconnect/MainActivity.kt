package com.eemphasys.vitalconnect

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainActivity() : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    override fun onCreate(savedInstanceState: Bundle?) {



        val username = intent.getStringExtra("username")
        val clientID = intent.getStringExtra("clientID")
        val clientSecret = intent.getStringExtra("clientSecret")
        val tenantcode = intent.getStringExtra("tenantcode")
        val baseurl = intent.getStringExtra("baseurl")

        Constants.BASE_URL = baseurl!!
        Constants.TENANT_CODE = tenantcode!!


        binding.text.text = username
        binding.text1.text = clientID
        binding.text2.text = clientSecret
        binding.text3.text= tenantcode
        binding.text4.text= baseurl

        val tenant = "VitalConnectDev"
        val client= "VitalConnect-sQz2LmzcRLU07nnod9MlJJcJdcQj6iFZTg6uSnPbjxZ9Vssm9qONFhbPh64hmUYLxbKWpURQ0JesgZTvhNsFj3ca67lDuIWwyir4rS6GWFK5dxaHQ/4kqh8aCmB6JJP0"
        val secret= "+QXNy5ItEjPCSDG6sF7R23oy7M9sDjfFJuNcizgyRXYKjcTc98EFye7g4G5CTiee7QCLaEfQhd2i1mihW9tOTaFxsO077LlciZyNCWpoUYjH5LLoPiqYIw7Ux/JYF3gP"
        val user = "abelorkar"
        val product = "eLog"

        val requestData = RequestToken( tenant,client,secret,user,product)

        val tokenApi = RetrofitHelper.getInstance().create(TwilioApi::class.java)
        // launching a new coroutine
        GlobalScope.launch {
            val result = tokenApi.getAuthToken(requestData)
            Log.d("result", result.toString())

            if (result != null)
                Log.d("Himanshu: ", result.body()!!.token)

            val httpClientWithToken = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(result.body()!!.token))
                .build()
            val retrofitWithToken = RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

            val TwilioToken = retrofitWithToken.getTwilioToken(tenantcode!!, username!!,username)
            Log.d("twiliotoken",TwilioToken.body()!!.token)
            Constants.TWILIO_TOKEN = TwilioToken.body()!!.token

            //ConversationsClientWrapper.createInstance(this)
            mainViewModel.create()



        }

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

    }
}
