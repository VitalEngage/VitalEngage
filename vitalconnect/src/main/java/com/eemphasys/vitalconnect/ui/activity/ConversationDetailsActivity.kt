package com.eemphasys.vitalconnect.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import com.eemphasys.vitalconnect.adapters.SuggestionAdapter
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchUsersResponse
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SheetListener
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.*
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.databinding.ActivityConversationDetailsBinding
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

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
        adapter= SuggestionAdapter(emptyList()){}
        adapter.notifyDataSetChanged()
    }

    fun setAdapter(list : List<ContactListViewItem>){
        Log.d("setadapter",list.toString()
        )
        adapter = SuggestionAdapter(list) { suggestion ->
            onSuggestionClick(suggestion)
        }
        recyclerView.adapter = adapter
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

        binding.addChatParticipantSheet.recyclerView

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
            ConversationListActivity.start(this)
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
