package com.eemphasys.vitalconnect.common

object ChatAppModel {
    var base_url : String? = null
    var twilio_proxy :String? = null
    var tenant_Code : String? =null
    var client_id : String? = null
    var client_secret : String? = null
    var parent_app : String? = null
    var current_user : String? = null

   fun init(
    base_url: String?,
    twilio_proxy: String?,
    tenant_Code: String?,
    client_id : String?,
    client_secret : String?,
    parent_app : String?,
    current_user : String?,
    ){
        this.base_url = base_url
        this.twilio_proxy = twilio_proxy
        this.tenant_Code= tenant_Code
       this.client_id=client_id
       this.client_secret=client_secret
       this.parent_app=parent_app
       this.current_user=current_user
    }
}