package com.eemphasys.vitalconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.eemphasys.vitalconnect.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val username = intent.getStringExtra("username")
        val clientID = intent.getStringExtra("clientID")
        val clientSecret = intent.getStringExtra("clientSecret")
        val tenantcode = intent.getStringExtra("tenantcode")
        val baseurl = intent.getStringExtra("baseurl")

        binding.text.text = username
        binding.text1.text = clientID
        binding.text2.text = clientSecret
        binding.text3.text= tenantcode
        binding.text4.text= baseurl
//        setContent {
//            VitalConnect_devTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Greeting("Android")
//                }
//            }
//        }

    }
}
