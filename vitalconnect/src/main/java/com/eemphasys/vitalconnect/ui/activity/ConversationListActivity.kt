package com.eemphasys.vitalconnect.ui.activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.databinding.ActivityConversationListBinding
import com.eemphasys.vitalconnect.ui.fragment.ContactListFragment
import com.eemphasys.vitalconnect.ui.fragment.ConversationListFragment
import com.eemphasys.vitalconnect.ui.fragment.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView


class ConversationListActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val menubottom : BottomNavigationView = findViewById(R.id.bottom_navigation)
        val menu = menubottom.menu
        val menuItem = menu.findItem(R.id.page_contact_list)

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