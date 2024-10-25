package com.eemphasys.vitalconnectdev.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.databinding.ActivitySettingsBinding
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding // Declare binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
        binding = ActivitySettingsBinding.inflate(layoutInflater) // Inflate binding
        setContentView(binding.root)

        // Access EditText fields through binding
        val baseurl = binding.baseurl
        val tenantcode = binding.tenantcode

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        baseurl.setText(sharedPreferences.getString("baseurlvalue", ""))
        tenantcode.setText(sharedPreferences.getString("tenantcodevalue", ""))

        // Set click listener using binding
        binding.save.setOnClickListener {
            // Update constants
            LoginConstants.BASE_URL = baseurl.text.toString()
            LoginConstants.TENANT_CODE = tenantcode.text.toString()

            // Save values to SharedPreferences
            with(sharedPreferences.edit()) {
                putString("baseurlvalue", baseurl.text.toString())
                putString("tenantcodevalue", tenantcode.text.toString())
                apply() // Save changes
            }
            // Start LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}


