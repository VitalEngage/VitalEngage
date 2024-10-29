package com.eemphasys.vitalconnect.common
import com.eemphasys.vitalconnect.interfaces.FirebaseLogEventListener

object ChatAppModel {
    var base_url : String? = null
    var twilio_token : String? = null
    var appId : String? = null
    var FirebaseLogEventListener: FirebaseLogEventListener?=null

   fun init(
    baseurl: String?,
    twiliotoken : String?,
    appId : String?,
    firebaseLogEventListener: FirebaseLogEventListener
    ){
        this.base_url = baseurl
       this.twilio_token=twiliotoken
       this.appId=appId
       this.FirebaseLogEventListener=firebaseLogEventListener
    }
}