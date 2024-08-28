package com.eemphasys.vitalconnect.ui.fragment

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.ConversationListAdapter
import com.eemphasys.vitalconnect.adapters.OnConversationEvent
import com.eemphasys.vitalconnect.common.Constants.Companion.getSearchViewEditText
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.databinding.FragmentConversationListBinding
import com.eemphasys.vitalconnect.ui.ConversationListSwipeCallback
import com.google.android.material.snackbar.Snackbar
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.onDismissed
import com.eemphasys.vitalconnect.common.extensions.requireValue
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import java.lang.reflect.Field

class ConversationListFragment:Fragment(), OnConversationEvent {
    lateinit var binding: FragmentConversationListBinding

    private val adapter by lazy { ConversationListAdapter(this) }

    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.conversationsListLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }

    val conversationListViewModel by lazyActivityViewModel { injector.createConversationListViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        setHasOptionsMenu(true)
    }

    fun shouldInterceptBackPress() = true
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreateView Called")
        binding = FragmentConversationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.title_conversations_list)

        conversationListViewModel.userConversationItems.observe(viewLifecycleOwner) { it:List<ConversationListViewItem>->
            adapter.conversations = it
            binding.conversationRefresh.isRefreshing = false
        }

        conversationListViewModel.isNoConversationsVisible.observe(viewLifecycleOwner) { visible ->
            binding.noConversations.root.visibility = if (visible) View.VISIBLE else View.GONE
        }

        conversationListViewModel.isNoResultsFoundVisible.observe(viewLifecycleOwner) { visible ->
            binding.noResultFound.root.visibility = if (visible) View.VISIBLE else View.GONE
        }

        conversationListViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable)
                activity?.finish()
        }

        conversationListViewModel.onConversationCreated.observe(viewLifecycleOwner) {
            showSnackbar(R.string.conversation_created)
        }

        conversationListViewModel.onConversationLeft.observe(viewLifecycleOwner) {
            showSnackbar(R.string.conversation_left)
        }

        conversationListViewModel.onConversationMuted.observe(viewLifecycleOwner) { muted ->
            val message = if (muted) R.string.conversation_muted else R.string.conversation_unmuted
            showSnackbar(message)
        }

        conversationListViewModel.onConversationError.observe(viewLifecycleOwner) { error ->
            showSnackbar(requireContext().getErrorMessage(error))
        }

        binding.conversationRefresh.setOnRefreshListener { conversationListViewModel.getUserConversations() }
        binding.conversationList.adapter = adapter
        binding.conversationList.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        //binding.conversationList.addFabExtendingOnScrollListener(binding.newConversationFab)

        val swipeCallback = ConversationListSwipeCallback(requireContext(), adapter)

        swipeCallback.onMute = { conversationSid ->
            conversationListViewModel.muteConversation(conversationSid)
        }

        swipeCallback.onUnMute = { conversationSid ->
            conversationListViewModel.unmuteConversation(conversationSid)
        }

        swipeCallback.onLeave = { conversationSid ->
            showLeaveConfirmationDialog(conversationSid)

        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.conversationList)

//        binding.newConversationFab.setOnClickListener {
//            NewConversationDialog().showNow(childFragmentManager, null)
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_conversation_list, menu)

        val filterMenuItem = menu.findItem(R.id.filter_conversations)
        if (conversationListViewModel.conversationFilter.isNotEmpty()) {
            filterMenuItem.expandActionView()
        }

        (filterMenuItem.actionView as SearchView).apply {
            queryHint = getString(R.string.conversation_filter_hint)
            val searchEditText = getSearchViewEditText(this)
            searchEditText?.filters = arrayOf(InputFilter.LengthFilter(15))
            if (conversationListViewModel.conversationFilter.isNotEmpty()) {
                setQuery(conversationListViewModel.conversationFilter, false)
            }

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String): Boolean {
                    conversationListViewModel.conversationFilter = newText
                    return true
                }

            })
        }
    }

    override fun onConversationClicked(conversationSid: String) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onConversationClicked Called" + conversationSid)
        MessageListActivity.start(requireContext(), conversationSid)
    }

    private fun showLeaveConfirmationDialog(conversationSid: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_dialog_title)
            .setMessage(R.string.leave_dialog_message)
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.leave) { _, _ -> conversationListViewModel.leaveConversation(conversationSid) }
            .create()

        dialog.setOnShowListener {
            val color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        }

        dialog.show()
    }

    private fun showSnackbar(@StringRes messageId: Int) = showSnackbar(getString(messageId))

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.conversationsListLayout, message, Snackbar.LENGTH_SHORT)
            .onDismissed { showNoInternetSnackbar(!conversationListViewModel.isNetworkAvailable.requireValue()) }
            .show()
    }

    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }
}