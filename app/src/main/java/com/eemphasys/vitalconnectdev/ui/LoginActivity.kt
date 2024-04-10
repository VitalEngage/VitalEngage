package com.eemphasys.vitalconnectdev.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.MainActivity
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.extensions.hideKeyboard
import com.eemphasys.vitalconnect.data.localCache.LocalCacheProvider
import com.eemphasys.vitalconnectdev.ChatApplication
import com.eemphasys.vitalconnectdev.R
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError.*
import com.eemphasys.vitalconnectdev.common.extensions.lazyViewModel
import com.eemphasys.vitalconnectdev.common.extensions.onSubmit
import com.eemphasys.vitalconnectdev.common.injector
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.data.model.Contact
import com.eemphasys.vitalconnectdev.data.model.WebUser
import com.eemphasys.vitalconnectdev.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import  com.eemphasys_enterprise.commonmobilelib.EETLog

class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    val loginViewModel by lazyViewModel { injector.createLoginViewModel(application) }

    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.loginCoordinatorLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
        setContentView(binding.root)



//        val items = listOf("e-servicetech", "e-logistics", "e-serviceplus")
//        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
//
//        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.dropdown_product)
//
//        autoCompleteTextView.setAdapter(adapter)

        loginViewModel.isLoading.observe(this) { isLoading ->
            showProgress(isLoading)
        }

        loginViewModel.isNetworkAvailable.observe(this) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
        }

        loginViewModel.onSignInError.observe(this) { signInError ->
            signInFailed(signInError)
        }

        loginViewModel.onSignInSuccess.observe(this) {
            signInSucceeded()
        }

        binding.usernameTv.onSubmit {
            hideKeyboard()
            signInPressed() }
        binding.signInBtn.setOnClickListener {
            hideKeyboard()
            signInPressed() }
        binding.settingsBtn.setOnClickListener { openSettings() }

        initializeChatAppModel()

    }

//    override fun onDestroy() {
//        super.onDestroy()
//    }

    private fun initializeChatAppModel(){
        ChatAppModel.init(
            LoginConstants.BASE_URL,
            LoginConstants.TWILIO_TOKEN

        )
    }

    private fun signInPressed() {
        val identity = binding.usernameTv.text.toString()
        LoginConstants.CURRENT_USER = identity
        LoginConstants.FRIENDLY_NAME = identity
//        val password = binding.passwordTv.text.toString()

        loginViewModel.signIn(identity)
    }

    private fun showProgress(show: Boolean) {
        binding.loginProgress.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }

    private fun goToChildAppScreen() {
        start(this)
    }

    private fun openSettings(){
        val intent = Intent(this, Settings::class.java)
        startActivity(intent)
    }

    private fun signInSucceeded() = goToChildAppScreen()

    private fun signInFailed(error: ConversationsError) {
        when (error) {
            EMPTY_USERNAME -> binding.usernameInputLayout.error = getString(R.string.enter_username)

//            EMPTY_PASSWORD -> binding.passwordInputLayout.error = getString(R.string.enter_password)

            EMPTY_USERNAME_AND_PASSWORD -> {
                binding.usernameInputLayout.error = getString(R.string.enter_username)
//                binding.passwordInputLayout.error = getString(R.string.enter_password)
            }

            //TOKEN_ACCESS_DENIED -> binding.passwordInputLayout.error = getString(R.string.token_access_denied)

//            NO_INTERNET_CONNECTION -> showNoInternetDialog()

            else -> binding.usernameInputLayout.error = getString( R.string.sign_in_error)
        }
    }

    companion object{


        fun start(context: Context){
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("username", LoginConstants.CURRENT_USER)
            intent.putExtra("clientID", LoginConstants.CLIENT_ID)
            intent.putExtra("clientSecret", LoginConstants.CLIENT_SECRET)
            intent.putExtra("baseurl",LoginConstants.BASE_URL)
            intent.putExtra("tenantcode",LoginConstants.TENANT_CODE)
            intent.putExtra("parentApp",LoginConstants.PRODUCT)
            intent.putExtra("proxyNumber",LoginConstants.PROXY_NUMBER)
            intent.putExtra("friendlyName",LoginConstants.FRIENDLY_NAME)
            intent.putExtra("twilioToken",LoginConstants.TWILIO_TOKEN)
            intent.putExtra("contacts", LoginConstants.CONTACTS)
            intent.putExtra("webusers", LoginConstants.WEBUSERS)
            intent.putExtra("authToken",LoginConstants.AUTH_TOKEN)
            context.startActivity(intent)


        }
    }

}