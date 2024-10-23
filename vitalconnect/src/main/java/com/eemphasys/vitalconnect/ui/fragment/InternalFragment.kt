package com.eemphasys.vitalconnect.ui.fragment
import android.annotation.SuppressLint
import android.graphics.Color
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
import androidx.fragment.app.findFragment
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
import com.eemphasys.vitalconnect.ui.dialogs.NewConversationDialog
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.snackbar.Snackbar
import com.twilio.conversations.Attributes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

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
    var maxPageSize: Int = 1

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_contact_list, menu)

        val searchItem = menu.findItem(R.id.filter_contacts)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(isAdded){
                if (newText != null && newText.length >= 3) {
                    if (Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false") {
                        //Search using SearchedUsers api
                        lifecycleScope.launch {
                            val listOfSearchedUsers = mutableListOf<ContactListViewItem>()
                            var request =
                                SearchContactRequest(
                                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,
                                    Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                                    newText!!
                                )
                            var response = RetrofitClient.retrofitWithToken.getSearchedUsers(request)

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
                                                        true,
                                                        "",
                                                        ""
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
                    if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false"){
                        setAdapter(listOfUsers)
                    }
                    else
                    {
                        setAdapter(originalList)
                    }
                    return false}
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
            binding?.userList?.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
            binding!!.progressBarID.visibility = View.VISIBLE
        }
    }

    fun getAllUserList(callBack: () -> Unit){
        var request = ContactListRequest(currentIndex,Constants.PAGE_SIZE.toInt(),"","fullName","asc",Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,0)
        var response = RetrofitClient.retrofitWithToken.getUserList(request)

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
                                true,
                                "",
                                ""
                                )
                            listOfUsers.add(userItem)
                            maxPageSize = ceil((response.totalCount/Constants.PAGE_SIZE)).toInt()
                        }
                        adapter.notifyItemRangeInserted(previousPosition,listOfUsers.size)
                    }
                }
                callBack.invoke()
            }

            override fun onFailure(call: Call<List<UserListResponse>>, t: Throwable) {
                callBack.invoke()
            }

        })
    }

    private fun formatList(webUsers: List<WebUser>): List<ContactListViewItem> {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " combineLists Called")
        val formattedList = mutableListOf<ContactListViewItem>()

        // Convert WebUser objects to ContactListViewItem
        val webUserItems = webUsers.map {
            ContactListViewItem(it.name, it.userName, "", "Chat",it.initials,it.designation,it.department,it.customer,it.countryCode,false,"",it.role)
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

        if(!Constants.getStringFromVitalTextSharedPreferences(applicationContext,"webUsers")!!.isNullOrEmpty()) {
            val jsonObjectwebusers = JSONObject(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"webUsers")!!)
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
                val role = jsonObject.getString("role")
                webuserList.add(WebUser(name, userName, initials, designation, department, customer,countryCode,role))
            }
        }

        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false"){
            getAllUserList{}
            setAdapter(listOfUsers)
        }
        else
            {
                originalList = formatList(webuserList)
                setAdapter(originalList)
            }

        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"withContext")!! == "false") {
            binding?.userList!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (!recyclerView.canScrollVertically(1)) {
                            Log.d("scroll---up1", newState.toString())
                            currentIndex++
                            Log.d("maxPagesize",maxPageSize.toString())
                            if(maxPageSize>=currentIndex) {
                                binding!!.progressBarRecyclerview.visibility = View.VISIBLE
                            }
                            getAllUserList{
                                binding!!.progressBarRecyclerview.visibility = View.GONE
                            }
                        }
                    }
                }
            })
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setAdapter(list : List<ContactListViewItem>){
        //Creating the Adapter
        adapter = ContactListAdapter(list,list,applicationContext,object : OnContactItemClickListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onContactItemClick(contact: ContactListViewItem) {
                Log.d(
                    "WebUserClicked",contact.name)
//                  Web to web chat
                    binding?.progressBarID?.visibility = View.VISIBLE
                if(!contact.email.isNullOrEmpty()) {
                    if (Constants.getStringFromVitalTextSharedPreferences(
                            applicationContext,
                            "withContext"
                        )!!.lowercase() == "true"
                    ) {
//                  Check if existing coversation exist
                        if (Constants.getStringFromVitalTextSharedPreferences(
                                applicationContext,
                                "isAutoRegistrationEnabled"
                            )!!.lowercase() == "false"
                        ) {
                            contactListViewModel.checkTwilioUser(contact) { isTwilioUser ->
                                if (isTwilioUser) {
                                    contactListViewModel.checkExistingconversation(contact)
                                } else {
                                    //need to show error here
                                    binding?.userList?.showSnackbar(
                                        "User not found."
                                    )
                                }
                            }
                        } else {
                            contactListViewModel.checkExistingconversation(contact)
                        }


                        binding?.progressBarID?.visibility = View.GONE
                    } else {
                        Constants.CURRENT_CONTACT = contact
                        contactListViewModel.checkTwilioUser(contact) { isTwilioUser ->
                            if (isTwilioUser) {
                                binding?.progressBarID?.visibility = View.GONE
                                NewConversationDialog().showNow(childFragmentManager, null)
                            } else {
                                //need to show error here
                                binding?.userList?.showSnackbar(
                                    getString(R.string.user_not_registered)
                                )
                            }
                        }

                    }
                }else {
                //email id is not configured or blank
                    binding?.progressBarID?.visibility = View.GONE
                    binding?.userList?.showSnackbar(
                            getString(R.string.empty_username)
                    )
                }
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
//                if (contact.number.isNullOrEmpty()){
//                    number.visibility = View.GONE
//                }
                if (contact.role.isNullOrEmpty()){
                    roleName.visibility = View.GONE
                    roledividerline.visibility = View.GONE
                }
//                if (contact.number.isNullOrEmpty()){
//                    number.visibility = View.GONE
//                }
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
        binding?.userList?.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL,false)
        //Assigning the created adapter to recyclerview
        binding?.userList?.adapter = adapter
        adapter.sizeChange.observe(viewLifecycleOwner,{size ->
            if (size > 0) {
                binding!!.progressBarID.visibility = View.GONE
            } else {
                binding!!.progressBarID.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding!!.progressBarID.visibility = View.GONE
                    if (isAdded) {
                        if (Constants.getStringFromVitalTextSharedPreferences(
                                requireContext(),
                                "withContext"
                            ) == "true"
                        )
                            binding!!.noResultFound.root.visibility = View.VISIBLE
                    }
                }, 5000)
            }
        })

        //Generate A-Z scrollbar
        val alphabet = ('A'..'Z').toList()
        for (letter in alphabet) {
            val textView = TextView(applicationContext).apply {
                text = letter.toString()
                setPadding(4, 4, 4, 4)
                isClickable = true
                isFocusable = true
                setTextColor(Color.LTGRAY)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    val position = adapter.getPositionForLetter(letter)
                    if (position != RecyclerView.NO_POSITION) {
                        binding!!.userList.smoothScrollToPosition(position)
                        binding!!.userList.post {
                            val layoutManager =  binding!!.userList.layoutManager as LinearLayoutManager
                            val viewHolder =  binding!!.userList.findViewHolderForAdapterPosition(position)

                            // If the view holder is not null, adjust the scroll
                            if (viewHolder != null) {
                                val itemView = viewHolder.itemView
                                val topOffset = itemView.top // Get the current top position

                                layoutManager.scrollToPositionWithOffset(
                                    position,
                                    0
                                ) // This scrolls it to the top
                                binding!!.userList.scrollBy(
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
                        binding!!.userList.smoothScrollToPosition(position)
                        binding!!.userList.post {
                            val layoutManager =  binding!!.userList.layoutManager as LinearLayoutManager
                            val viewHolder =  binding!!.userList.findViewHolderForAdapterPosition(position)

                            // If the view holder is not null, adjust the scroll
                            if (viewHolder != null) {
                                val itemView = viewHolder.itemView
                                val topOffset = itemView.top // Get the current top position

                                layoutManager.scrollToPositionWithOffset(
                                    position,
                                    0
                                ) // This scrolls it to the top
                                binding!!.userList.scrollBy(
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