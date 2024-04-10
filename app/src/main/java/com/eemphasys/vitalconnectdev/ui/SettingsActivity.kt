package com.eemphasys.vitalconnectdev.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnectdev.R
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

        try {
            baseurl.setText(LoginConstants.BASE_URL)
            tenantcode.setText(LoginConstants.TENANT_CODE)
        } catch (e: Exception) {
            e.printStackTrace()

            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }

        // Set click listener using binding
        binding.save.setOnClickListener {
            // Update constants
            LoginConstants.BASE_URL = baseurl.text.toString()
            LoginConstants.TENANT_CODE = tenantcode.text.toString()

            // Start LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}


