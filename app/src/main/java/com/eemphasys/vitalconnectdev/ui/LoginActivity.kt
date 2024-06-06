package com.eemphasys.vitalconnectdev.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.eemphasys.vitalconnect.MainActivity
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.extensions.hideKeyboard
import android.Manifest
import android.os.CountDownTimer
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View.VISIBLE
import androidx.core.app.ActivityCompat
import com.eemphasys.vitalconnect.common.extensions.enableErrorResettingOnTextChanged
import com.eemphasys.vitalconnectdev.R
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError
import com.eemphasys.vitalconnectdev.common.enums.ConversationsError.*
import com.eemphasys.vitalconnectdev.common.extensions.lazyViewModel
import com.eemphasys.vitalconnectdev.common.extensions.onSubmit
import com.eemphasys.vitalconnectdev.common.injector
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import  com.eemphasys_enterprise.commonmobilelib.EETLog
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    val loginViewModel by lazyViewModel { injector.createLoginViewModel(application) }

    private lateinit var countDownTimer: CountDownTimer
    private val totalTimeInMillis: Long = 60000 // Total time for the timer (60 seconds)

    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.loginCoordinatorLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }
    private val passwordUpdateSnackbar by lazy {
        Snackbar.make(binding.loginCoordinatorLayout, "Password changed successfully", Snackbar.LENGTH_INDEFINITE)
    }

    override fun onStart() {
        super.onStart()
        setContentView(binding.root)
    }

    override fun onStop() {
        super.onStop()

        // Clear input fields when the activity is stopped
        binding.usernameTv.text!!.clear()
        binding.passwordTv.text!!.clear()
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

                binding.usernameInputLayoutRP.visibility = GONE
                binding.passwordInputLayoutRP.visibility = GONE
                binding.confirmpasswordInputLayoutRP.visibility = GONE
                binding.sendOTP.visibility= GONE

        binding.TenantCodeInputLayoutRP.editText?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                binding.progressBarID.visibility = VISIBLE
                loginViewModel.isAzureADEnabled(s?.toString()!!)
            }

        })

        binding.confirmpasswordInputLayoutRP.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                // Call the comparison logic
                comparePasswords()
            }
        })

        binding.pinview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val otp = s?.toString().orEmpty()
                // Enable the button if the OTP field is full
                binding.saveBtn.isEnabled = otp.length == 6
            }
        })

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
                binding.progressBarID.visibility = GONE
                binding.TenantCodeInputLayoutRP.error = "Password can't be changed for this tenant"
                binding.TenantCodeInputLayoutRP.enableErrorResettingOnTextChanged()
                binding.usernameInputLayoutRP.visibility = GONE
                binding.passwordInputLayoutRP.visibility = GONE
                binding.confirmpasswordInputLayoutRP.visibility = GONE
                binding.sendOTP.visibility= GONE

            }
            else{
                binding.progressBarID.visibility = GONE

                binding.usernameInputLayoutRP.visibility = VISIBLE
                binding.passwordInputLayoutRP.visibility = VISIBLE
                binding.confirmpasswordInputLayoutRP.visibility = VISIBLE
                binding.sendOTP.visibility= VISIBLE
            }
        }

        loginViewModel.isPasswordUpdated.observe(this){
            if(it){
                passwordUpdateSnackbar.show()
                binding.passwordresetLayout.visibility = GONE
                binding.loginLayout.visibility= VISIBLE
                passwordUpdateSnackbar.dismiss()
            }
            else{
//                binding.confirmpasswordInputLayoutRP.error = "Failed to update password. Try again later."
            }
        }
        initializeChatAppModel()
        try
        {
            if
                    (!checkAndRequestPermissions())
                return
        }
        catch
            (e:Exception){
            e.printStackTrace()
        }
    }


    private val REQUESTIDMULTIPLEPERMISSIONS = 1
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if
                (requestCode == REQUESTIDMULTIPLEPERMISSIONS) {
            for (permission in grantResults) {
                val permission = permission
                var isPermitted = permission == PackageManager.PERMISSION_GRANTED
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
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        val listPermissionsNeeded: MutableList<String> = ArrayList()

        if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUESTIDMULTIPLEPERMISSIONS
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

    private fun comparePasswords(){
        val password = binding.passwordTvRP.text.toString()
        val confirmPassword = binding.confirmpasswordTvRP.text.toString()

        if (password == confirmPassword) {
            // Clear any existing error
            binding.confirmpasswordInputLayoutRP.error = null
        } else {
            // Show error message
            binding.confirmpasswordInputLayoutRP.error = "Passwords do not match"
        }

    }
            private fun startCountdownTimer() {
                countDownTimer.start()
                binding.textViewTimer.visibility = VISIBLE
            }

            private fun updateTimerUI(secondsLeft: Long) {
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                val timerText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                // Update your TextView with the timer text
                binding.textViewTimer.text = "Resend OTP in " + timerText + " seconds"
            }

            private fun enableResendButton() {
                binding.sendOTP.visibility = VISIBLE
            }

            private fun initCountDownTimer() {
                countDownTimer = object : CountDownTimer(totalTimeInMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val secondsLeft = millisUntilFinished / 1000
                        updateTimerUI(secondsLeft)
                    }

                    override fun onFinish() {
                        enableResendButton()
                        binding.textViewTimer.visibility = GONE
                    }
                }
            }


    fun onTextViewClickForgotPassword(view: View){
        binding.loginLayout.visibility = GONE
        binding.passwordresetLayout.visibility = VISIBLE
    }

    private fun validateInputs(): Boolean{
        val tenantCode = binding.tenantcodeTvRP.text.toString()
        val username = binding.usernameTvRP.text.toString()
        val password = binding.confirmpasswordTvRP.text.toString()

        if(tenantCode.isBlank()){
            binding.TenantCodeInputLayoutRP.error = "Tenant Code can't be empty."
            binding.TenantCodeInputLayoutRP.enableErrorResettingOnTextChanged()
            return false
        }
        if(username.isBlank()){
            binding.usernameInputLayoutRP.error = "Username can't be empty."
            binding.usernameInputLayoutRP.enableErrorResettingOnTextChanged()
            return false
        }
        if(password.isBlank()){
            binding.confirmpasswordInputLayoutRP.error = "Please enter password again"
            binding.confirmpasswordInputLayoutRP.enableErrorResettingOnTextChanged()
            return false
        }

        val passwordRegex = Regex("""^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()_+-=]).{8,}$""")

        if (password.matches(passwordRegex) && password.length > 8) {
        } else {
           binding.passwordInputLayoutRP.error = "Password length should be greater than 8  and should have atleast 1 uppercase 1 lowercase 1 number and 1 special character"
            binding.passwordInputLayoutRP.enableErrorResettingOnTextChanged()
            return false
        }

        return true
    }
    fun onTextViewClickSendOTP(view: View){
        val tenantCode = binding.tenantcodeTvRP.text.toString()
        val username = binding.usernameTvRP.text.toString()
        val valid = validateInputs()
        if(valid) {
            binding.progressBarID.visibility = VISIBLE
            loginViewModel.sendOtp(tenantCode,username) { success ->
                if (success) {
                    binding.progressBarID.visibility = GONE
                    binding.enterotp.visibility = VISIBLE
                    binding.pinview.visibility = VISIBLE
                    initCountDownTimer()  //timer starts
                    startCountdownTimer()
                    binding.sendOTP.visibility = GONE
                } else {
                    binding.progressBarID.visibility = GONE
                    binding.usernameInputLayoutRP.error = "Enter valid tenant code or username."
                    binding.usernameInputLayoutRP.errorIconDrawable = null
                    binding.usernameInputLayoutRP.enableErrorResettingOnTextChanged()
                }
            }
        }
        }

    fun updatePassword(view: View){
        val tenantCode = binding.tenantcodeTvRP.text.toString()
        val username = binding.usernameTvRP.text.toString()
        val password = binding.confirmpasswordTvRP.text.toString()
        val otp = binding.pinview.text.toString()
        loginViewModel.updatePassword(tenantCode,username,password,otp)
    }

    fun goToLogin(view:View){
        binding.passwordresetLayout.visibility= GONE
        binding.loginLayout.visibility= VISIBLE
    }

    private fun signInPressed() {
        val identity = binding.usernameTv.text.toString()
        LoginConstants.CURRENT_USER = identity
        LoginConstants.FRIENDLY_NAME = identity
        val password = binding.passwordTv.text.toString()
//        credentialStorage.identity = identity
//        credentialStorage.password= password

        loginViewModel.signIn(identity,password)
    }

    private fun showProgress(show: Boolean) {
        binding.loginProgress.root.visibility = if (show) VISIBLE else GONE
        binding.loginLayout.visibility = if (show) GONE else VISIBLE
    }

    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }

    private fun goToChildAppScreen() {
        loginViewModel.isLoading.value = false
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
                binding.passwordInputLayout.errorIconDrawable = null
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
            intent.putExtra("customerNumber","9876543210")
            intent.putExtra("customerName","DummyUser")
            intent.putExtra("showConversations","true")
            intent.putExtra("userSMSAlert",LoginConstants.USER_SMS_ALERT)
            context.startActivity(intent)


        }
    }

}