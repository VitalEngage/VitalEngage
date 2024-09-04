package com.eemphasys.vitalconnect.api

import com.eemphasys.vitalconnect.api.data.AuthToken
import com.eemphasys.vitalconnect.api.data.EncryptionRequest
import com.eemphasys.vitalconnect.api.data.GetUserAlertStatusRequest
import com.eemphasys.vitalconnect.api.data.GetUserAlertStatusResponse
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.api.data.RequestToken
import com.eemphasys.vitalconnect.api.data.SearchContactRequest
import com.eemphasys.vitalconnect.api.data.SearchContactResponse
import com.eemphasys.vitalconnect.api.data.SendOtpReq
import com.eemphasys.vitalconnect.api.data.TenantDetails
import com.eemphasys.vitalconnect.api.data.Token
import com.eemphasys.vitalconnect.api.data.UpdatePasswordReq
import com.eemphasys.vitalconnect.api.data.UpdatePasswordResp
import com.eemphasys.vitalconnect.api.data.UserAlertRequest
import com.eemphasys.vitalconnect.api.data.ValidateUserReq
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
     fun fetchExistingConversation(@Query("tenantCode") tenantCode : String,@Query("identity") identity : String,@Query("isWebUser") isWebUser : Boolean,@Query("page") page : Int,@Query("proxyNumber") proxyNumber: String) : Call<List<ParticipantExistingConversation>>

     @GET("Conversation/AddParticipantToConversation")
     fun addParticipantToConversation(@Query("tenantCode") tenantCode : String,@Query("conversationSid") conversationSid : String,@Query("identity") identity : String, ) : Call<String>

    @POST("User/ValidateTenant")
    suspend fun validateTenant(@Query("tenantCode") tenantCode : String) : Response<TenantDetails>

    @POST("User/ValidateUser")
    suspend fun validateUser(@Body requestData : ValidateUserReq) : Response<AuthToken>

    @POST("User/UpdateUserAlertStatus")
    suspend fun updateUserAlertStatus(@Body requestData : UserAlertRequest) : Response<UserAlertRequest>

    @POST("User/SendOtp")
    suspend fun sendOTP(@Body requestData : SendOtpReq) : Response<String>

    @POST("User/SavePassword")
    suspend fun updatePassword(@Body requestData : UpdatePasswordReq) : Response<UpdatePasswordResp>

    @POST("GetEncryptedValues")
    suspend fun getEncryptedValues(@Body requestData : EncryptionRequest) : Response<EncryptionRequest>

    @GET("Conversation/GetUnreadMessageCountForWebUser")
    suspend fun getUnreadMessageCountForWebUser(@Query("tenantCode") tenantCode : String,@Query("identity") identity : String) : Response<Map<String, String>>

    @POST("Conversation/GetUnreadMessageCountForMobileNumbers")
    suspend fun getUnreadMessageCountForMobileNumbers(@Query("tenantCode") tenantCode: String, @Query("identity") identity: String, @Body requestData: ArrayList<String>) : Response<Map<String, Int>>

    @POST("Contact/GetSearchedContact")
    fun getSearchedContact(@Body requestData : SearchContactRequest) : Call<List<SearchContactResponse>>

    @POST("User/GetUserAlertStatus")
    suspend fun getUserAlertStatus(@Body requestData : GetUserAlertStatusRequest ) : Response<GetUserAlertStatusResponse>
}