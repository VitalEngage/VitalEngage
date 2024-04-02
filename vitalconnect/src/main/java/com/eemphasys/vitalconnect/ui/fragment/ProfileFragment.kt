package com.eemphasys.vitalconnect.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.getErrorMessage
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.databinding.FragmentProfileBinding
import com.eemphasys.vitalconnect.ui.dialogs.EditProfileDialog
import com.google.android.material.snackbar.Snackbar

class ProfileFragment:Fragment() {
    lateinit var binding: FragmentProfileBinding

    val profileViewModel by lazyActivityViewModel { injector.createProfileViewModel(applicationContext) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.title_profile)

        profileViewModel.selfUser.observe(viewLifecycleOwner) { user ->
            binding.profileName.text = user.friendlyName
            binding.profileIdentity.text = user.identity
        }

        profileViewModel.onUserUpdated.observe(viewLifecycleOwner) {
            Snackbar.make(view, R.string.profile_updated, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_navigation)
                .show()
        }

       /* profileViewModel.onSignedOut.observe(viewLifecycleOwner) {
            LoginActivity.start(requireContext(), SIGN_OUT_SUCCEEDED)
        }*/

        profileViewModel.onError.observe(viewLifecycleOwner) { error ->
            val message = requireContext().getErrorMessage(error)
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottom_navigation)
                .setAction(R.string.retry) { showEditProfileDialog() }
                .show()
        }

        binding.editProfile.setOnClickListener { showEditProfileDialog() }
        binding.signOut.setOnClickListener { showSignOutDialog() }
    }

    fun showEditProfileDialog() = EditProfileDialog().showNow(childFragmentManager, null)

    private fun showSignOutDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.sign_out_dialog_title)
            .setMessage(R.string.sign_out_dialog_message)
            //.setPositiveButton(R.string.sign_out) { _, _ -> profileViewModel.signOut() }
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
}