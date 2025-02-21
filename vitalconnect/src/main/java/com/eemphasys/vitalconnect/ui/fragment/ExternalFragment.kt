package com.eemphasys.vitalconnect.ui.fragment
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.ContactListAdapter
import com.eemphasys.vitalconnect.adapters.OnContactItemClickListener
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ContactListRequest
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchContactResponse
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.Contact
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.FragmentExternalBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.snackbar.Snackbar
import com.twilio.conversations.Attributes
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class ExternalFragment : Fragment() {
    var binding: FragmentExternalBinding? = null
    val contactListViewModel by lazyActivityViewModel { injector.createContactListViewModel(applicationContext) }
    private val noInternetSnackBar by lazy {
        Snackbar.make(requireView(), R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }
    private var contactsList = arrayListOf<Contact>()
    private lateinit var adapter: ContactListAdapter
    private lateinit var originalList: List<ContactListViewItem>
    private var listOfContacts = mutableListOf<ContactListViewItem>()
//    var currentIndex: Int = 1
//    var maxPageSize: Int = 1

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_contact_list, menu)

        val searchItem = menu.findItem(R.id.filter_contacts)
        val searchView = searchItem?.actionView as SearchView

        (searchItem.actionView as SearchView).apply {
            queryHint = getString(R.string.search_contacts)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(isAdded) {
                    val text = newText.orEmpty()
                    Log.d("length", text.length.toString())
                    if (text.length >= 3) {
                        if (Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false") {
                            //Search using SearchedUsers api
//                            lifecycleScope.launch {
//                                val listOfSearchedContacts = mutableListOf<ContactListViewItem>()
//                                var request =
//                                    SearchContactRequest(
//                                        Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,
//                                        Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
//                                        newText!!
//                                    )
//                                var response = RetrofitClient.getRetrofitWithToken().getSearchedContact(request)
//
//                                response.enqueue(object : Callback<List<SearchContactResponse>> {
//                                    override fun onResponse(
//                                        call: Call<List<SearchContactResponse>>,
//                                        response: Response<List<SearchContactResponse>>
//                                    ) {
//                                        if (response.isSuccessful) {
//                                            var contactsResponse: List<SearchContactResponse>? =
//                                                response.body()
//                                            if (!contactsResponse.isNullOrEmpty()) {
//
//                                                for (response in contactsResponse) {
//                                                    var contactItem =
//                                                        ContactListViewItem(
//                                                            response.fullName,
//                                                            "",
//                                                            response.mobileNumber,
//                                                            "SMS",
//                                                            Constants.getInitials(response.fullName.trim { it <= ' ' }),
//                                                            response.designation,
//                                                            response.department,
//                                                            response.customerName,
//                                                            "",
//                                                            true,
//                                                            response.bpId,
//                                                            response.role
//                                                        )
//
//                                                    listOfSearchedContacts.add(contactItem)
//                                                }
//                                                setAdapter(listOfSearchedContacts)
//                                            }
//                                        }
//                                    }
//
//                                    override fun onFailure(
//                                        call: Call<List<SearchContactResponse>>,
//                                        t: Throwable
//                                    ) {
//
//                                    }
//
//                                })
//
//                            }
                            setAdapter(listOfContacts)
                        } else {
                            setAdapter(originalList)
                        }
                        adapter.filter(text)
                        return true
                    } else {
                        if (Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false") {
                            setAdapter(listOfContacts)
                        } else {
                            setAdapter(originalList)
                        }
                        return false
                    }
                }else {
                        return false
                    }
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        setHasOptionsMenu(true)

        contactListViewModel.onParticipantAdded.observe(this) { identity ->
            binding?.contactList?.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
        }
    }
    override fun onResume() {
        // enable interaction
        binding?.contactList?.isClickable = true
        binding?.contactList?.isEnabled = true
        super.onResume()
        ChatAppModel.FirebaseLogEventListener?.screenLogEvent(requireContext(),"VC_CustomerContacts","ExternalFragment")
    }
    fun getAllContactList(callBack: () -> Unit){
        lifecycleScope.launch {
//        var request = ContactListRequest(currentIndex,Constants.PAGE_SIZE.toInt(),"","fullName","asc",Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,0)
            var request = ContactListRequest(0,0,"","fullName","asc",Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,0)
            var response = RetrofitClient.getRetrofitWithToken().getContactList(request)

        if (response.isSuccessful) {
//            var previousPosition = listOfContacts.size
            for (contact in response.body()!!.contacts) {
                var userItem = ContactListViewItem(
                    contact.fullName,
                    "",
                    contact.mobileNumber,
                    "SMS",
                    Constants.getInitials(contact.fullName.trim { it <= ' ' }),
                    contact.designation,
                    contact.department,
                    contact.customerName,
                    "",
                    true,
                    contact.bpId,
                    contact.role
                )
                listOfContacts.add(userItem)
            }
//            adapter.notifyItemRangeInserted(previousPosition,listOfContacts.size)
//            maxPageSize = ceil((response.body()!!.totalCount/Constants.PAGE_SIZE)).toInt()
        }
            callBack.invoke()
    }
    }
    private fun formatList(contacts: List<Contact>): List<ContactListViewItem> {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " combineLists Called")
        val combinedList = mutableListOf<ContactListViewItem>()

        // Convert Contact objects to ContactListViewItem
        val contactItems = contacts.map {
            ContactListViewItem(it.name, "", it.number, "SMS",it.initials,it.designation,it.department,it.customer,it.countryCode,false,it.bpId,it.role)
        }

        // Add all ContactListViewItem objects to the combined list
        combinedList.addAll(contactItems)

        return combinedList
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.contacts)

        contactListViewModel.onDetailsError.observe(viewLifecycleOwner){ error ->
            Log.d("snackbar",error.toString())
            binding!!.azScrollbar.showSnackbar(applicationContext.getErrorMessage(error))
            binding?.progressBarID?.visibility = View.GONE
        }
        binding?.contactList?.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

        contactListViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
//            if(!isNetworkAvailable)
//                activity?.finish()
        }
        contactsList = ArrayList<Contact>()
        if(!Constants.getStringFromVitalTextSharedPreferences(applicationContext,"contacts")!!.isNullOrEmpty()) {
            val jsonObjectcontacts = JSONObject(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"contacts")!!)
            val jsonArrayContacts = jsonObjectcontacts.getJSONArray("contacts")
            for (i in 0 until jsonArrayContacts.length()) {
                val jsonObject = jsonArrayContacts.getJSONObject(i)
                val name = jsonObject.getString("name")
                val number = Constants.formatPhoneNumber(applicationContext,jsonObject.getString("number"),jsonObject.getString("countryCode"))
                val customerName = jsonObject.getString("customerName")
                val initials = Constants.getInitials(name.trim { it <= ' ' })
                val designation = jsonObject.getString("designation")
                val department = jsonObject.getString("department")
                val customer = jsonObject.getString("customer")
                val countryCode = jsonObject.getString("countryCode")
                val role = jsonObject.getString("role")
                val bpId = jsonObject.getString("bpId")
                contactsList.add(Contact(name, number, customerName, initials, designation, department, customer,countryCode,bpId,role))
            }
        }
        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false"){

            if(listOfContacts.isEmpty()) {
                binding?.progressBarID?.visibility = View.VISIBLE
                getAllContactList {
                    binding?.progressBarID?.visibility = View.GONE
                    setAdapter(listOfContacts)
                }
            }
        }
        else
        {
            originalList = formatList(contactsList)
            setAdapter(originalList)
        }

//        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false") {
//            binding?.contactList!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                    super.onScrollStateChanged(recyclerView, newState)
//                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                        if (!recyclerView.canScrollVertically(1)) {
//                            Log.d("scroll---up1", newState.toString())
//                            currentIndex++
//                            if(maxPageSize>=currentIndex) {
//                                binding!!.progressBarRecyclerview.visibility = View.VISIBLE
//                            }
//                            getAllContactList {
//                                binding!!.progressBarRecyclerview.visibility = View.GONE
//                            }
//                        }
//                    }
//                }
//            })
//        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setAdapter(list : List<ContactListViewItem>){
        //Creating the Adapter
        adapter = ContactListAdapter(list,list,applicationContext,object : OnContactItemClickListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onContactItemClick(contact: ContactListViewItem) {
                // disable interaction
                binding?.contactList?.isClickable = false
                binding?.contactList?.isEnabled = false
                var phone  = Constants.cleanedNumber(Constants.formatPhoneNumber(applicationContext,contact.number,contact.countryCode!!))
//                    if(Constants.isValidPhoneNumber(phone, Locale.getDefault().country)) {
                        binding?.progressBarID?.visibility = View.VISIBLE

                        val existingConversation = RetrofitClient.getRetrofitWithToken().fetchExistingConversation(
                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                            Constants.cleanedNumber(
                                Constants.formatPhoneNumber(
                                    applicationContext,
                                    contact.number,
                                    contact.countryCode!!
                                )
                            ),
                            false,
                            1,
                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"proxyNumber")!!
                        )
                Log.d("contactNumberClicked",Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!! + "  " +
                    Constants.cleanedNumber(
                        Constants.formatPhoneNumber(
                            applicationContext,
                            contact.number,
                            contact.countryCode!!
                        )
                    )  + "  " +
                    false  + "  " +
                    1  + "  " +
                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"proxyNumber")!!
                )
                        Log.d(
                            "contactNumberClicked",
                            Constants.cleanedNumber(
                                Constants.formatPhoneNumber(
                                    applicationContext,
                                    contact.number,
                                    contact.countryCode!!
                                )
                            )
                        )
                        existingConversation.enqueue(object :
                            Callback<List<ParticipantExistingConversation>> {
                            @SuppressLint("SuspiciousIndentation")
                            override fun onResponse(
                                call: Call<List<ParticipantExistingConversation>>,
                                response: Response<List<ParticipantExistingConversation>>
                            ) {
                                if (response.isSuccessful) {
                                    val conversationList: List<ParticipantExistingConversation>? =
                                        response.body()

                                    // Check if the list is not null and not empty
                                    if (!conversationList.isNullOrEmpty()) {
                                        // Iterate through the list and access each Conversation object
                                        for (conversation in conversationList) {
                                            // Access properties of each Conversation object
                                            println("Conversation SID: ${conversation.conversationSid}")
                                            //when conversation exists
                                            if (!conversation.conversationSid.isNullOrEmpty()) {
                                                try {
                                                    val participantSid =
                                                        RetrofitClient.getRetrofitWithToken().addParticipantToConversation(
                                                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                                                            conversation.conversationSid,
                                                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!
                                                        )

                                                    participantSid.enqueue(object :
                                                        Callback<String> {
                                                        override fun onResponse(
                                                            call: Call<String>,
                                                            response: Response<String>
                                                        ) {
                                                            if (response.isSuccessful) {
                                                                val responseBody: String? =
                                                                    response.body()
                                                                // Handle the string value as needed
                                                                println("Response body: $responseBody")
                                                            } else {
                                                                println("Response was not successful: ${response.code()}")
                                                            }
                                                        }

                                                        override fun onFailure(
                                                            call: Call<String>,
                                                            t: Throwable
                                                        ) {
                                                            println("Failed to execute request: ${t.message}")
                                                        }
                                                    })
                                                } catch (e: Exception) {
                                                    println("Exception :  ${e.message}")
                                                    EETLog.error(
                                                        AppContextHelper.appContext,
                                                        LogConstants.logDetails(
                                                            e,
                                                            LogConstants.LOG_LEVEL.ERROR.toString(),
                                                            LogConstants.LOG_SEVERITY.HIGH.toString()
                                                        ),
                                                        Constants.EX,
                                                        LogTraceConstants.getUtilityData(
                                                            AppContextHelper.appContext!!
                                                        )!!
                                                    )
                                                }


                                                //set attrtibute fetched from parent if blank in response
                                                if (conversation.attributes.Department.isNullOrEmpty() &&
                                                    conversation.attributes.Designation.isNullOrEmpty() &&
                                                    conversation.attributes.CustomerName.isNullOrEmpty()
                                                ) {

                                                    var customer = contact.customerName ?: ""
                                                    var department = contact.department ?: ""
                                                    var designation = contact.designation ?: ""
                                                    var role = contact.role ?: ""
                                                    var bpId = contact.bpId ?: ""

                                                    val attributes = mapOf(
                                                        "Designation" to designation,
                                                        "Department" to department,
                                                        "CustomerName" to customer,
                                                        "Role" to role,
                                                        "BpId" to bpId,
                                                        "isWebChat" to "false"
                                                    )
                                                    val jsonObject = JSONObject(attributes)

                                                    contactListViewModel.setAttributes(
                                                        conversation.conversationSid,
                                                        Attributes(jsonObject)
                                                    )
                                                }
                                                //Starting and redirecting to Existing conversation
                                                contactListViewModel.getSyncStatus(conversation.conversationSid)
                                                binding?.progressBarID?.visibility = View.GONE
                                            }
                                            //conversation doesnt exist, create new
                                            else {
                                                var customer = ""
                                                var department = ""
                                                var designation = ""
                                                var role = ""
                                                var bpId = ""

//                                                if (conversation.attributes.Department.isNullOrEmpty() &&
//                                                    conversation.attributes.Designation.isNullOrEmpty() &&
//                                                    conversation.attributes.CustomerName.isNullOrEmpty()
//                                                ) {
                                                    customer = contact.customerName ?: ""
                                                    department = contact.department ?: ""
                                                    designation = contact.designation ?: ""
                                                    role = contact.role ?: ""
                                                    bpId = contact.bpId ?: ""
//                                                } else {
//                                                    customer = conversation.attributes.CustomerName
//                                                    department = conversation.attributes.Department
//                                                    designation =
//                                                        conversation.attributes.Designation
//                                                }
                                                val attributes = mapOf(
                                                    "Designation" to designation,
                                                    "Department" to department,
                                                    "CustomerName" to customer,
                                                    "Role" to role,
                                                    "BpId" to bpId,
                                                    "isWebChat" to "false"
                                                )
                                                val jsonObject = JSONObject(attributes)
                                                Log.d("selected1",attributes.toString())
                                                contactListViewModel.createConversation(
                                                    contact.name + " " +
                                                            Constants.formatPhoneNumber(
                                                                applicationContext,
                                                                contact.number!!,
                                                                contact.countryCode
                                                            ),
                                                    " ",
                                                    "${
                                                        Constants.cleanedNumber(
                                                            Constants.formatPhoneNumber(
                                                                applicationContext,
                                                                contact.number!!,
                                                                contact.countryCode
                                                            )
                                                        )
                                                    }",
                                                    Attributes(jsonObject)
                                                )
                                                binding?.progressBarID?.visibility = View.GONE
                                            }

                                        }
                                    } else {
                                        try {
                                            //If there is no existing conversation with SMS user, create new
                                            //set attributes fetched from parent
                                            var customer = contact.customerName ?: ""
                                            var department = contact.department ?: ""
                                            var designation = contact.designation ?: ""
                                            var role = contact.role ?: ""
                                            var bpId = contact.bpId ?: ""

                                            Log.d("selected contact", contact.bpId + " " + contact.role + " " + contact.department + " " + contact.designation)

                                            val attributes = mapOf(
                                                "Designation" to designation,
                                                "Department" to department,
                                                "CustomerName" to customer,
                                                "Role" to role,
                                                "BpId" to bpId,
                                                "isWebChat" to "false"
                                            )
                                            val jsonObject = JSONObject(attributes)
                                            Log.d("selected",attributes.toString())
                                            contactListViewModel.createConversation(
                                                contact.name + " " + Constants.formatPhoneNumber(
                                                    applicationContext,
                                                    contact.number!!,
                                                    contact.countryCode
                                                ),
                                                contact.email,
                                                Constants.cleanedNumber(
                                                    Constants.formatPhoneNumber(
                                                        applicationContext,
                                                        contact.number!!,
                                                        contact.countryCode
                                                    )
                                                ),
                                                Attributes(jsonObject)
                                            )
                                            binding?.progressBarID?.visibility = View.GONE
                                        } catch (e: Exception) {
                                            println("Exception :  ${e.message}")
                                            EETLog.error(
                                                AppContextHelper.appContext, LogConstants.logDetails(
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
                                } else {
                                    binding?.progressBarID?.visibility = View.GONE
                                    binding!!.contactList.showSnackbar("Something went wrong")
                                    println("Response was not successful: ${response.code()}")
                                }
                            }

                            override fun onFailure(
                                call: Call<List<ParticipantExistingConversation>>,
                                t: Throwable
                            ) {
                                println("Failed to fetch existing conversations: ${t.message}")
                            }
                        })
//                    }
//                    else{
//                        binding!!.contactList .showSnackbar("Invalid Phone number.")
//                    }
                // enable interaction
                binding?.contactList?.isClickable = true
                binding?.contactList?.isEnabled = true
                ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_Contacts_ExternalContactClick",
                    "Contacts",
                    "ExternalFragment"
                )
            }

            override fun onContactItemLongClick(contact: ContactListViewItem) {
                showPopup(contact)
            }

            override fun onParticipantIconClick(contact: ContactListViewItem) {
                showPopup(contact)
            }

            private fun showPopup(contact: ContactListViewItem) {
                // Inflate the popup layout
                val inflater = layoutInflater
                val popupView = inflater.inflate(R.layout.popup_layout, null)
                val linearLayout = popupView.findViewById<LinearLayout>(R.id.rolesLinearLayout)
                val name = popupView.findViewById<TextView>(R.id.contactName)
                val department = popupView.findViewById<TextView>(R.id.department)
                val designation = popupView.findViewById<TextView>(R.id.designation)
                val customer = popupView.findViewById<TextView>(R.id.customer)
                val bpId = popupView.findViewById<TextView>(R.id.bpId)
                val number = popupView.findViewById<TextView>(R.id.number)
                val roleName = popupView.findViewById<TextView>(R.id.role)
                val roledividerline = popupView.findViewById<View>(R.id.roledividerline)
                val customerdividerline = popupView.findViewById<View>(R.id.customerdividerline)

                name.text = contact.name
                department.text = "(" + contact.department + ")"
                designation.text = contact.designation
                customer.text = contact.customerName
                bpId.text = contact.bpId
                if(contact.type == "SMS"){
                number.text = contact.number
                }else{
                    number.text = contact.email
                }

                if (contact.designation.isNullOrEmpty() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDesignation")!! == "false"){
                    designation.visibility = View.GONE
                }
                if (contact.department.isNullOrEmpty() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDepartment")!! == "false"){
                    department.visibility = View.GONE
                }
                if (contact.customerName.isNullOrEmpty()){
                    customer.visibility = View.GONE
                }
                if (contact.bpId.isNullOrEmpty()){
                    bpId.visibility = View.GONE
                }
                if (contact.number.isNullOrEmpty()){
                    number.visibility = View.GONE
                }
                if (contact.role.isNullOrEmpty()){
                    roleName.visibility = View.GONE
                    roledividerline.visibility = View.GONE
                }
                if(contact.customerName.isNullOrEmpty() && contact.bpId.isNullOrEmpty()){
                    customerdividerline.visibility = View.GONE
                }

                var itemsArray : Array<String>?
                if(contact.role.isNullOrEmpty()){
                    itemsArray = arrayOf()
                }else {
                    itemsArray = contact.role!!.split(",").toTypedArray()
                    for (item in itemsArray!!) {
                        if (!item.trim().isNullOrBlank()){
                        val textView = TextView(applicationContext).apply {
                            text = item.trim()
                            textSize = 16f
                            setTextColor(getResources().getColor(R.color.text_gray))
                            ellipsize = TextUtils.TruncateAt.MARQUEE
                            // Set additional properties if needed
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }
                        linearLayout.addView(textView)
                    }
                    }
                }

                // Create the PopupWindow
                val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                // Create a dimmed background view
                val dimBackground = View(applicationContext).apply {
                    setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    visibility = View.VISIBLE
                }

                // Get the root view
                val rootView = activity!!.window.decorView.findViewById<View>(android.R.id.content) as ViewGroup
                rootView.addView(dimBackground)
                // Close the popup when the button is clicked
                val closeButton: TextView = popupView.findViewById(R.id.close_button)
                closeButton.setOnClickListener {
                    popupWindow.dismiss()
                    rootView.removeView(dimBackground)
                }

                // Show the popup
                popupWindow.isFocusable = true
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

                // Set OnDismissListener to remove the dim background
                popupWindow.setOnDismissListener {
                    rootView.removeView(dimBackground)
                }

                // Optional: Dismiss popup when clicking on the dim background
                dimBackground.setOnClickListener {
                    popupWindow.dismiss()
                }
            }
        })

        //Providing Linearlayoutmanager to recyclerview
        binding?.contactList?.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        //Assigning the created adapter to recyclerview
        binding?.contactList?.adapter = adapter
        adapter.sizeChange.observe(viewLifecycleOwner,{size ->
            if (size > 0) {
                binding!!.progressBarID.visibility = View.GONE
            } else {
                binding!!.progressBarID.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding!!.progressBarID.visibility = View.GONE
                    if (isAdded){
                        if (Constants.getStringFromVitalTextSharedPreferences(
                                requireContext(),
                                "withContext"
                            ) == "true"
                        ) {
                            binding!!.noResultFound.root.visibility = View.VISIBLE
                        }
                }
                }, 5000)
            }
        })

        //Generate A-Z scrollbar
        val alphabet = ('A'..'Z').toList()

        // Clear previous views before adding new ones
        binding!!.azScrollbar.removeAllViews()

        for (letter in alphabet) {
            val textView = TextView(applicationContext).apply {
                text = letter.toString()
                setPadding(4, 2, 4, 2)
                isClickable = true
                isFocusable = true
                setTextColor(Color.LTGRAY)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    val position = adapter.getPositionForLetter(letter)
                    if (position != RecyclerView.NO_POSITION) {
                        binding!!.contactList.smoothScrollToPosition(position)
                        binding!!.contactList.post {
                            val layoutManager =  binding!!.contactList.layoutManager as LinearLayoutManager
                            val viewHolder =  binding!!.contactList.findViewHolderForAdapterPosition(position)

                            // If the view holder is not null, adjust the scroll
                            if (viewHolder != null) {
                                val itemView = viewHolder.itemView
                                val topOffset = itemView.top // Get the current top position

                                layoutManager.scrollToPositionWithOffset(
                                    position,
                                    0
                                ) // This scrolls it to the top
                                binding!!.contactList.scrollBy(
                                    0,
                                    topOffset
                                )
                            }
                        }

                        setTextColor(Color.RED)
                        // Reset the highlight after a short delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            setTextColor(Color.LTGRAY)
                        }, 300)
                    }
                }
            }
            binding!!.azScrollbar.addView(textView)
        }

        binding!!.azScrollbar.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                val y = event.y
                val height =  binding!!.azScrollbar.height
                val letterHeight = height / alphabet.size

                val index = (y / letterHeight).toInt()
                if (index in alphabet.indices) {
                    val letter = alphabet[index]
                    val position = adapter.getPositionForLetter(letter)
                    if (position != RecyclerView.NO_POSITION) {
                        binding!!.contactList.smoothScrollToPosition(position)
                        binding!!.contactList.post {
                            val layoutManager =  binding!!.contactList.layoutManager as LinearLayoutManager
                            val viewHolder =  binding!!.contactList.findViewHolderForAdapterPosition(position)

                            // If the view holder is not null, adjust the scroll
                            if (viewHolder != null) {
                                val itemView = viewHolder.itemView
                                val topOffset = itemView.top // Get the current top position

                                layoutManager.scrollToPositionWithOffset(
                                    position,
                                    0
                                ) // This scrolls it to the top
                                binding!!.contactList.scrollBy(
                                    0,
                                    topOffset
                                )
                            }
                        }
                    }
                }
            }
            true
        }

    }

    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }


//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_tab_one, container, false)
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreateView Called")
        binding = FragmentExternalBinding.inflate(inflater, container, false)
        return binding!!.root
    }
}