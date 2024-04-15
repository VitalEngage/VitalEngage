package com.eemphasys.vitalconnect.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.ParticipantListAdapter
import com.eemphasys.vitalconnect.common.SheetListener
import com.eemphasys.vitalconnect.common.extensions.*
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.ActivityParticipantsBinding
import com.eemphasys_enterprise.commonmobilelib.EETLog

class ParticipantListActivity : AppCompatActivity() {
    private val binding by lazy { ActivityParticipantsBinding.inflate(layoutInflater) }
    private val sheetBehavior by lazy { BottomSheetBehavior.from(binding.participantDetailsSheet.root) }
    private val sheetListener by lazy { SheetListener(binding.sheetBackground) }

    val participantListViewModel by lazyViewModel {
        injector.createParticipantListViewModel(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
        setContentView(binding.root)

        initViews()
    }

    override fun onBackPressed() {
        if (sheetBehavior.isShowing()) {
            sheetBehavior.hide()
            return
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_participant_list, menu)

        val filterMenuItem = menu.findItem(R.id.filter_participants)
        if (participantListViewModel.participantFilter.isNotEmpty()) {
            filterMenuItem.expandActionView()
        }
        (filterMenuItem.actionView as SearchView).apply {
            queryHint = getString(R.string.participant_filter_hint)
            if (participantListViewModel.participantFilter.isNotEmpty()) {
                setQuery(participantListViewModel.participantFilter, false)
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String): Boolean {
                    participantListViewModel.participantFilter = newText
                    return true
                }
            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_participants -> sheetBehavior.hide()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        setSupportActionBar(binding.conversationToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.conversationToolbar.setNavigationOnClickListener { onBackPressed() }
        sheetBehavior.addBottomSheetCallback(sheetListener)
        title = getString(R.string.participant_title)
        val adapter = ParticipantListAdapter { participant ->
            participantListViewModel.selectedParticipant = participant

            binding.participantDetailsSheet.participantDetailsName.text = participant.friendlyName
            binding.participantDetailsSheet.participantDetailsStatus.setText(if (participant.isOnline) R.string.participant_online else R.string.participant_offline)
//            sheetBehavior.show()
        }

        binding.participantRefresh.setOnRefreshListener { participantListViewModel.getConversationParticipants() }
        binding.participantList.adapter = adapter

        binding.sheetBackground.setOnClickListener {
            sheetBehavior.hide()
        }

        binding.participantDetailsSheet.participantDetailsRemove.setOnClickListener {
            participantListViewModel.removeSelectedParticipant()
            sheetBehavior.hide()
        }

        participantListViewModel.participantsList.observe(this) { participants ->
            adapter.participants = participants
            binding.participantRefresh.isRefreshing = false
        }
        participantListViewModel.onParticipantError.observe(this) { error ->
            binding.participantListLayout.showSnackbar(getErrorMessage(error))
        }
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

        fun start(context: Context, conversationSid: String) =
            context.startActivity(getStartIntent(context, conversationSid))

        fun getStartIntent(context: Context, conversationSid: String) =
            Intent(context, ParticipantListActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)
    }
}
