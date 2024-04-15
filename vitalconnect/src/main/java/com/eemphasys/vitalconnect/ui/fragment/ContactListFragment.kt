package com.eemphasys.vitalconnect.ui.fragment

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.adapters.ContactListAdapter
import com.eemphasys.vitalconnect.adapters.OnContactItemClickListener
import com.eemphasys.vitalconnect.api.AuthInterceptor
import com.eemphasys.vitalconnect.api.RetrofitHelper
import com.eemphasys.vitalconnect.api.RetryInterceptor
import com.eemphasys.vitalconnect.api.TwilioApi
import com.eemphasys.vitalconnect.api.data.ParticipantExistingConversation
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.showSnackbar
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.Contact
import com.eemphasys.vitalconnect.data.models.ContactListViewItem
import com.eemphasys.vitalconnect.data.models.WebUser
import com.eemphasys.vitalconnect.databinding.FragmentContactListBinding
import com.eemphasys.vitalconnect.ui.activity.MessageListActivity
import com.eemphasys_enterprise.commonmobilelib.EETLog
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import java.util.logging.Handler

class ContactListFragment : Fragment() {
    var binding: FragmentContactListBinding? = null

val contactListViewModel by lazyActivityViewModel { injector.createContactListViewModel(applicationContext) }

    var contactslist = arrayListOf<Contact>()
    var webuserlist = arrayListOf<WebUser>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
        setHasOptionsMenu(true)

        contactListViewModel.onParticipantAdded.observe(this) { identity ->
            binding?.contactsListLayout?.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
        }
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(shouldInterceptBackPress()){
                    Log.d("ContactListFragment","ConversationListFragement back button pressed")
                    // in here you can do logic when backPress is clicked
                }else{
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })

    }
    fun shouldInterceptBackPress() = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactListBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    fun combineLists(contacts: List<Contact>, webUsers: List<WebUser>): List<ContactListViewItem> {
        val combinedList = mutableListOf<ContactListViewItem>()

        // Convert Contact objects to ContactListViewItem
        val contactItems = contacts.map {
            ContactListViewItem(it.name, "", it.number, "SMS")
        }

        // Convert WebUser objects to ContactListViewItem
        val webUserItems = webUsers.map {
            ContactListViewItem(it.name, it.userName, "", "Chat")
        }

        // Add all ContactListViewItem objects to the combined list
        combinedList.addAll(contactItems)
        combinedList.addAll(webUserItems)

        return combinedList
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.contacts)


        contactslist = ArrayList<Contact>()
        webuserlist = ArrayList<WebUser>()

        val jsonObjectcontacts = JSONObject(Constants.CONTACTS)
        val jsonArrayContacts = jsonObjectcontacts.getJSONArray("contacts")
        for (i in 0 until jsonArrayContacts.length()) {
            val jsonObject = jsonArrayContacts.getJSONObject(i)
            val name = jsonObject.getString("name")
            val number = jsonObject.getString("number")
            val customerName = jsonObject.getString("customerName")
            contactslist.add(Contact(name,number,customerName,"","","",""))
            //Log.d("contactname",name)
        }

        val jsonObjectwebusers = JSONObject(Constants.WEBUSERS)
        val jsonArrayWebUsers = jsonObjectwebusers.getJSONArray("webUser")
        for (i in 0 until jsonArrayWebUsers.length()) {
            val jsonObject = jsonArrayWebUsers.getJSONObject(i)
            val name = jsonObject.getString("name")
            val userName = jsonObject.getString("userName")
            webuserlist.add(WebUser(name,userName,"","","",""))
            //Log.d("webusername",name)
        }

        val combinedList = combineLists(contactslist, webuserlist)

        val httpClientWithToken = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(Constants.AUTH_TOKEN))
            .addInterceptor(RetryInterceptor())
            .build()
        val retrofitWithToken =
            RetrofitHelper.getInstance(httpClientWithToken).create(TwilioApi::class.java)

        //Creating the Adapter
        val adapter = ContactListAdapter(combinedList,object : OnContactItemClickListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onContactItemClick(contact: ContactListViewItem) {

                // Handle item click here
                //Log.d("ContactClicked", "Name: ${contact.name}, Number: ${contact.number}, Type: ${contact.type}")

                if (contact.type == "SMS") {
                    binding?.progressBarID?.visibility = VISIBLE

                    val existingConversation  = retrofitWithToken.fetchExistingConversation(
                        Constants.TENANT_CODE,
                        contact.number,
                        false,
                        1
                    )
                    existingConversation.enqueue(object : Callback<List<ParticipantExistingConversation>> {
                        @SuppressLint("SuspiciousIndentation")
                        override fun onResponse(
                            call: Call<List<ParticipantExistingConversation>>,
                            response: Response<List<ParticipantExistingConversation>>
                        ) {
                            if (response.isSuccessful) {
                                val conversationList: List<ParticipantExistingConversation>? = response.body()

                                // Check if the list is not null and not empty
                                if (!conversationList.isNullOrEmpty()) {
                                    // Iterate through the list and access each Conversation object
                                    for (conversation in conversationList) {
                                        // Access properties of each Conversation object
                                        println("Conversation SID: ${conversation.conversationSid}")

                                        try{
                                        val participantSid = retrofitWithToken.addParticipantToConversation(Constants.TENANT_CODE,conversation.conversationSid,Constants.USERNAME)

                                            participantSid.enqueue(object : Callback<String> {
                                                override fun onResponse(call: Call<String>, response: Response<String>) {
                                                    if (response.isSuccessful) {
                                                        val responseBody: String? = response.body()
                                                        // Handle the string value as needed
                                                        println("Response body: $responseBody")
                                                    } else {
                                                        println("Response was not successful: ${response.code()}")
                                                    }
                                                }

                                                override fun onFailure(call: Call<String>, t: Throwable) {
                                                    println("Failed to execute request: ${t.message}")
                                                }
                                            })
                                        }
                                        catch (e: Exception){
                                            println("Exception :  ${e.message}")
                                        }
                                        //Starting and redirecting to Existing conversation
//                                        delay(1000)
                                        MessageListActivity.startfromFragment(applicationContext,conversation.conversationSid)
                                        binding?.progressBarID?.visibility = GONE
                                    }
                                } else { //If there is no existing conversation with SMS user, create new
                                    contactListViewModel.createConversation(contact.name + " " + contact.number ,contact.email,contact.number)
                                    binding?.progressBarID?.visibility = GONE
                                }
                            } else {
                                println("Response was not successful: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<List<ParticipantExistingConversation>>, t: Throwable) {
                            println("Failed to fetch existing conversations: ${t.message}")
                        }
                    })


                }
                else{
                    binding?.progressBarID?.visibility = VISIBLE
                    //Below code can be uncommented if we want universal channel for webchatusers too.

                    //If contact is webchat user
//                    val existingConversation  = retrofitWithToken.fetchExistingConversation(
//                        Constants.TENANT_CODE,
//                        contact.email,
//                        true,
//                        1
//                    )
//
//                    existingConversation.enqueue(object : Callback<List<ParticipantExistingConversation>> {
//                        override fun onResponse(
//                            call: Call<List<ParticipantExistingConversation>>,
//                            response: Response<List<ParticipantExistingConversation>>
//                        ) {
//                            if (response.isSuccessful) {
//                                val conversationList: List<ParticipantExistingConversation>? = response.body()
//
//                                // Check if the list is not null and not empty
//                                if (!conversationList.isNullOrEmpty()) {
//                                    // Iterate through the list and access each Conversation object
//                                    for (conversation in conversationList) {
//                                        // Access properties of each Conversation object
//                                        println("Conversation SID: ${conversation.conversationSid}")
//
//                                        //Starting and redirecting to Existing conversation
//                                        MessageListActivity.startfromFragment(applicationContext,conversation.conversationSid)
//                                    }
//                                } else { //If there is no existing conversation with web user, create new
                                    contactListViewModel.createConversation(contact.name,contact.email,contact.number)
                                    binding?.progressBarID?.visibility = GONE
                //                                }
//                            } else {
//                                println("Response was not successful: ${response.code()}")
//                            }
//                        }
//
//                        override fun onFailure(call: Call<List<ParticipantExistingConversation>>, t: Throwable) {
//                            println("Failed to fetch existing conversations: ${t.message}")
//                        }
//                    })
                }
            }
        })

        //Providing Linearlayoutmanager to recyclerview
        binding?.contactList?.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
       //Assigning the created adapter to recyclerview
        binding?.contactList?.adapter = adapter
        binding?.contactList?.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

    }

}