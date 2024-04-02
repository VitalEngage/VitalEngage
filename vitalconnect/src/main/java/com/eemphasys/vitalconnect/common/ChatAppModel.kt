package com.eemphasys.vitalconnect.common

object ChatAppModel {
    var base_url : String? = null
    var twilio_token : String? = null

   fun init(
    base_url: String?,
    twilio_token : String?
    ){
        this.base_url = base_url
       this.twilio_token=twilio_token
    }
}