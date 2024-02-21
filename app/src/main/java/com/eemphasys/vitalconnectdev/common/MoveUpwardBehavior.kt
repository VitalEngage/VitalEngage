package com.eemphasys.vitalconnectdev.common

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import kotlin.math.min

class MoveUpwardBehavior : CoordinatorLayout.Behavior<LoginBackgroundLayout>() {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: LoginBackgroundLayout, dependency: View): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: LoginBackgroundLayout, dependency: View): Boolean {
        ViewCompat.animate(child.logoView).cancel()
        child.logoView.translationY = min(0f, dependency.translationY - dependency.height)
        return true
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: LoginBackgroundLayout, dependency: View) {
        ViewCompat.animate(child.logoView).cancel()
        ViewCompat.animate(child.logoView).translationY(0f).start();
    }
}