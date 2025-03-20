package com.eemphasys.vitalconnect.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.SuggestionAdapter
import com.eemphasys.vitalconnect.api.RetrofitClient
import com.eemphasys.vitalconnect.api.data.AddAzureAdParticipantConversationRequest
import com.eemphasys.vitalconnect.api.data.GetAzureADUserAndGroupListRequest
import com.eemphasys.vitalconnect.api.data.GetAzureADUserAndGroupListResponse
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchUsersResponse
import com.eemphasys.vitalconnect.api.data.webParticipant
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SheetListener
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.hide
import com.eemphasys.vitalconnect.common.extensions.hideKeyboard
import com.eemphasys.vitalconnect.common.extensions.isShowing
import com.eemphasys.vitalconnect.common.extensions.lazyViewModel
import com.eemphasys.vitalconnect.common.extensions.show
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.extensions.showToast
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.ActivityConversationDetailsBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConversationDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationDetailsBinding
    private val renameConversationSheetBehavior by lazy { BottomSheetBehavior.from(binding.renameConversationSheet.root) }
    private val addChatParticipantSheetBehavior by lazy { BottomSheetBehavior.from(binding.addChatParticipantSheet.root) }
    private val addNonChatParticipantSheetBehavior by lazy { BottomSheetBehavior.from(binding.addNonChatParticipantSheet.root) }
    private val sheetListener by lazy { SheetListener(binding.sheetBackground) { hideKeyboard() } }
    private val progressDialog: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(R.layout.view_loading_dialog)
            .create()
    }
    val conversationDetailsViewModel by lazyViewModel {
        injector.createConversationDetailsViewModel(applicationContext, intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
    }
    val messageListViewModel by lazyViewModel {
        injector.createMessageListViewModel(applicationContext, intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
    }
    val conversationListViewModel by lazyViewModel { injector.createConversationListViewModel(applicationContext) }
    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.conversationDetailsLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }

    private lateinit var editText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SuggestionAdapter
//    private var isManualTextChange = false

    private val _isManualTextChange = MutableLiveData<Boolean>()
    val isManualTextChange: LiveData<Boolean> get() = _isManualTextChange
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        binding = DataBindingUtil
            .setContentView<ActivityConversationDetailsBinding>(this, R.layout.activity_conversation_details)
            .apply {
                lifecycleOwner = this@ConversationDetailsActivity
            }

        binding.viewModel = conversationDetailsViewModel
        binding.lifecycleOwner = this

        initViews()

//        // Initialize the LiveData with a default value
//        _isManualTextChange.value = false

//        isManualTextChange.observe(this) { isChanged ->
//            if (isChanged) {
//                binding.addChatParticipantSheet.addChatParticipantIdButton.isEnabled = isChanged
//            } else {
//                binding.addChatParticipantSheet.addChatParticipantIdButton.isEnabled = isChanged
//            }
//        }

        if (Constants.getStringFromVitalTextSharedPreferences(
                applicationContext,
                "isAzureAdEnabled"
            )!!.equals("true", ignoreCase = true)
        ) {
            binding.addChatParticipantSheet.addParticpantLabel.text =
                getString(R.string.details_add_azure_participant)
            binding.addChatParticipantButton.text = getString(R.string.details_add_azure_participant)
        }
        else {
            binding.addChatParticipantSheet.addParticpantLabel.text =
                getString(R.string.details_add_chat_participant)
            binding.addChatParticipantButton.text = getString(R.string.details_add_chat_participant)
        }

        editText = findViewById(R.id.add_chat_participant_id_input)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.layoutManager = LinearLayoutManager(this)

        editText.addTextChangedListener(object : TextWatcher {
            var listOfSearchedUsers = mutableListOf<ContactListViewItem>()
            var lastSearchText = ""  // Keep track of the last search text
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (_isManualTextChange.value == true) {
                    // Don't trigger search logic if the change is manual
                    return
                }
                val text = s.toString().orEmpty()
                Log.d("setadapter", text.length.toString())
                lastSearchText = text // Saving the text before making the API call
                if( text.length >= 3) {
                    binding.addChatParticipantSheet.progressBarID.visibility = View.VISIBLE
                    if (Constants.getStringFromVitalTextSharedPreferences(
                            applicationContext,
                            "isAzureAdEnabled"
                        )!!.equals("true", ignoreCase = true)
                    ) {
                        val request = GetAzureADUserAndGroupListRequest(
                            Constants.getStringFromVitalTextSharedPreferences(
                                applicationContext,
                                "tenantCode"
                            )!!,
                            Constants.getStringFromVitalTextSharedPreferences(
                                applicationContext,
                                "currentUser"
                            )!!,
                            text
                        )
                        val apiResponse =
                            RetrofitClient.getRetrofitWithToken().getAzureADUserAndGroupList(request)
                        apiResponse.enqueue(object : Callback<List<GetAzureADUserAndGroupListResponse>> {
                            override fun onResponse(
                                call: Call<List<GetAzureADUserAndGroupListResponse>>,
                                response: Response<List<GetAzureADUserAndGroupListResponse>>
                            ) {
                                if (response.isSuccessful && text == lastSearchText) {
                                    val contactsResponse: List<GetAzureADUserAndGroupListResponse>? =
                                        response.body()
                                    if (!contactsResponse.isNullOrEmpty()) {
                                        listOfSearchedUsers.clear()
                                        for (res in contactsResponse) {
                                            val contactItem =
                                                ContactListViewItem(
                                                    res.fullName,
                                                    if(res.isGroup) "" else res.userName,
                                                    "",
                                                    "Web",
                                                    Constants.getInitials(res.fullName.trim { it <= ' ' }),
                                                    "",
                                                    if(res.isGroup) "Group" else "",
                                                    "",
                                                    "",
                                                    true,
                                                    "",
                                                    "",
                                                    res.isGroup,
                                                    res.objectId
                                                )

                                            listOfSearchedUsers.add(contactItem)
                                        }
                                        setAdapter(listOfSearchedUsers)
                                        binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                    }
                                    else{
                                        binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                    }
                                }
                                else{
                                    binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                }
                            }

                            override fun onFailure(
                                call: Call<List<GetAzureADUserAndGroupListResponse>>,
                                t: Throwable
                            ) {
                                binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                            }

                        })
                    } else {
                        lifecycleScope.launch {
                            val request =
                                SearchContactRequest(
                                    Constants.getStringFromVitalTextSharedPreferences(
                                        applicationContext,
                                        "currentUser"
                                    )!!,
                                    Constants.getStringFromVitalTextSharedPreferences(
                                        applicationContext,
                                        "tenantCode"
                                    )!!,
                                    text
                                )
                            val apiResponse =
                                RetrofitClient.getRetrofitWithToken().getSearchedUsers(request)

                            apiResponse.enqueue(object : Callback<List<SearchUsersResponse>> {
                                override fun onResponse(
                                    call: Call<List<SearchUsersResponse>>,
                                    response: Response<List<SearchUsersResponse>>
                                ) {
                                    if (response.isSuccessful && text == lastSearchText) {
                                        val contactsResponse: List<SearchUsersResponse>? =
                                            response.body()
                                        if (!contactsResponse.isNullOrEmpty()) {
                                            listOfSearchedUsers.clear()
                                            for (res in contactsResponse) {
                                                val contactItem =
                                                    ContactListViewItem(
                                                        res.fullName,
                                                        res.userName,
                                                        res.mobileNumber,
                                                        "Web",
                                                        Constants.getInitials(res.fullName.trim { it <= ' ' }),
                                                        "",
                                                        res.department,
                                                        "",
                                                        "",
                                                        true,
                                                        "",
                                                        ""
                                                    )

                                                listOfSearchedUsers.add(contactItem)
                                            }
                                            setAdapter(listOfSearchedUsers)
                                            binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                        }
                                        else{
                                            binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                        }
                                    }
                                    else{
                                        binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                    }
                                }

                                override fun onFailure(
                                    call: Call<List<SearchUsersResponse>>,
                                    t: Throwable
                                ) {
                                    binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                                }

                            })

                        }
                    }
                }
                else{
                    listOfSearchedUsers.clear()
                    setAdapter(emptyList())
                    binding.addChatParticipantSheet.progressBarID.visibility = View.GONE
                    binding.addChatParticipantSheet.recyclerView.visibility = View.GONE // Hide RecyclerView
                }
                // Disable the button if user is typing after the suggestion click
                binding.addChatParticipantSheet.addChatParticipantIdButton.isEnabled = false
                binding.addChatParticipantSheet.identity.text = ""
                Constants.CURRENT_CONTACT = ContactListViewItem(
                    name = "",
                    email = "",
                    number = "",
                    type = "",
                    initials = "",
                    designation = null,
                    department = null,
                    customerName = null,
                    countryCode = null,
                    isGlobal = false,
                    "",
                    "",
                    isGroup = null,
                    objectId = null
                )

            }

            override fun afterTextChanged(s: Editable?) {
                val text = s
                if (text.isNullOrEmpty() || text.length < 3) {
                    listOfSearchedUsers.clear()
                    setAdapter(emptyList())  // Clear the RecyclerView data when text is less than 3 or empty
                    binding.addChatParticipantSheet.recyclerView.visibility = View.GONE // Hide RecyclerView
                }}
        })
    }

    private fun onSuggestionClick(suggestion: ContactListViewItem) {
        _isManualTextChange.value = true
        binding.addChatParticipantSheet.recyclerView.visibility = View.GONE
        binding.addChatParticipantSheet.addChatParticipantIdButton.isEnabled = true
        editText.setText(suggestion.name)
        if (Constants.getStringFromVitalTextSharedPreferences(
                applicationContext,
                "isAzureAdEnabled"
            )!!.equals("true", ignoreCase = true)
        ) {
            Constants.CURRENT_CONTACT = suggestion
        }else {
            binding.addChatParticipantSheet.identity.text = suggestion.email
        }

//        editText.setSelection(suggestion.name.length)
        adapter= SuggestionAdapter(applicationContext,emptyList()){}
//        adapter.notifyDataSetChanged()
        _isManualTextChange.value = false
    }

    fun setAdapter(list : List<ContactListViewItem>){
        Log.d("setadapter",list.toString()
        )
        adapter = SuggestionAdapter(applicationContext,list) { suggestion ->
            onSuggestionClick(suggestion)
        }
        recyclerView.adapter = adapter
        if(recyclerView.adapter?.itemCount!! > 0) recyclerView.visibility = View.VISIBLE else recyclerView.visibility = View.GONE
    }

    override fun onStart() {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onStart Called")
        super.onStart()
        try {
            binding.progressBarID.visibility= View.VISIBLE
            if(ConversationsClientWrapper.INSTANCE.isClientCreated){
                binding.progressBarID.visibility= View.GONE
            }
            else{
                Log.d("onStart ConversationDetailsActivity","onStart called")
                lifecycleScope.launch {
                    ConversationsClientWrapper.INSTANCE.getclient(applicationContext)
                    ConversationsRepositoryImpl.INSTANCE.subscribeToConversationsClientEvents()
                    delay(3000)
                    binding.progressBarID.visibility= View.GONE
                }
            }
        }catch(e: Exception){
            Log.d("onStart ConversationDetailsActivity", e.message.toString())
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
        if (renameConversationSheetBehavior.isShowing()) {
            renameConversationSheetBehavior.hide()
            return
        }
        if (addChatParticipantSheetBehavior.isShowing()) {
            addChatParticipantSheetBehavior.hide()
            return
        }
        if (addNonChatParticipantSheetBehavior.isShowing()) {
            addNonChatParticipantSheetBehavior.hide()
            return
        }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        ChatAppModel.FirebaseLogEventListener?.screenLogEvent(this,"VC_ConversationDetails","ConversationDetailsActivity")
    }
    private fun initViews() {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " initViews Called")
        setSupportActionBar(binding.conversationDetailsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.conversationDetailsToolbar.setNavigationIcon(R.drawable.back_button)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back_button)
        binding.conversationDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
        renameConversationSheetBehavior.addBottomSheetCallback(sheetListener)
        addChatParticipantSheetBehavior.addBottomSheetCallback(sheetListener)
        addNonChatParticipantSheetBehavior.addBottomSheetCallback(sheetListener)
        title = getString(R.string.details_title)

        if(Constants.getStringFromVitalTextSharedPreferences(this,"showContacts")!!.lowercase() == "true" && Constants.getStringFromVitalTextSharedPreferences(this,"isStandalone")!!.lowercase() == "false" && Constants.getStringFromVitalTextSharedPreferences(this,"showConversations")!!.lowercase() == "false") {
            binding.conversationLeaveButton.visibility = View.GONE
        }


        binding.addChatParticipantButton.setOnClickListener {
            binding.addChatParticipantSheet.addChatParticipantIdInput.text?.clear()
            addChatParticipantSheetBehavior.show()
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_ConversationDetails_AddParticipantClick",
                "ConversationDetails",
                "ConversationDetailsActivity"
            )
        }

        binding.addNonChatParticipantButton.setOnClickListener {
            binding.addNonChatParticipantSheet.addNonChatParticipantPhoneInput.text?.clear()
            binding.addNonChatParticipantSheet.addNonChatParticipantProxyInput.text?.clear()
            addNonChatParticipantSheetBehavior.show()
        }

        binding.participantsListButton.setOnClickListener {
            ParticipantListActivity.start(this, conversationDetailsViewModel.conversationSid)
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_ConversationDetails_ParticipantListClick",
                "ConversationDetails",
                "ConversationDetailsActivity"
            )
        }

        binding.conversationRenameButton.setOnClickListener {
            renameConversationSheetBehavior.show()
        }

        binding.conversationMuteButton.setOnClickListener {
            if (conversationDetailsViewModel.isConversationMuted()) {
                conversationDetailsViewModel.unmuteConversation()
            } else {
                conversationDetailsViewModel.muteConversation()
            }
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_ConversationDetails_MuteUnmuteClick",
                "ConversationDetails",
                "ConversationDetailsActivity"
            )
        }

        conversationDetailsViewModel.isPinned.observe(this, Observer { isPinned ->
            if(isPinned){
            binding.conversationDetailsLayout.showSnackbar(
                getString(
                    R.string.conversation_pinned,
                )
            )
            }else{
                binding.conversationDetailsLayout.showSnackbar(
                    getString(
                        R.string.conversation_unpinned,
                    )
                )
            }
            updatePinButtonUI(isPinned)

            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_ConversationDetails_PinUnpinClick",
                "ConversationDetails",
                "ConversationDetailsActivity"
            )
        })

        // Initial UI setup
        initPinButtonUI(conversationDetailsViewModel.isPinned.value ?: false)

        binding.conversationLeaveButton.setOnClickListener {
            conversationDetailsViewModel.leaveConversation()
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_ConversationDetails_LeaveConversationClick",
                "ConversationDetails",
                "ConversationDetailsActivity"
            )
        }

        binding.sheetBackground.setOnClickListener {
            renameConversationSheetBehavior.hide()
            addChatParticipantSheetBehavior.hide()
            addNonChatParticipantSheetBehavior.hide()
        }

        binding.renameConversationSheet.renameConversationCancelButton.setOnClickListener {
            renameConversationSheetBehavior.hide()
        }

        binding.renameConversationSheet.renameConversationButton.setOnClickListener {
            renameConversationSheetBehavior.hide()
            conversationDetailsViewModel.renameConversation(binding.renameConversationSheet.renameConversationInput.text.toString())
        }

        binding.addChatParticipantSheet.addChatParticipantIdCancelButton.setOnClickListener {
            addChatParticipantSheetBehavior.hide()
        }
//add chat participant through this button
        binding.addChatParticipantSheet.addChatParticipantIdButton.setOnClickListener {
            addChatParticipantSheetBehavior.hide()
            if (Constants.getStringFromVitalTextSharedPreferences(
                    applicationContext,
                    "isAzureAdEnabled"
                )!!.equals("true", ignoreCase = true)
            ) {
                conversationDetailsViewModel.addAzureAdParticipant(binding.details)
            }
            else {
                if (adapter.itemCount == 0) {
                    conversationDetailsViewModel.addChatParticipant("")
                } else {
                    conversationDetailsViewModel.addChatParticipant(binding.addChatParticipantSheet.identity.text.toString())
                }
            }
        }

        binding.addNonChatParticipantSheet.addNonChatParticipantIdCancelButton.setOnClickListener {
            addNonChatParticipantSheetBehavior.hide()
        }

        binding.addNonChatParticipantSheet.addNonChatParticipantIdButton.setOnClickListener {
            addNonChatParticipantSheetBehavior.hide()
            conversationDetailsViewModel.addNonChatParticipant(
                binding.addNonChatParticipantSheet.addNonChatParticipantPhoneInput.text.toString(),
                binding.addNonChatParticipantSheet.addNonChatParticipantProxyInput.text.toString(),
            )
        }

        conversationDetailsViewModel.isShowProgress.observe(this) { show ->
            if (show) {
                progressDialog.show()
            } else {
//                progressDialog.hide()
                progressDialog.dismiss()
            }
        }

        conversationDetailsViewModel.onConversationMuted.observe(this) { show ->
            if (show) {
                binding.conversationDetailsLayout.showSnackbar(
                    getString(
                        R.string.conversation_muted,
                    )
                )
            } else {
                binding.conversationDetailsLayout.showSnackbar(
                    getString(
                        R.string.conversation_unmuted,
                    )
                )
            }
        }

        messageListViewModel.isWebChat.observe(this){ isWebChat ->
            Constants.saveStringToVitalTextSharedPreferences(applicationContext,"isWebChat",isWebChat)
//            if(isWebChat.toLowerCase() == "true") {
//                binding.addChatParticipantButton.visibility = View.VISIBLE
//            }
//            else{
//                binding.addChatParticipantButton.visibility = View.GONE
//            }
        }

        conversationDetailsViewModel.conversationDetails.observe(this) { conversationDetails ->
            binding.details = conversationDetails
            binding.renameConversationSheet.renameConversationInput.setText(conversationDetails.conversationName)
        }

        conversationDetailsViewModel.onDetailsError.observe(this) { error ->
            if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                showToast(R.string.err_failed_to_get_conversation)
                finish()
            }
            binding.conversationDetailsLayout.showSnackbar(getErrorMessage(error))
        }

        conversationDetailsViewModel.onConversationLeft.observe(this) {
//            ConversationListActivity.start(this)
            binding.progressBarID.visibility = View.VISIBLE
            binding.conversationDetailsLayout.showSnackbar(getString(R.string.conversation_left))
            conversationListViewModel.savePinnedConversationToDB {
                ConversationListActivity.start(this, 2)
                finish()
                binding.progressBarID.visibility = View.GONE
            }
        }

        conversationDetailsViewModel.onParticipantAdded.observe(this) { identity ->
            if(identity.equals("failed")){
                binding.conversationDetailsLayout.showSnackbar(
                    getString(
                        R.string.failed_to_add_participant
                    )
                )
            }
            else
            binding.conversationDetailsLayout.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
        }
        conversationDetailsViewModel.isNetworkAvailable.observe(this) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable){
                Constants.showPopup(layoutInflater, this)
                var layout = layoutInflater.inflate(R.layout.activity_conversation_details, null)
                var text = layout.findViewById<TextView>(R.id.textBox)
                text.text = "offline"
                text.setBackgroundColor(ContextCompat.getColor(this, R.color.text_gray))
            }
//                this.finish()
        }
    }

    private fun updatePinButtonUI(isPinned: Boolean) {
        binding.conversationPinButton.apply {
            val type = object : TypeToken<ArrayList<String>>() {}.type
//            var jsonString = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"pinnedConvo")!!
//            var pinnedConvo : ArrayList<String> = Gson().fromJson(jsonString, type)
            if(isPinned){
//                Constants.PINNED_CONVO.add(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
                var added = Constants.addToPinnedConvo(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
//                pinnedConvo.add(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
                if (added) {
                    text = context.getString(R.string.details_unpin_conversation)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_unpin,0,0,0)
                    conversationListViewModel.savePinnedConversationToDB {}
                    conversationDetailsViewModel.conversationDetails.value?.isPinned = true
                }
                else{
                    binding.conversationDetailsLayout.showSnackbar(getString(R.string.pin_limit))
                }
            }else{
                text = context.getString(R.string.details_pin_conversation)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin,0,0,0)
                Constants.PINNED_CONVO.remove(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
//                pinnedConvo.remove(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
                conversationListViewModel.savePinnedConversationToDB{}
                conversationDetailsViewModel.conversationDetails.value?.isPinned = false
            }
        }
    }
    private fun initPinButtonUI(isPinned: Boolean) {
        binding.conversationPinButton.apply {
            text = if (isPinned) {
                context.getString(R.string.details_unpin_conversation)
            } else {
                context.getString(R.string.details_pin_conversation)
            }
            setCompoundDrawablesWithIntrinsicBounds(
                if (isPinned) R.drawable.icon_unpin else R.drawable.ic_pin,
                0,
                0,
                0
            )
        }
    }
    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

        fun start(context: Context, conversationSid: String) =
            context.startActivity(getStartIntent(context, conversationSid))

        fun getStartIntent(context: Context, conversationSid: String) =
            Intent(context, ConversationDetailsActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)
    }
}
