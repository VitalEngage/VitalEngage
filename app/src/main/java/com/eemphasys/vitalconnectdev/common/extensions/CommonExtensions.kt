package com.eemphasys.vitalconnectdev.common.extensions

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.onSubmit(func: () -> Unit) = setOnEditorActionListener { _, actionId, keyEvent ->

    if (actionId == EditorInfo.IME_ACTION_DONE
        || (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
    ) {
        hideKeyboard()
        func()
        return@setOnEditorActionListener true
    }

    return@setOnEditorActionListener false
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}