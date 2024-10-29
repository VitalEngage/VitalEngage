package com.eemphasys.vitalconnect.ui.fragment

import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.text.toLowerCase
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.viewpager2.widget.ViewPager2
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.PagerAdapter
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.Constants.Companion.isValidPhoneNumber
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants.Companion.getSearchViewEditText
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.FragmentContactListBinding
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.twilio.conversations.Attributes
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Field
import java.util.Locale
import java.util.concurrent.TimeUnit

class ContactListFragment : Fragment() {
    var binding: FragmentContactListBinding? = null
    val contactListViewModel by lazyActivityViewModel { injector.createContactListViewModel(applicationContext) }
    private val noInternetSnackBar by lazy {
        Snackbar.make(binding!!.contactsListLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
    }

    override fun onResume() {
        super.onResume()
        ChatAppModel.FirebaseLogEventListener?.screenLogEvent(requireContext(),"Contacts","ContactListFragment")
    }
    fun shouldInterceptBackPress() = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreateView Called")
        binding = FragmentContactListBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.contacts)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        // Determine flags based on your conditions
        val showInternal = (Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showInternalContacts")!! == "true")
        val showExternal = (Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showExternalContacts")!! == "true")

        val pagerAdapter = PagerAdapter(this, showInternal, showExternal)
        viewPager.adapter = pagerAdapter

        // Bind TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = if (showInternal) Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!! else "Customer"
                1 -> tab.text = if (showExternal) "Customer" else Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!!
            }
        }.attach()

        tabLayout.setTabTextColors(
            ContextCompat.getColor(
                applicationContext,
                R.color.alternate_message_text
            ), // Normal text color
            ContextCompat.getColor(applicationContext, R.color.tabSelected) // Selected text color
        )
        tabLayout.setSelectedTabIndicatorColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.tabSelected
            )
        )

        binding?.contactList?.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        contactListViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if (!isNetworkAvailable)
                activity?.finish()
        }
    }

    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }

}