package com.eemphasys.vitalconnect.api

import com.eemphasys.vitalconnect.api.data.AuthToken
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.api.data.Token
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TwilioApi {

    @POST("User/Authenticate")
    suspend fun getAuthToken(@Body requestData : RequestToken) : Response<AuthToken>

    @GET("Notification/getTwilioToken")
     suspend fun getTwilioToken(@Query("tenantCode") tenantCode : String,@Query("user") user : String, @Query("friendlyName") friendlyName: String) : Response<Token>

     @GET("Conversation/FetchParticipantConversations")
     fun fetchExistingConversation(@Query("tenantCode") tenantCode : String,@Query("identity") identity : String,@Query("isWebUser") isWebUser : Boolean,@Query("page") page : Int) : Call<List<ParticipantExistingConversation>>

     @GET("Conversation/AddParticipantToConversation")
     fun addParticipantToConversation(@Query("tenantCode") tenantCode : String,@Query("conversationSid") conversationSid : String,@Query("identity") identity : String, ) : Call<String>
}