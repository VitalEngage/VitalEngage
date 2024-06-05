package com.eemphasys.vitalconnect.common

object ChatAppModel {
    var base_url : String? = null
    var twilio_token : String? = null
    var appId : String? = null

   fun init(
    baseurl: String?,
    twiliotoken : String?,
    appId : String?
    ){
        this.base_url = baseurl
       this.twilio_token=twiliotoken
       this.appId=appId
    }
}