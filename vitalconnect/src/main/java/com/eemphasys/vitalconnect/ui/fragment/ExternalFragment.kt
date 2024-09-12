package com.eemphasys.vitalconnect.ui.fragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
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
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ContactListRequest
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchContactResponse
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
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

class ExternalFragment : Fragment() {
    var binding: FragmentExternalBinding? = null
    val contactListViewModel by lazyActivityViewModel { injector.createContactListViewModel(applicationContext) }
    private val noInternetSnackBar by lazy {
        Snackbar.make(binding!!.contactList, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }
    private var contactsList = arrayListOf<Contact>()
    private lateinit var adapter: ContactListAdapter
    private lateinit var originalList: List<ContactListViewItem>
    private var listOfContacts = mutableListOf<ContactListViewItem>()
    var currentIndex: Int = 1

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_contact_list, menu)

        val searchItem = menu.findItem(R.id.filter_contacts)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.length >= 3) {
                    if (Constants.WITH_CONTEXT == "false") {
                        //Search using SearchedUsers api
                        lifecycleScope.launch {
                            val listOfSearchedContacts = mutableListOf<ContactListViewItem>()
                            val httpClientWithToken = OkHttpClient.Builder()
                                .connectTimeout(300, TimeUnit.SECONDS)
                                .readTimeout(300, TimeUnit.SECONDS)
                                .writeTimeout(300, TimeUnit.SECONDS)
                                .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
                                .addInterceptor(RetryInterceptor())
                                .build()
                            val retrofitWithToken =
                                RetrofitHelper.getInstance(httpClientWithToken)
                                    .create(TwilioApi::class.java)
                            var request =
                                SearchContactRequest(
                                    Constants.USERNAME,
                                    Constants.TENANT_CODE,
                                    newText!!
                                )
                            var response = retrofitWithToken.getSearchedContact(request)

                            response.enqueue(object : Callback<List<SearchContactResponse>> {
                                override fun onResponse(
                                    call: Call<List<SearchContactResponse>>,
                                    response: Response<List<SearchContactResponse>>
                                ) {
                                    if (response.isSuccessful) {
                                        var contactsResponse: List<SearchContactResponse>? =
                                            response.body()
                                        if (!contactsResponse.isNullOrEmpty()) {

                                            for (response in contactsResponse) {
                                                var contactItem =
                                                    ContactListViewItem(
                                                        response.fullName,
                                                        "",
                                                        response.mobileNumber,
                                                        "SMS",
                                                        Constants.getInitials(response.fullName.trim { it <= ' ' }),
                                                        response.designation,
                                                        response.department,
                                                        response.customerName,
                                                        "",
                                                        true
                                                    )

                                                listOfSearchedContacts.add(contactItem)
                                            }
                                            setAdapter(listOfSearchedContacts)
                                        }
                                    }
                                }

                                override fun onFailure(
                                    call: Call<List<SearchContactResponse>>,
                                    t: Throwable
                                ) {

                                }

                            })

                        }
                    }
                    else{
                        setAdapter(originalList)
                    }
                    adapter.filter(newText.orEmpty())
                    return true
                }
                else{
                    if(Constants.WITH_CONTEXT == "false"){
                        setAdapter(listOfContacts)
                    }
                    else
                    {
                        setAdapter(originalList)
                    }
                    return false}
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

    fun getAllContactList(){
        lifecycleScope.launch {
        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken)
                .create(TwilioApi::class.java)
        var request = ContactListRequest(currentIndex,20,"","fullName","asc",Constants.TENANT_CODE,Constants.USERNAME,0)
        var response = retrofitWithToken.getContactList(request)

        if (response.isSuccessful) {
            var previousPosition = listOfContacts.size
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
                    true
                )
                listOfContacts.add(userItem)
            }
            adapter.notifyItemRangeInserted(previousPosition,listOfContacts.size)
        }
    }
    }
    private fun formatList(contacts: List<Contact>): List<ContactListViewItem> {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " combineLists Called")
        val combinedList = mutableListOf<ContactListViewItem>()

        // Convert Contact objects to ContactListViewItem
        val contactItems = contacts.map {
            ContactListViewItem(it.name, "", it.number, "SMS",it.initials,it.designation,it.department,it.customer,it.countryCode,false)
        }

        // Add all ContactListViewItem objects to the combined list
        combinedList.addAll(contactItems)

        return combinedList
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.contacts)
        binding?.contactList?.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

        contactListViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable)
                activity?.finish()
        }
        contactsList = ArrayList<Contact>()
        if(!Constants.CONTACTS.isNullOrEmpty()) {
            val jsonObjectcontacts = JSONObject(Constants.CONTACTS)
            val jsonArrayContacts = jsonObjectcontacts.getJSONArray("contacts")
            for (i in 0 until jsonArrayContacts.length()) {
                val jsonObject = jsonArrayContacts.getJSONObject(i)
                val name = jsonObject.getString("name")
                val number = Constants.formatPhoneNumber(jsonObject.getString("number"),jsonObject.getString("countryCode"))
                val customerName = jsonObject.getString("customerName")
                val initials = Constants.getInitials(name.trim { it <= ' ' })
                val designation = jsonObject.getString("designation")
                val department = jsonObject.getString("department")
                val customer = jsonObject.getString("customer")
                val countryCode = jsonObject.getString("countryCode")
                contactsList.add(Contact(name, number, customerName, initials, designation, department, customer,countryCode))
            }
        }
        if(Constants.WITH_CONTEXT == "false"){
            getAllContactList()
            setAdapter(listOfContacts)
        }
        else
        {
            originalList = formatList(contactsList)
            setAdapter(originalList)
        }

        if(Constants.WITH_CONTEXT == "false") {
            binding?.contactList!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (!recyclerView.canScrollVertically(1)) {
                            Log.d("scroll---up1", newState.toString())
                            currentIndex++
                            getAllContactList()
                        }
                    }
                }
            })
        }
    }

    private fun setAdapter(list : List<ContactListViewItem>){
        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)
        //Creating the Adapter
        adapter = ContactListAdapter(list,list,object : OnContactItemClickListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onContactItemClick(contact: ContactListViewItem) {
                var phone  = Constants.cleanedNumber(Constants.formatPhoneNumber(contact.number,contact.countryCode!!))
                    if(Constants.isValidPhoneNumber(phone, Locale.getDefault().country)) {
                        binding?.progressBarID?.visibility = View.VISIBLE

                        val existingConversation = retrofitWithToken.fetchExistingConversation(
                            Constants.TENANT_CODE,
                            Constants.cleanedNumber(
                                Constants.formatPhoneNumber(
                                    contact.number,
                                    contact.countryCode!!
                                )
                            ),
                            false,
                            1,
                            Constants.PROXY_NUMBER
                        )
                        Log.d(
                            "contactNumberClicked",
                            Constants.cleanedNumber(
                                Constants.formatPhoneNumber(
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
                                                        retrofitWithToken.addParticipantToConversation(
                                                            Constants.TENANT_CODE,
                                                            conversation.conversationSid,
                                                            Constants.USERNAME
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

                                                    val attributes = mapOf(
                                                        "Designation" to designation,
                                                        "Department" to department,
                                                        "CustomerName" to customer
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

                                                if (conversation.attributes.Department.isNullOrEmpty() &&
                                                    conversation.attributes.Designation.isNullOrEmpty() &&
                                                    conversation.attributes.CustomerName.isNullOrEmpty()
                                                ) {

                                                    customer = contact.customerName ?: ""
                                                    department = contact.department ?: ""
                                                    designation = contact.designation ?: ""
                                                } else {
                                                    customer = conversation.attributes.CustomerName
                                                    department = conversation.attributes.Department
                                                    designation =
                                                        conversation.attributes.Designation
                                                }
                                                val attributes = mapOf(
                                                    "Designation" to designation,
                                                    "Department" to department,
                                                    "CustomerName" to customer
                                                )
                                                val jsonObject = JSONObject(attributes)
                                                contactListViewModel.createConversation(
                                                    contact.name + " " +
                                                            Constants.formatPhoneNumber(
                                                                contact.number!!,
                                                                contact.countryCode
                                                            ),
                                                    " ",
                                                    "${
                                                        Constants.cleanedNumber(
                                                            Constants.formatPhoneNumber(
                                                                contact.number!!,
                                                                contact.countryCode
                                                            )
                                                        )
                                                    }",
                                                    Attributes(jsonObject)
                                                )
                                            }

                                        }
                                    } else {
                                        try {
                                            //If there is no existing conversation with SMS user, create new
                                            //set attributes fetched from parent
                                            var customer = contact.customerName ?: ""
                                            var department = contact.department ?: ""
                                            var designation = contact.designation ?: ""

                                            val attributes = mapOf(
                                                "Designation" to designation,
                                                "Department" to department,
                                                "CustomerName" to customer
                                            )
                                            val jsonObject = JSONObject(attributes)
                                            contactListViewModel.createConversation(
                                                contact.name + " " + Constants.formatPhoneNumber(
                                                    contact.number!!,
                                                    contact.countryCode
                                                ),
                                                contact.email,
                                                Constants.cleanedNumber(
                                                    Constants.formatPhoneNumber(
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
                    }
                    else{
                        binding!!.contactList .showSnackbar("Invalid Phone number.")
                    }
            }
        })

        //Providing Linearlayoutmanager to recyclerview
        binding?.contactList?.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        //Assigning the created adapter to recyclerview
        binding?.contactList?.adapter = adapter

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