package com.eemphasys.vitalconnect.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.ConversationsClientWrapper
import com.eemphasys.vitalconnect.databinding.FragmentProfileBinding
import com.eemphasys.vitalconnect.ui.dialogs.EditProfileDialog
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.google.android.material.snackbar.Snackbar
import com.twilio.conversations.extensions.createConversationsClient
import kotlinx.coroutines.launch

class ProfileFragment:Fragment() {
    lateinit var binding: FragmentProfileBinding
    private val noInternetSnackBar by lazy {
        Snackbar.make(binding.root, R.string.no_internet_connection, Snackbar.LENGTH_INDEFINITE)
    }
    private val profileViewModel by lazyActivityViewModel { injector.createProfileViewModel(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreate Called")
        setHasOptionsMenu(true)

//        lifecycleScope.launch {
//            val user = ConversationsClientWrapper.INSTANCE.getConversationsClient().myUser
//            binding.profileName.text = user.friendlyName
//            binding.profileIdentity.text = user.identity
//            binding.profileImage.text = Constants.getInitials(user.friendlyName.trim { it <= ' '} )
//            binding.emailId.text = Constants.EMAIL
//            binding.phoneNumber.text = Constants.MOBILENUMBER
//        }
    }

    fun shouldInterceptBackPress() = true
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onCreateView Called")
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.switchbtn.setOnCheckedChangeListener{ buttonView,isChecked ->

            profileViewModel.changeUserAlertStatus(isChecked,applicationContext)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EETLog.saveUserJourney("vitaltext: " + this::class.java.simpleName + " onViewCreated Called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.title_settings)

        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"userSMSAlert").equals("true", ignoreCase = true)){
            binding.switchbtn.isChecked = true
        }
        else if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"userSMSAlert").equals("false", ignoreCase = true)) {
            binding.switchbtn.isChecked = false
        }


        profileViewModel.selfUser.observe(viewLifecycleOwner) { user ->
            binding.profileName.text = user.friendlyName
            binding.profileIdentity.text = user.identity
            binding.profileImage.text = Constants.getInitials(user.friendlyName.trim { it <= ' '} )
            binding.emailId.text = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"email")
            binding.phoneNumber.text = Constants.getStringFromVitalTextSharedPreferences(applicationContext,"mobileNumber")
        }

        profileViewModel.onUserUpdated.observe(viewLifecycleOwner) {
            Snackbar.make(view, R.string.profile_updated, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_navigation)
                .show()
        }

        profileViewModel.onSignedOut.observe(viewLifecycleOwner) {
//            LoginActivity.start(requireContext())
            activity?.finish()
        }

        profileViewModel.onError.observe(viewLifecycleOwner) { error ->
            val message = requireContext().getErrorMessage(error)
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_navigation)
                .setAction(R.string.retry) { showEditProfileDialog() }
                .show()
        }

//        binding.editProfile.setOnClickListener { showEditProfileDialog() }
        binding.signOut.setOnClickListener { showSignOutDialog() }
//        if(Constants.USER_SMS_ALERT.equals("true", ignoreCase = true)){
//            binding.switchbtn.isChecked = true
//        }
//        else if(Constants.USER_SMS_ALERT.equals("false", ignoreCase = true)) {
//            binding.switchbtn.isChecked = false
//        }
//        binding.switchbtn.setOnCheckedChangeListener{ buttonView,isChecked ->
//
//            profileViewModel.changeUserAlertStatus(isChecked)
//        }

        profileViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable)
                activity?.finish()
        }

        if(Constants.getStringFromVitalTextSharedPreferences(applicationContext,"isStandalone")!!.lowercase()== "false") {
            binding.signOut.visibility= View.GONE
        }

    }

    fun showEditProfileDialog() = EditProfileDialog().showNow(childFragmentManager, null)

    private fun showSignOutDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.sign_out_dialog_title)
            .setMessage(R.string.sign_out_dialog_message)
            .setPositiveButton(R.string.sign_out) { _, _ -> profileViewModel.signOut() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        }

        dialog.show()
    }
    private fun showNoInternetSnackbar(show: Boolean) {

        if (show) {
            noInternetSnackBar.show()
        } else {
            noInternetSnackBar.dismiss()
        }
    }
}