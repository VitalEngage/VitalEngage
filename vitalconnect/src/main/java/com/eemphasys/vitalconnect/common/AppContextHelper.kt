package com.eemphasys.vitalconnect.common

import android.content.Context
import android.util.Log

object AppContextHelper {
    lateinit var appContext: Context
    fun init(ctx:Context)
    {
        Log.d("AppContextHelper","AppContextHelper invoked")
        appContext=ctx.applicationContext
    }
}