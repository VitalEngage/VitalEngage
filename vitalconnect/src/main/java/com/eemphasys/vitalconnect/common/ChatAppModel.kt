package com.eemphasys.vitalconnect.common

object ChatAppModel {
    var base_url : String? = null
    var twilio_token : String? = null
    var appId : String? = null

   fun init(
    base_url: String?,
    twilio_token : String?,
    appId : String?
    ){
        this.base_url = base_url
       this.twilio_token=twilio_token
       this.appId=appId
    }
}