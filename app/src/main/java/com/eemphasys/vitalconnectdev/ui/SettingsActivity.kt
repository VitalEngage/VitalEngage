package com.eemphasys.vitalconnectdev.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.eemphasys.vitalconnectdev.R
import com.eemphasys.vitalconnectdev.data.LoginConstants
import com.eemphasys.vitalconnectdev.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding // Declare binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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


