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
import com.eemphasys.vitalconnect.api.data.ConversationSidFromFriendlyNameRequest
import com.eemphasys.vitalconnect.api.data.ConversationSidFromFriendlyNameResponse
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchContactResponse
import com.eemphasys.vitalconnect.api.data.SearchUsersResponse
import com.eemphasys.vitalconnect.api.data.UserListResponse
import com.eemphasys.vitalconnect.api.data.addParticipantToWebConversationRequest
import com.eemphasys.vitalconnect.api.data.webParticipant
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.data.models.WebUser
import com.eemphasys.vitalconnect.databinding.FragmentInternalBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
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

class InternalFragment : Fragment() {
    var binding: FragmentInternalBinding? = null
    val contactListViewModel by lazyActivityViewModel { injector.createContactListViewModel(applicationContext) }
    private val noInternetSnackBar by lazy {
        Snackbar.make(binding!!.userList, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }
    private var webuserList = arrayListOf<WebUser>()
    private lateinit var adapter: ContactListAdapter
    private lateinit var originalList: List<ContactListViewItem>
    private var listOfUsers = mutableListOf<ContactListViewItem>()
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
                            val listOfSearchedUsers = mutableListOf<ContactListViewItem>()
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
                            var response = retrofitWithToken.getSearchedUsers(request)

                            response.enqueue(object : Callback<List<SearchUsersResponse>> {
                                override fun onResponse(
                                    call: Call<List<SearchUsersResponse>>,
                                    response: Response<List<SearchUsersResponse>>
                                ) {
                                    if (response.isSuccessful) {
                                        var contactsResponse: List<SearchUsersResponse>? =
                                            response.body()
                                        if (!contactsResponse.isNullOrEmpty()) {

                                            for (response in contactsResponse) {
                                                var contactItem =
                                                    ContactListViewItem(
                                                        response.fullName,
                                                        response.userName,
                                                        response.mobileNumber,
                                                        "Web",
                                                        Constants.getInitials(response.fullName.trim { it <= ' ' }),
                                                        "",
                                                        response.department,
                                                        "",
                                                        "",
                                                        true
                                                    )

                                                listOfSearchedUsers.add(contactItem)
                                            }
                                            setAdapter(listOfSearchedUsers)
                                        }
                                    }
                                }

                                override fun onFailure(
                                    call: Call<List<SearchUsersResponse>>,
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
                        setAdapter(listOfUsers)
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
            binding?.userList?.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
        }
    }

    fun getAllUserList(){
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
        var response = retrofitWithToken.getUserList(request)

        response.enqueue(object : Callback<List<UserListResponse>>{
            override fun onResponse(
                call: Call<List<UserListResponse>>,
                response: Response<List<UserListResponse>>
            ) {
                if(response.isSuccessful){
                    var usersResponse: List<UserListResponse>? =
                        response.body()
                    if (!usersResponse.isNullOrEmpty()) {
                    var previousPosition = listOfUsers.size
                        for (response in usersResponse) {
                            var userItem = ContactListViewItem(
                                response.fullName,
                                response.userName,
                                response.mobileNumber,
                                "Web",
                                Constants.getInitials(response.fullName.trim { it <= ' ' }),
                                "",
                                response.department,
                                "",
                                "",
                                true
                                )
                            listOfUsers.add(userItem)
                        }
                        adapter.notifyItemRangeInserted(previousPosition,listOfUsers.size)
                    }
                }
            }

            override fun onFailure(call: Call<List<UserListResponse>>, t: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun formatList(webUsers: List<WebUser>): List<ContactListViewItem> {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " combineLists Called")
        val formattedList = mutableListOf<ContactListViewItem>()

        // Convert WebUser objects to ContactListViewItem
        val webUserItems = webUsers.map {
            ContactListViewItem(it.name, it.userName, "", "Chat",it.initials,it.designation,it.department,it.customer,it.countryCode,false)
        }

        // Add all ContactListViewItem objects to the combined list
        formattedList.addAll(webUserItems)

        return formattedList
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.contacts)
        binding?.userList?.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

        contactListViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable)
                activity?.finish()
        }
        webuserList = ArrayList<WebUser>()

        if(!Constants.WEBUSERS.isNullOrEmpty()) {
            val jsonObjectwebusers = JSONObject(Constants.WEBUSERS)
            val jsonArrayWebUsers = jsonObjectwebusers.getJSONArray("webUser")
            for (i in 0 until jsonArrayWebUsers.length()) {
                val jsonObject = jsonArrayWebUsers.getJSONObject(i)
                val name = jsonObject.getString("name")
                val userName = jsonObject.getString("userName")
                val initials = Constants.getInitials(name.trim { it <= ' ' })
                val designation = jsonObject.getString("designation")
                val department = jsonObject.getString("department")
                val customer = jsonObject.getString("customer")
                val countryCode = jsonObject.getString("countryCode")
                webuserList.add(WebUser(name, userName, initials, designation, department, customer,countryCode))
            }
        }

        if(Constants.WITH_CONTEXT == "false"){
            getAllUserList()
            setAdapter(listOfUsers)
        }
        else
            {
                originalList = formatList(webuserList)
                setAdapter(originalList)
            }

        if(Constants.WITH_CONTEXT == "false") {
            binding?.userList!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (!recyclerView.canScrollVertically(1)) {
                            Log.d("scroll---up1", newState.toString())
                            currentIndex++
                            getAllUserList()
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
                if (contact.type == "SMS") {
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

                                                    var customer = contact.customerName
                                                    var department = contact.department
                                                    var designation = contact.designation

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

                                                    customer = contact.customerName!!
                                                    department = contact.department!!
                                                    designation = contact.designation!!
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
                                                    "$contact.name ${
                                                        Constants.formatPhoneNumber(
                                                            contact.number!!,
                                                            contact.countryCode
                                                        )
                                                    }",
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
                                            var customer = contact.customerName
                                            var department = contact.department
                                            var designation = contact.designation

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
                        binding!!.userList .showSnackbar("Invalid Phone number.")
                    }

                } else {
//                  Web to web chat
                    binding?.progressBarID?.visibility = View.VISIBLE

//                  Check if existing coversation exist
                    contactListViewModel.checkExistingconversation(contact)

                    binding?.progressBarID?.visibility = View.GONE
                }
            }
        })

        //Providing Linearlayoutmanager to recyclerview
        binding?.userList?.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        //Assigning the created adapter to recyclerview
        binding?.userList?.adapter = adapter
//        if (binding!!.userList.adapter!!.itemCount < 1){
//            binding!!.noResultFound.root.visibility = View.VISIBLE
//        }
//        else
//            binding!!.noResultFound.root.visibility = View.GONE

    }

    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreateView Called")
        binding = FragmentInternalBinding.inflate(inflater, container, false)
        return binding!!.root
    }
}