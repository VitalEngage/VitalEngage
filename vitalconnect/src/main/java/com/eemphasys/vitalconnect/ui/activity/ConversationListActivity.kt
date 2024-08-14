package com.eemphasys.vitalconnect.ui.activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.databinding.ActivityConversationListBinding
import com.eemphasys.vitalconnect.ui.fragment.ContactListFragment
import com.eemphasys.vitalconnect.ui.fragment.ConversationListFragment
import com.eemphasys.vitalconnect.ui.fragment.ProfileFragment
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ConversationListActivity:AppCompatActivity() {
    private val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " +this::class.java.simpleName + " onCreate Called")

//        mainViewModel.create()
        val binding = ActivityConversationListBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val menubottom : BottomNavigationView = findViewById(R.id.bottom_navigation)
        val menu = menubottom.menu
        val menuItem = menu.findItem(R.id.page_contact_list)
        menubottom.itemIconTintList = null

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.page_contact_list -> replaceFragment(ContactListFragment())
                R.id.page_conversation_list -> replaceFragment(ConversationListFragment())
                R.id.page_profile -> replaceFragment(ProfileFragment())

            }
            return@setOnItemSelectedListener true
        }

        if (savedInstanceState == null) {
//            if (isDataAvailable) {
                replaceFragment(ContactListFragment())
//            }
//            else {
//                replaceFragment(ConversationListFragment())
//            }
        }
        if(Constants.IS_STANDALONE == "false")
        {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    override fun onBackPressed() {

        if(Constants.IS_STANDALONE == "true") {
//            this.moveTaskToBack(true)
        }
        else
        {
            super.onBackPressed()
//            profileViewModel.signOut()
        }
    }
    private fun replaceFragment(fragment: Fragment) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " replaceFragment Called")
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