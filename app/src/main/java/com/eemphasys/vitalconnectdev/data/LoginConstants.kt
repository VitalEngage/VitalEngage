package com.eemphasys.vitalconnectdev.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class LoginConstants   {
    companion object{
        //Himanshu account
        /*var BASE_URL = "https://xappsweb.e-emphasys.com/VitalConnect/NewWebApi/"
        var TENANT_CODE = "VitalConnectNonAzureAd"
        var CLIENT_ID = "169JNr3JkBOMBopDincd2desazfFO+lQMtt8rdKJqlRSgSINwFlO17STwfJuYa3BatHv7t2NnnOh1B36KspC68oEkZZU1x3dJQhtsXzLqXa7LC2zMlOMcd5Q74UNeLxGNB9nejUoGpEdRmrNycXdtWf4IRY6Rn2z0JZ1/VwvIx89K46+vIW9EfyH2oHj7EHDg0w3vBMxYA591rPt/k+YT4u38/JT5TGaQTkg8Nlu7YTgHcAOV4nDEUTSNcAKvLkujeQJjajvsx+tCMTLzgm1HE5kZ6JRz9TnOwCTVHQy/9yvX3/2TbjIersyq3oSF3g0a5MgQZI9/b8VDRtax0doEJVPsrMKtNPxAlfKrKmzNrVsReyXJwVolsWDI3h2QCoVPM94Q8z5vJGObEpiuBaPI0qb7gQ3gOcgleSmjTjuo88eMdCsfI1XjZeLathRf9A6RkFIrboqLAJB6SkOqOAWbQVsM+YcLqMsW5YX4yy6RNhxcvB7pwlrZpAzLHMsI1vg"
        var CLIENT_SECRET = "cBTnK1QObPwWoGbY0Fv6doJaRnmwvnDcW0fWHtx7NDrwb1GF7oDhtFjtV6JBFXQ5xDb4WJ2ln9S7I4X7TpKH7L+LZ3Khs1zQO29ubgtX/J8C9fQI9b7kvaOPJJOpOquu8O/4RH1zA1A3qNMvrMtvargWPJzxiEBrcF34pk2j32doR8K8gBANerOG1zWpyDTIReOFVD5AUwdWp96xXlDEcM/SuHtrG9tMN1ookxnAC0bZ5fIMB5pDy5frXOSu+dkbC8JixVFGh3KlP3G1fu5dIAjAvHkqC5JRxYZig1MRVTxG3HatjFBBxlxkomVXjCihcvN1GWPzVHtw4MXJ8G/vtxcj/4dBUJ9L3T3QKWpqKwwoGrWqJkNOeOfw+b1OQBJWuLZ9WYa+Cz1dTp2izK7x4MBkcYLnK82iTWSi3fTK+gTyEZeiyp+JSDwPD2+DSsEBaWh2mPgcSiKVUPBdWFJzHw=="*/

        //Sachin's account
        var BASE_URL = ""
        var TENANT_CODE = ""
        //fields to be change as per tenant(clientid, secret, proxy)
        var CLIENT_ID = "VitalConnect-sQz2LmzcRLU07nnod9MlJJcJdcQj6iFZTg6uSnPbjxZ9Vssm9qONFhbPh64hmUYLxbKWpURQ0JesgZTvhNsFj3ca67lDuIWwyir4rS6GWFK5dxaHQ/4kqh8aCmB6JJP0"
        var CLIENT_SECRET = "+QXNy5ItEjPCSDG6sF7R23oy7M9sDjfFJuNcizgyRXYKjcTc98EFye7g4G5CTiee7QCLaEfQhd2i1mihW9tOTaFxsO077LlciZyNCWpoUYjH5LLoPiqYIw7Ux/JYF3gP"
        var PROXY_NUMBER = "+16503895687"
        var SHOW_CONTACTS = "false"
        var PRODUCT = "eLog"
        var CURRENT_USER =""
        var FRIENDLY_NAME = ""
        var TWILIO_TOKEN = ""
        var AUTH_TOKEN = ""
        var FULL_NAME =""
        var IS_AADENABLED = "false"
        var IS_STANDALONE = "true"
//        var USER_SMS_ALERT = ""
        var SHOW_DESIGNATION = ""
        var SHOW_DEPARTMENT = ""
        var EMAIL = ""
        var MOBILENUMBER = ""
        var WITH_CONTEXT = "false"
        var OPEN_CHAT = "false"
        var CONTEXT = "QWERTY"
        var DEALER_NAME = ""
        var PINNED_CONVO : ArrayList<String> = arrayListOf()
        var SHOW_INTERNAL_CONTACTS = "true"
        var SHOW_EXTERNAL_CONTACTS = "true"
        var EXPIRATION_DURATION = 0
        var REFRESH_TOKEN = ""



        val CONTACTS =
            """{
            "contacts" : 
            [
                  {
                     "name":"Ankush Belorkar",
                     "number":"+919422855735",
                     "customerName":"",
                     "initials":"",
                     "designation":"Technician",
                     "department":"Service",
                     "customer":"Customer",
                     "countryCode":"",
                     "role":"",
                     "bpId":""
                  },
                  {
                     "name":"Mark Moulder",
                     "number":"8600125105",
                     "customerName":"",
                     "initials":"",
                     "designation":"",
                     "department":"",
                     "customer":"",
                     "countryCode":"+91",
                     "role":"",
                     "bpId":""
                  }
            ]
       } """.trimIndent()

        val WEBUSERS = """{
            "webUser":[
            {
                "name":"Ankush Belorkar",
                "userName":"abelorkar@e-emphasys.com",
                "initials":"",
                "designation":"Technician",
                "department":"",
                "customer":"VitalEdge",
                "countryCode":"",
                "role":""
            },
            {
                "name":"Himanshu Mahajan",
                "userName":"hmahajan@e-emphasys.com",
                "initials":"",
                "designation":"Supervisor",
                "department":"Xapps",
                "customer":"VitalEdge",
                "countryCode":"",
                "role":""
            }
            ]
        }
        """.trimIndent()

        const val MyPREFERENCES = "MyVitaltextPrefs"
        var sharedpreferences: SharedPreferences? = null
        @JvmStatic
        fun getStringFromVitalTextSharedPreferences(context: Context?, key: String): String? {
            context?.let {
                sharedpreferences = context.getSharedPreferences(MyPREFERENCES,Context.MODE_PRIVATE)
                val value = sharedpreferences?.getString(key, null) // Use null as default value to check if key exists
                Log.d("SharedPreferencesUtil", "Retrieved value: $value for key: $key")
                return value
            } ?: Log.e("SharedPreferencesUtil", "Context is null")
            return null
        }

        @JvmStatic
        fun saveStringToVitalTextSharedPreferences(context: Context?, key: String, value: String) {
            context?.let {
                sharedpreferences = context.getSharedPreferences(MyPREFERENCES,Context.MODE_PRIVATE)
                val editor = sharedpreferences?.edit()
                editor?.putString(key, value)
                editor?.apply()
                Log.d("SharedPreferencesUtil", "Saved value: $value with key: $key")
            } ?: Log.e("SharedPreferencesUtil", "Context is null")
        }
    }
}