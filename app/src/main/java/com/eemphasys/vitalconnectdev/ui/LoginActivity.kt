package com.eemphasys.vitalconnectdev.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.eemphasys.vitalconnect.MainActivity
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.extensions.hideKeyboard
import com.eemphasys.vitalconnect.data.localCache.LocalCacheProvider
import com.eemphasys.vitalconnect.ui.activity.ConversationListActivity
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys.vitalconnectdev.ChatApplication
import android.Manifest
import android.os.Handler
import androidx.core.app.ActivityCompat
import com.eemphasys.vitalconnect.common.extensions.enableErrorResettingOnTextChanged
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

    override fun onStart() {
        super.onStart()
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")

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
        binding.passwordTv.onSubmit {
            hideKeyboard()
            signInPressed() }
        binding.signInBtn.setOnClickListener {
            hideKeyboard()
            signInPressed() }
        binding.settingsBtn.setOnClickListener { openSettings() }

        loginViewModel.isAADEnabled.observe(this){
            if(it) {
                binding.usernameInputLayout.visibility = GONE
            }
        }
        initializeChatAppModel()
        //loginViewModel.isAzureADEnabled()

    }

//    override fun onDestroy() {
//        super.onDestroy()
//    }

    val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if
                (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            for (permission in grantResults) {
                var permission = permission;
                var isPermitted = permission == PackageManager.PERMISSION_GRANTED;
                if
                        (permission == PackageManager.PERMISSION_DENIED
                ) {
// user rejected the permission
                    checkAndRequestPermissions()
                } else {
// btnLogin.performClick()
                }

            }
                }
        }
    private fun checkAndRequestPermissions(): Boolean {
//        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//        val storage =
//            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//        val networkTypePermission =
//            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

//        val loc =
//            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//        val loc2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val listPermissionsNeeded: MutableList<String> = ArrayList()
//        if (camera != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.CAMERA)
//        }
//        if (storage != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        }
//        if (mic != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
//        }
//
//        if (networkTypePermission != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
//        }

        if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        } else {
            Handler().postDelayed(Runnable {
//                btnLogin.performClick()
            }, 300)

        }
        return true
    }

    private fun initializeChatAppModel(){
        ChatAppModel.init(
            LoginConstants.BASE_URL,
            LoginConstants.TWILIO_TOKEN,
            packageName.toString()

        )
    }

    private fun signInPressed() {
        val identity = binding.usernameTv.text.toString()
        LoginConstants.CURRENT_USER = identity
        LoginConstants.FRIENDLY_NAME = identity
        val password = binding.passwordTv.text.toString()

        loginViewModel.signIn(identity,password)
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
            EMPTY_USERNAME -> {binding.usernameInputLayout.error = getString(R.string.enter_username)
                binding.usernameInputLayout.enableErrorResettingOnTextChanged()}

            EMPTY_PASSWORD -> {
                binding.passwordInputLayout.error = getString(R.string.enter_password)
                binding.passwordInputLayout.enableErrorResettingOnTextChanged()}

            EMPTY_USERNAME_AND_PASSWORD -> {
                binding.usernameInputLayout.error = getString(R.string.enter_username)
                binding.usernameInputLayout.enableErrorResettingOnTextChanged()
                binding.passwordInputLayout.error = getString(R.string.enter_password)
                binding.passwordInputLayout.enableErrorResettingOnTextChanged()
            }

            TOKEN_ACCESS_DENIED -> {binding.passwordInputLayout.error = getString(R.string.token_access_denied)
                binding.passwordInputLayout.enableErrorResettingOnTextChanged()}

            USERNAME_PASSWORD_INCORRECT -> {
                binding.passwordInputLayout.error = "Username or password is incorrect."
                binding.passwordInputLayout.enableErrorResettingOnTextChanged()
                binding.passwordInputLayout.setErrorIconDrawable(null)
            }
//            NO_INTERNET_CONNECTION -> showNoInternetDialog()

            else -> binding.usernameInputLayout.error = getString( R.string.sign_in_error)
        }
    }

    companion object{


        fun start(context: Context){
//            val intent = Intent(context, MessageListActivity::class.java)
//            val intent = Intent(context, ConversationListActivity::class.java)
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
            intent.putExtra("fullName",LoginConstants.FULL_NAME)
            intent.putExtra("showContacts",LoginConstants.SHOW_CONTACTS)
            intent.putExtra("isStandalone",LoginConstants.IS_STANDALONE)
            context.startActivity(intent)


        }
    }

}