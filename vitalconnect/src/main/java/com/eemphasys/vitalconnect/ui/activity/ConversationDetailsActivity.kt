package com.eemphasys.vitalconnect.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SheetListener
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.*
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.databinding.ActivityConversationDetailsBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.conversationDetailsLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        binding = DataBindingUtil
            .setContentView<ActivityConversationDetailsBinding>(this, R.layout.activity_conversation_details)
            .apply {
                lifecycleOwner = this@ConversationDetailsActivity
            }

        initViews()
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

        binding.addChatParticipantSheet.addChatParticipantIdButton.setOnClickListener {
            addChatParticipantSheetBehavior.hide()
            conversationDetailsViewModel.addChatParticipant(binding.addChatParticipantSheet.addChatParticipantIdInput.text.toString())
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

        conversationDetailsViewModel.conversationDetails.observe(this) { conversationDetails ->
//            conversationDetails.friendlyName = conversationDetailsViewModel.getFriendlyName(binding.details!!.createdBy)
            binding.details = conversationDetails
//            val friendlyName = conversationDetailsViewModel.getFriendlyName(binding.details.createdBy)
//            binding.textView3.text = "Created by: " + friendlyName
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
