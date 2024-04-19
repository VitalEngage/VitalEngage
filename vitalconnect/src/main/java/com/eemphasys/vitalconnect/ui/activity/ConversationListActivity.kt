package com.eemphasys.vitalconnect.ui.activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.eemphasys.vitalconnect.MainActivity
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.ActivityConversationListBinding
import com.eemphasys.vitalconnect.ui.fragment.ContactListFragment
import com.eemphasys.vitalconnect.ui.fragment.ConversationListFragment
import com.eemphasys.vitalconnect.ui.fragment.ProfileFragment
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.bottomnavigation.BottomNavigationView


class ConversationListActivity:AppCompatActivity() {
    val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
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
        val fullName = intent.getStringExtra("fullName")

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
        Constants.FULL_NAME = fullName!!

        mainViewModel.create()
        val binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val menubottom : BottomNavigationView = findViewById(R.id.bottom_navigation)
        val menu = menubottom.menu
        val menuItem = menu.findItem(R.id.page_contact_list)
        menubottom.itemIconTintList = null
        val isDataAvailable = Constants.CONTACTS.isNullOrEmpty() || Constants.WEBUSERS.isNullOrEmpty()
        menuItem.isVisible = !isDataAvailable

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.page_contact_list -> replaceFragment(ContactListFragment())
                R.id.page_conversation_list -> replaceFragment(ConversationListFragment())
                R.id.page_profile -> replaceFragment(ProfileFragment())

            }
            return@setOnItemSelectedListener true
        }

        if (savedInstanceState == null) {
            if (!isDataAvailable) {
                replaceFragment(ContactListFragment())
            }
            else {
                replaceFragment(ConversationListFragment())
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    override fun onBackPressed() {
        super.onBackPressed()
    }
    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { currentFragment ->
            if (currentFragment::class == fragment::class) {
                return
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }
    companion object {

        fun start(context: Context) {
            val intent = getStartIntent(context)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun getStartIntent(context: Context) =
            Intent(context, ConversationListActivity::class.java)
    }
}