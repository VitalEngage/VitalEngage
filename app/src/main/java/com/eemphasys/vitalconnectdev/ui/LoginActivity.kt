package com.eemphasys.vitalconnectdev.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.MainActivity
import com.eemphasys.vitalconnect.data.localCache.LocalCacheProvider
import com.eemphasys.vitalconnectdev.ChatApplication
import com.eemphasys.vitalconnectdev.R
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError.*
import com.eemphasys.vitalconnectdev.common.extensions.lazyViewModel
import com.eemphasys.vitalconnectdev.common.extensions.onSubmit
import com.eemphasys.vitalconnectdev.common.injector
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    val loginViewModel by lazyViewModel { injector.createLoginViewModel(application) }

    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.loginCoordinatorLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        val items = listOf("e-servicetech", "e-logistics", "e-serviceplus")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.dropdown_product)

        autoCompleteTextView.setAdapter(adapter)
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

        binding.passwordTv.onSubmit { signInPressed() }
        binding.signInBtn.setOnClickListener { signInPressed() }
        binding.settingsBtn.setOnClickListener { openSettings() }
    }

//    override fun onDestroy() {
//       // Timber.d("onDestroy")
//        super.onDestroy()
//    }

    private fun signInPressed() {
        //Timber.d("signInPressed")
        val identity = binding.usernameTv.text.toString()
        val password = binding.passwordTv.text.toString()

        loginViewModel.signIn(identity, password)
    }

    private fun showProgress(show: Boolean) {
        //Timber.d("showProgress: $show")
        binding.loginProgress.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showNoInternetSnackbar(show: Boolean) {
        // Timber.d("showNoInternetSnackbar: $show")

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }

    private fun goToChildAppScreen() {
        val identity = binding.usernameTv.text.toString()
        val selectedItem = binding.dropdownProduct.text.toString()

        if (selectedItem == "e-servicetech") {
            val cliendID = "12345"
            val clientSecret = "servicetech"
            start(this, cliendID, clientSecret, identity)
        } else if (selectedItem == "e-logistics") {
            val cliendID = "12345"
            val clientSecret = "logistics"
            start(this, cliendID, clientSecret, identity)
        } else if (selectedItem == ("e-serviceplus")) {
            val cliendID = "12345"
            val clientSecret = "plus"
            start(this, cliendID, clientSecret, identity)
        }

    }

    private fun openSettings(){
        val intent = Intent(this, Settings::class.java)
        startActivity(intent)
    }

    private fun signInSucceeded() = goToChildAppScreen()

    private fun signInFailed(error: ConversationsError) {
        when (error) {
            EMPTY_USERNAME -> binding.usernameInputLayout.error = getString(R.string.enter_username)

            EMPTY_PASSWORD -> binding.passwordInputLayout.error = getString(R.string.enter_password)

            EMPTY_USERNAME_AND_PASSWORD -> {
                binding.usernameInputLayout.error = getString(R.string.enter_username)
                binding.passwordInputLayout.error = getString(R.string.enter_password)
            }

            //TOKEN_ACCESS_DENIED -> binding.passwordInputLayout.error = getString(R.string.token_access_denied)

//            NO_INTERNET_CONNECTION -> showNoInternetDialog()

            else -> binding.passwordInputLayout.error = getString( R.string.sign_in_error)
        }
    }

    companion object{


        fun start(context: Context, clientID: String, clientSecret: String, identity:String ){
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("username", identity)
            intent.putExtra("clientID", clientID)
            intent.putExtra("clientSecret", clientSecret)
            intent.putExtra("baseurl",LoginConstants.BASE_URL)
            intent.putExtra("tenantcode",LoginConstants.TENANT_CODE)
            context.startActivity(intent)


        }
    }

}