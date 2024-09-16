package com.eemphasys.vitalconnect.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.PagerAdapter
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.FragmentContactListBinding
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

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
        val showInternal = Constants.SHOW_INTERNAL_CONTACTS
        val showExternal = Constants.SHOW_EXTERNAL_CONTACTS

        val pagerAdapter = PagerAdapter(this, showInternal, showExternal)
        viewPager.adapter = pagerAdapter

        // Bind TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = if (showInternal) Constants.DEALER_NAME else "Customer"
                1 -> tab.text = if (showExternal) "Customer" else Constants.DEALER_NAME
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