package com.eemphasys.vitalconnect.ui.activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.databinding.ActivityConversationListBinding
import com.eemphasys.vitalconnect.ui.fragment.ContactListFragment
import com.eemphasys.vitalconnect.ui.fragment.ConversationListFragment


class ConversationListActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        val binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.page_contact_list -> replaceFragment(ContactListFragment())
                R.id.page_conversation_list -> replaceFragment(ConversationListFragment())
               /* R.id.page_profile -> replaceFragment(ProfileFragment())*/

            }
            return@setOnItemSelectedListener true
        }

        if (savedInstanceState == null) {
            replaceFragment(ContactListFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        //Timber.d("replaceFragment")

        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { currentFragment ->
            if (currentFragment::class == fragment::class) {
                //Timber.d("replaceFragment: skip")
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