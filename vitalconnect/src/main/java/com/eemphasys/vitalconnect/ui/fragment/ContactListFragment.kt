package com.eemphasys.vitalconnect.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.viewpager2.widget.ViewPager2
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.PagerAdapter
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ChatAppModel
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

    override fun onResume() {
        super.onResume()
//        ChatAppModel.FirebaseLogEventListener?.screenLogEvent(requireContext(),"VC_Contacts","ContactListFragment")
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
//        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
//            when (position) {
//                0 -> tab.text = if (showInternal) Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!! else "Customer"
//                1 -> tab.text = if (showExternal) "Customer" else Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!!
//            }
//        }.attach()
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Prepare the tab icon and text
            val context = tabLayout.context
            var tabText: String? = ""
            var tabIconResId: Int = 0

            when (position) {
                0 -> {
                    tabText = if (showInternal) Constants.getStringFromVitalTextSharedPreferences(applicationContext, "dealerName")!! else "Customer"
                    tabIconResId = if (showInternal) R.drawable.ic_internal_selector else R.drawable.ic_external_selector
                }
                1 -> {
                    tabText = if (showExternal) "Customer" else Constants.getStringFromVitalTextSharedPreferences(applicationContext, "dealerName")!!
                    tabIconResId = if (showExternal) R.drawable.ic_external_selector else R.drawable.ic_internal_selector
                }
            }

            if (context != null) {
            // Create a custom view for the tab
            val customTabView = LayoutInflater.from(context).inflate(R.layout.custom_fragment_tab,null)
            val tabIcon = customTabView.findViewById<ImageView>(R.id.tab_icon)
            val tabTextView = customTabView.findViewById<TextView>(R.id.tab_text)

                if(tabTextView != null) {
                    tabTextView.text = tabText ?: ""
                }
            tabIcon.setImageResource(tabIconResId)

            tab.customView = customTabView

            // Listen for tab selection changes to update the icon and text
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(selectedTab: TabLayout.Tab?) {
                    if (selectedTab == tab) {
                        tabIcon.setImageResource(tabIconResId) // Set the active icon
                        tabTextView.setTextColor(ContextCompat.getColor(context, R.color.tabSelected)) // Set selected text color

                        val selectedTabText = tabTextView.text.toString()

                        if(selectedTabText =="Customer"){
                            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_Contacts_ExternalContactsTabClick",
                                "Contacts",
                                "ContactListFragment"
                            )
                        }else{
                            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_Contacts_InternalUsersTabClick",
                                "Contacts",
                                "ContactListFragment"
                            )
                        }
                    }
                }

                override fun onTabUnselected(unselectedTab: TabLayout.Tab?) {
                    if (unselectedTab == tab) {
                        tabTextView.setTextColor(ContextCompat.getColor(context, R.color.alternate_message_text)) // Set unselected text color
                    }
                }

                override fun onTabReselected(p0: TabLayout.Tab?) {
                    // No-op for re-selection
                }

            })
            } else {
                Log.e("TabLayout", "Context is null!")
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
            if (!isNetworkAvailable){
                Constants.showPopup(layoutInflater, requireActivity())
                var layout = layoutInflater.inflate(R.layout.activity_conversation_list, null)
                var text = layout.findViewById<TextView>(R.id.textBox)
                text.text = "offline"
                text.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            }
//                activity?.finish()
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