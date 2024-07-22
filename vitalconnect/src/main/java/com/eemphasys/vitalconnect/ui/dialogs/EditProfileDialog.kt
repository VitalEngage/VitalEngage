package com.eemphasys.vitalconnect.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.enableErrorResettingOnTextChanged
import com.eemphasys.vitalconnect.common.extensions.onSubmit
import com.eemphasys.vitalconnect.databinding.DialogEditProfileBinding

class EditProfileDialog : BaseBottomSheetDialogFragment(){
    lateinit var binding: DialogEditProfileBinding

    val profileViewModel by lazyActivityViewModel { injector.createProfileViewModel(applicationContext) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.profileFriendlyNameInputHolder.enableErrorResettingOnTextChanged()
        binding.profileFriendlyNameInput.onSubmit { saveChanges() }
        binding.saveChanges.setOnClickListener { saveChanges() }
        binding.cancelButton.setOnClickListener { dismiss() }
    }

    private fun saveChanges() {
        val friendlyName = binding.profileFriendlyNameInput.text.toString()
        if (friendlyName.isBlank()) {
            binding.profileFriendlyNameInputHolder.error = getString(R.string.profile_friendly_name_error_text)
            return
        }

        profileViewModel.setFriendlyName(friendlyName)
        dismiss()
    }
}