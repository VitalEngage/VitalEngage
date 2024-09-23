package com.eemphasys.vitalconnect.ui.activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.databinding.ActivityConversationListBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.ui.fragment.ContactListFragment
import com.eemphasys.vitalconnect.ui.fragment.ConversationListFragment
import com.eemphasys.vitalconnect.ui.fragment.ProfileFragment
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ConversationListActivity:AppCompatActivity() {
    private val mainViewModel by lazyViewModel { injector.createMainViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " +this::class.java.simpleName + " onCreate Called")

        mainViewModel.create()
        val binding = ActivityConversationListBinding.inflate(layoutInflater)
        mainViewModel.getUserAlertStatus()
        setContentView(binding.root)
        val fragmentToShow = intent.getIntExtra(EXTRA_FRAGMENT_TO_SHOW, 1)
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
            when (fragmentToShow) {
                1 -> {replaceFragment(ContactListFragment())
                    menu.findItem(R.id.page_contact_list).setChecked(true)}
                2 -> {replaceFragment(ConversationListFragment())
                    menu.findItem(R.id.page_conversation_list).setChecked(true)}
                3 -> {replaceFragment(ProfileFragment())
                    menu.findItem(R.id.page_profile).setChecked(true)}
                else -> replaceFragment(ContactListFragment()) // Default case
            }
        }
        if(Constants.IS_STANDALONE == "false")
        {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onStart() {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onStart Called")
        super.onStart()
        try {
            if(ConversationsClientWrapper.INSTANCE.isClientCreated){

            }
            else{
                Log.d("onStart ConversationListActivity","finishing activity")
//                this.finish()
            }
        }catch(e: Exception){
            Log.d("onStart ConversationListActivity", e.message.toString())
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
    }

    override fun onResume() {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onResume Called")
        super.onResume()
        try {
            if(ConversationsClientWrapper.INSTANCE.isClientCreated){

            }
            else{
                Log.d("onResume ConversationListActivity","finishing activity")
//                this.finish()
            }
        }catch(e: Exception){
            Log.d("onResume ConversationListActivity", e.message.toString())
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
    }

    override fun onStop() {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onStop Called")
        super.onStop()
        try {

        }catch(e: Exception){
            Log.d("onStop ConversationListActivity", e.message.toString())
            EETLog.error(
                AppContextHelper.appContext!!, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
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
        private const val EXTRA_FRAGMENT_TO_SHOW = "fragment_to_show"
        fun start(context: Context, fragmentToShow: Int) {
            val intent = getStartIntent(context).apply {
                putExtra(EXTRA_FRAGMENT_TO_SHOW, fragmentToShow)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun start(context: Context) {
            val intent = getStartIntent(context)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun getStartIntent(context: Context) =
            Intent(context, ConversationListActivity::class.java)
    }
}