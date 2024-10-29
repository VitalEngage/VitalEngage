package com.eemphasys.vitalconnect.interfaces

import android.content.Context

interface FirebaseLogEventListener {
    fun screenLogEvent(context: Context, screenName:String, className:String)
    fun buttonLogEvent(context: Context, eventName: String, screenName:String, className:String)
}