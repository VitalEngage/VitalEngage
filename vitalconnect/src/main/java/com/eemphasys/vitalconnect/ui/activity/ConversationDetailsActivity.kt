package com.eemphasys.vitalconnect.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.adapters.SuggestionAdapter
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchUsersResponse
import com.eemphasys.vitalconnect.common.SheetListener
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.*
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.ActivityConversationDetailsBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.repository.ConversationsRepositoryImpl
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        binding = DataBindingUtil
            .setContentView<ActivityConversationDetailsBinding>(this, R.layout.activity_conversation_details)
            .apply {
                lifecycleOwner = this@ConversationDetailsActivity
            }

        initViews()

        editText = findViewById(R.id.add_chat_participant_id_input)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.layoutManager = LinearLayoutManager(this)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s!!.length >= 3){
                lifecycleScope.launch {
                    val listOfSearchedUsers = mutableListOf<ContactListViewItem>()
                    val httpClientWithToken = OkHttpClient.Builder()
                        .connectTimeout(300, TimeUnit.SECONDS)
                        .readTimeout(300, TimeUnit.SECONDS)
                        .writeTimeout(300, TimeUnit.SECONDS)
                        .addInterceptor(AuthInterceptor(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"authToken")!!))
                        .addInterceptor(RetryInterceptor())
                        .build()
                    val retrofitWithToken =
                        RetrofitHelper.getInstance(applicationContext,httpClientWithToken)
                            .create(TwilioApi::class.java)
                    var request =
                        SearchContactRequest(
                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"currentUser")!!,
                            Constants.getStringFromVitalTextSharedPreferences(applicationContext,"tenantCode")!!,
                            s.toString()
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
                    setAdapter(emptyList())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun onSuggestionClick(suggestion: ContactListViewItem) {
        editText.setText(suggestion.name)
        binding.addChatParticipantSheet.identity.text = suggestion.email
//        editText.setSelection(suggestion.name.length)
        adapter= SuggestionAdapter(applicationContext,emptyList()){}
        adapter.notifyDataSetChanged()
    }

    fun setAdapter(list : List<ContactListViewItem>){
        Log.d("setadapter",list.toString()
        )
        adapter = SuggestionAdapter(applicationContext,list) { suggestion ->
            onSuggestionClick(suggestion)
        }
        recyclerView.adapter = adapter
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

    private fun initViews() {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " initViews Called")
        setSupportActionBar(binding.conversationDetailsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
        }

        binding.addNonChatParticipantButton.setOnClickListener {
            binding.addNonChatParticipantSheet.addNonChatParticipantPhoneInput.text?.clear()
            binding.addNonChatParticipantSheet.addNonChatParticipantProxyInput.text?.clear()
            addNonChatParticipantSheetBehavior.show()
        }

        binding.participantsListButton.setOnClickListener {
            ParticipantListActivity.start(this, conversationDetailsViewModel.conversationSid)
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
        }

        binding.conversationPinButton.setOnClickListener {
            val type = object : TypeToken<ArrayList<String>>() {}.type
            var jsonString = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"pinnedConvo")!!
            var pinnedConvo : ArrayList<String> = Gson().fromJson(jsonString, type)

            if(binding.conversationPinButton.text == getString(R.string.details_pin_conversation)){
                pinnedConvo.add(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
                conversationListViewModel.savePinnedConversationToDB()
//                binding.conversationPinButton.text == getString(R.string.details_unpin_conversation)
//                binding.conversationPinButton.setCompoundDrawables(resources.getDrawable(R.drawable.icon_unpin),null,null,null)
//                binding.details!!.isPinned = false
            }
            else {
                pinnedConvo.remove(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
                conversationListViewModel.savePinnedConversationToDB()
//                binding.conversationPinButton.text == getString(R.string.details_pin_conversation)
//                binding.conversationPinButton.setCompoundDrawables(resources.getDrawable(R.drawable.ic_pin),null,null,null)
//                binding.details!!.isPinned = true
            }
            Constants.saveStringToVitalTextSharedPreferences(applicationContext,"pinnedConvo",Gson().toJson(pinnedConvo!!))
//            binding.details!!.isPinned = binding.details!!.isPinned
        }

//        binding.conversationPinButton.setOnClickListener {
//            if(binding.conversationPinButton.text == getString(R.string.details_pin_conversation)){
//                Constants.PINNED_CONVO.add(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
//                conversationListViewModel.savePinnedConversationToDB()
//                binding.conversationPinButton.text == getString(R.string.details_unpin_conversation)
//                binding.conversationPinButton.setCompoundDrawables(resources.getDrawable(R.drawable.icon_unpin),null,null,null)
//                binding.details!!.isPinned = false
//            }
//            else if(binding.conversationPinButton.text == getString(R.string.details_unpin_conversation)){
//                Constants.PINNED_CONVO.remove(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
//                conversationListViewModel.savePinnedConversationToDB()
//                binding.conversationPinButton.text == getString(R.string.details_pin_conversation)
//                binding.conversationPinButton.setCompoundDrawables(resources.getDrawable(R.drawable.ic_pin),null,null,null)
//                binding.details!!.isPinned = true
//            }
//        }

//        binding.conversationPinButton.setOnClickListener {
//            if(binding.details!!.isPinned){
//                Log.d("pinned","pinned")
//                Constants.PINNED_CONVO.add(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
//                conversationListViewModel.savePinnedConversationToDB()
//                binding.conversationPinButton.text == getString(R.string.details_unpin_conversation)
//                binding.details!!.isPinned = false
//            }
//            else{
//                Log.d("pinned","unpinned")
//                Constants.PINNED_CONVO.remove(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
//                conversationListViewModel.savePinnedConversationToDB()
//                binding.conversationPinButton.text == getString(R.string.details_pin_conversation)
//                binding.details!!.isPinned = true
//            }
////            binding.details!!.isPinned = !binding.details!!.isPinned
//        }

        binding.conversationLeaveButton.setOnClickListener {
            conversationDetailsViewModel.leaveConversation()
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
            conversationDetailsViewModel.addChatParticipant(binding.addChatParticipantSheet.identity.text.toString())
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

        messageListViewModel.isWebChat.observe(this){ isWebChat ->
            if(isWebChat.toLowerCase() == "true") {
                binding.addChatParticipantButton.visibility = View.VISIBLE
            }
            else{
                binding.addChatParticipantButton.visibility = View.GONE
            }
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
            ConversationListActivity.start(this, 2)
            finish()
        }

        conversationDetailsViewModel.onParticipantAdded.observe(this) { identity ->
            binding.conversationDetailsLayout.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
        }
        conversationDetailsViewModel.isNetworkAvailable.observe(this) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable)
                this.finish()
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
