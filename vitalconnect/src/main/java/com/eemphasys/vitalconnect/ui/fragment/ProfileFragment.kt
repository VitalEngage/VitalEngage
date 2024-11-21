package com.eemphasys.vitalconnect.ui.fragment

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.FragmentProfileBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.ui.dialogs.EditProfileDialog
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.google.android.material.snackbar.Snackbar

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

    }

    override fun onResume() {
        super.onResume()
        ChatAppModel.FirebaseLogEventListener?.screenLogEvent(requireContext(),"VC_Settings","ProfileFragment")
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
        binding.root.showSnackbar(getString(
            R.string.settings_saved,
        ))
            profileViewModel.changeUserAlertStatus(isChecked,applicationContext)
            ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(applicationContext, "VC_Settings_AlertToggleClick",
                "Settings",
                "ProfileFragment"
            )
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
            changeButtonBackgroundColor(
                binding.profileImage,
                ParticipantColorManager.getColorForParticipant(user.friendlyName),
                ParticipantColorManager.getDarkColorForParticipant(user.friendlyName)
            )
        }

        profileViewModel.onUserUpdated.observe(viewLifecycleOwner) {
            Snackbar.make(view, R.string.profile_updated, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_navigation)
                .show()
        }

        profileViewModel.onSignedOut.observe(viewLifecycleOwner) {
            val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", AppCompatActivity.MODE_PRIVATE)
//            sharedPreferences.edit().clear().apply()  //toclear all values
            //to clear specific values
            with(sharedPreferences.edit()) {
                remove("username")
                remove("password")
                apply() // Commit changes
            }
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


        profileViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isNetworkAvailable ->
            showNoInternetSnackbar(!isNetworkAvailable)
            if(!isNetworkAvailable){
                Constants.showPopup(layoutInflater, requireActivity())
                var layout = layoutInflater.inflate(R.layout.activity_conversation_list, null)
                var text = layout.findViewById<TextView>(R.id.textBox)
                text.text = "offline"
                text.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            }
//                activity?.finish()
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
            val color = ContextCompat.getColor(requireContext(), R.color.colorEet)
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

    private fun changeButtonBackgroundColor(textView: TextView?, colorid: Int, coloridText:Int) {
        try {
            val background = textView!!.background
            if (background is ShapeDrawable) {
                background.paint.color = colorid
                textView.setTextColor(coloridText)
            } else if (background is GradientDrawable) {
                background.setColor(colorid)
                textView.setTextColor(coloridText)
            } else if (background is ColorDrawable) {
                background.color = colorid
                textView.setTextColor(coloridText)
            }
        } catch (e: Exception) {
            Log.e("Catchmessage", Log.getStackTraceString(e))
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
}