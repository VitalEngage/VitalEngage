package com.eemphasys.vitalconnect.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.ConversationListAdapter
import com.eemphasys.vitalconnect.adapters.OnConversationEvent
import com.eemphasys.vitalconnect.common.Constants.Companion.getSearchViewEditText
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.databinding.FragmentConversationListBinding
import com.eemphasys.vitalconnect.ui.ConversationListSwipeCallback
import com.google.android.material.snackbar.Snackbar
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.onDismissed
import com.eemphasys.vitalconnect.common.extensions.requireValue
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.data.models.ConversationListViewItem
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import kotlinx.coroutines.delay
import java.lang.reflect.Field

class ConversationListFragment:Fragment(), OnConversationEvent {
    lateinit var binding: FragmentConversationListBinding

    private val adapter by lazy { ConversationListAdapter(this,applicationContext) }

    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.conversationsListLayout, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }

    val conversationListViewModel by lazyActivityViewModel { injector.createConversationListViewModel(applicationContext) }
    var isSelectedMap = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        setHasOptionsMenu(true)
    }

    private fun handleLabelClick(clickedLabel: TextView) {
        val isSelected = isSelectedMap[clickedLabel.id] ?: true
        if (isSelected) {
            clickedLabel?.let {
                it.background = resources.getDrawable(R.drawable.bg_selectedlabel)
                it.setTextColor(resources.getColorStateList(R.color.white))
                // Perform logic on deselection
                onLabelSelect(it)
            }
        }
        else{
            clickedLabel?.let {
                it.background = resources.getDrawable(R.drawable.bg_unselectedlabel)
                it.setTextColor(resources.getColorStateList(R.color.alternate_message_text))
                // Perform logic on deselection
                onLabelDeselect(it)
            }
        }
        isSelectedMap[clickedLabel.id] = !isSelected
    }

    private fun onLabelSelect(label: TextView) {
        //when a label is selected
        adapter.setFilter(label.text.toString(),true)
    }

    private fun onLabelDeselect(label: TextView) {
        //when a label is deselected
        adapter.setFilter(label.text.toString(),false)
    }

    fun shouldInterceptBackPress() = true
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreateView Called")
        binding = FragmentConversationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume","onResumeCalled")
        conversationListViewModel.getUserConversations()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.title_conversations_list)

        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showInternalContacts")!! == "false") {
            binding.lblInternal.visibility = View.GONE
        }

        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showExternalContacts")!! == "false"){
            binding.lblExternal.visibility = View.GONE
        }

        binding.lblUnread.text = "Unread"
        binding.lblInternal.text = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"dealerName")!!
        binding.lblExternal.text = "Customer"

        isSelectedMap[binding.lblUnread.id] = true
        isSelectedMap[binding.lblInternal.id] = true
        isSelectedMap[binding.lblExternal.id] = true
        // Set click listeners
//        binding.label0.setOnClickListener { handleLabelClick(binding.label0) }
        binding.lblUnread.setOnClickListener { handleLabelClick(binding.lblUnread) }
        binding.lblInternal.setOnClickListener { handleLabelClick(binding.lblInternal) }
        binding.lblExternal.setOnClickListener { handleLabelClick(binding.lblExternal) }

        conversationListViewModel.userConversationItems.observe(viewLifecycleOwner) { it:List<ConversationListViewItem>->
            adapter.notifyDataSetChanged()
            adapter.conversations = it
            adapter.allConversations = it
            binding.conversationList.scrollToPosition(0)
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

        conversationListViewModel.pinConversation.observe(viewLifecycleOwner) { pinned ->
            val message = if (pinned) R.string.conversation_pinned else R.string.conversation_unpinned
            showSnackbar(message)
        }

        binding.conversationRefresh.setOnRefreshListener { conversationListViewModel.getUserConversations() }
        binding.conversationList.adapter = adapter
        binding.conversationList.scrollToPosition(0)
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

    override fun onConversationLongClicked(conversation: ConversationListViewItem) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onConversationLongClicked Called")
        showPinnedConversationDialog(conversation)
    }

    override fun onParticipantIconClicked(conversation: ConversationListViewItem) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onParticipantIconClicked Called")
        showPopup(conversation)
        Log.d("clicked","clicked")
    }

    private fun showPopup(conversation: ConversationListViewItem) {
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

        name.text = conversation.name
        department.text = "(" + conversation.department + ")"
        designation.text = conversation.designation
        customer.text = conversation.customer
        bpId.text = conversation.bpId
//        if(contact.type == "SMS"){
//            number.text = contact.number
//        }else{
//            number.text = contact.email
//        }

        if (conversation.designation.isNullOrEmpty() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDesignation")!! == "false"){
            designation.visibility = View.GONE
        }
        if (conversation.department.isNullOrEmpty() || Constants.getStringFromVitalTextSharedPreferences(applicationContext,"showDepartment")!! == "false"){
            department.visibility = View.GONE
        }
        if (conversation.customer.isNullOrEmpty()){
            customer.visibility = View.GONE
        }
        if (conversation.bpId.isNullOrEmpty()){
            bpId.visibility = View.GONE
        }
//        if (conversation.number.isNullOrEmpty()){
            number.visibility = View.GONE
//        }
        if (conversation.role.isNullOrEmpty()){
            roleName.visibility = View.GONE
            roledividerline.visibility = View.GONE
        }
        if(conversation.customer.isNullOrEmpty() && conversation.bpId.isNullOrEmpty()){
            customerdividerline.visibility = View.GONE
        }

        var itemsArray : Array<String>?
        if(conversation.role.isNullOrEmpty()){
            itemsArray = arrayOf()
        }else {
            itemsArray = conversation.role!!.split(",").toTypedArray()
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
        val rootView = requireActivity().window.decorView.findViewById<View>(android.R.id.content) as ViewGroup
        rootView.addView(dimBackground)
        // Close the popup when the button is clicked
        val closeButton: Button = popupView.findViewById(R.id.close_button)
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

    fun showPinnedConversationDialog(conversation : ConversationListViewItem){

        if(conversation.isPinned){
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(getText(R.string.unpinConversation))
                .setPositiveButton(getText(R.string.cancel), null)
                .setNegativeButton(getText(R.string.yes)) { _, _ -> conversationListViewModel.savePinnedConversation(conversation,false,adapter) }
                .create()
            dialog.setOnShowListener {
                val color = ContextCompat.getColor(requireContext(), R.color.colorEet)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.alternate_message_text))

                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
            }

            dialog.show()
        }
        else {
    val dialog = AlertDialog.Builder(requireContext())
        .setTitle(getText(R.string.pinConversation))
        .setPositiveButton(getText(R.string.cancel), null)
        .setNegativeButton(getText(R.string.yes)) { _, _ -> conversationListViewModel.savePinnedConversation(conversation,true,adapter) }
        .create()
    dialog.setOnShowListener {
        val color = ContextCompat.getColor(requireContext(), R.color.colorEet)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.alternate_message_text))

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
    }

    dialog.show()
    }
    }
    private fun showLeaveConfirmationDialog(conversationSid: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_dialog_title)
            .setMessage(R.string.leave_dialog_message)
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.leave) { _, _ -> conversationListViewModel.leaveConversation(conversationSid) }
            .create()

        dialog.setOnShowListener {
            val color = ContextCompat.getColor(requireContext(), R.color.colorEet)
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