package com.eemphasys.vitalconnectdev.data

class LoginConstants   {
    companion object{
        //Himanshu account
//        var BASE_URL = "https://xappsweb.e-emphasys.com/VitalConnect/NewWebApi/"
//        var TENANT_CODE = "VitalConnectNonAzureAd"
//        var CLIENT_ID = "169JNr3JkBOMBopDincd2desazfFO+lQMtt8rdKJqlRSgSINwFlO17STwfJuYa3BatHv7t2NnnOh1B36KspC68oEkZZU1x3dJQhtsXzLqXa7LC2zMlOMcd5Q74UNeLxGNB9nejUoGpEdRmrNycXdtWf4IRY6Rn2z0JZ1/VwvIx89K46+vIW9EfyH2oHj7EHDg0w3vBMxYA591rPt/k+YT4u38/JT5TGaQTkg8Nlu7YTgHcAOV4nDEUTSNcAKvLkujeQJjajvsx+tCMTLzgm1HE5kZ6JRz9TnOwCTVHQy/9yvX3/2TbjIersyq3oSF3g0a5MgQZI9/b8VDRtax0doEJVPsrMKtNPxAlfKrKmzNrVsReyXJwVolsWDI3h2QCoVPM94Q8z5vJGObEpiuBaPI0qb7gQ3gOcgleSmjTjuo88eMdCsfI1XjZeLathRf9A6RkFIrboqLAJB6SkOqOAWbQVsM+YcLqMsW5YX4yy6RNhxcvB7pwlrZpAzLHMsI1vg"
//        var CLIENT_SECRET = "cBTnK1QObPwWoGbY0Fv6doJaRnmwvnDcW0fWHtx7NDrwb1GF7oDhtFjtV6JBFXQ5xDb4WJ2ln9S7I4X7TpKH7L+LZ3Khs1zQO29ubgtX/J8C9fQI9b7kvaOPJJOpOquu8O/4RH1zA1A3qNMvrMtvargWPJzxiEBrcF34pk2j32doR8K8gBANerOG1zWpyDTIReOFVD5AUwdWp96xXlDEcM/SuHtrG9tMN1ookxnAC0bZ5fIMB5pDy5frXOSu+dkbC8JixVFGh3KlP3G1fu5dIAjAvHkqC5JRxYZig1MRVTxG3HatjFBBxlxkomVXjCihcvN1GWPzVHtw4MXJ8G/vtxcj/4dBUJ9L3T3QKWpqKwwoGrWqJkNOeOfw+b1OQBJWuLZ9WYa+Cz1dTp2izK7x4MBkcYLnK82iTWSi3fTK+gTyEZeiyp+JSDwPD2+DSsEBaWh2mPgcSiKVUPBdWFJzHw=="

        //Sachin's account
        var BASE_URL = "https://vitalconnect.azurewebsites.net/"
        var TENANT_CODE = "VitalEdge"
        var CLIENT_ID = "169JNr3JkBOMBopDincd2TCB+kMrmSNuXGjj3GUg0UsnuIOs2lny0ZG18Tsrq9+D312KFj1qLOM+7fuYXbDT9qCcoXFKJol8bc0xYPXtQsLIKS8jV38dcFJwKtRc/ITSjuytU7rvbQ3e9eNHspi4HmnToR7cYHr03rAeLzBdsZhbjaSJGS/rxz9LtZFYwYsxfK2NbqlMAl9DdMKgW8yJPULbBZR5fw6RQfjZknPwut7krh/unAgl8E8mbrwKXoDMs+mnXdUbjW5K4mfQQPwc/nAV/gLQJ/4Ng7whd7Lq85I34l11tw3qkF2Ku+gtqu8q2YU0ce4mQZn/mVXsxzY9ITfGY95X0UCah+XrlOs9QAhVoxwb5UQ1fdforEbeSga9"
        var CLIENT_SECRET = "j/mAdkXSMbAmXYi8Lz1jaaoKcfqK1HHA0YkehjjSRB6Xi54N44e/4WTxxKTLosP1bcJs2vZXQTkLo21YznlevAinN7re6uj05by7Wyn0shrFGvNnmctVBvvZI1UfcQcIGGvNvyEgyVAeANGjromG4mSngqX5b9713/SuUHrpOoeESFVD6d4LRftckVwtl9zkylrs/yMBLCFAfDapvopg4a8GSt2y/P6z806WZTzvOGIxHDbHA/ZkB2djOW1HBLPEdZ10PEfbUK7pahM3s6GvXoQP2qpnV5U+ql4vKz6xrpZW842yboKsr6EhfQc1tQqMQOT3eNJMUKEStBXHVXfcjFeLQw8OSAUFvLkHCkzGGkM="
        var PROXY_NUMBER = ""
        var SHOW_CONTACTS = "false"
        var PRODUCT = "eLog"
        var CURRENT_USER =""
        var FRIENDLY_NAME = ""
        var TWILIO_TOKEN = ""
        var AUTH_TOKEN = ""
        var FULL_NAME =""
        var IS_AADENABLED = "false"
        var TIMESTAMP = ""
        var IS_STANDALONE = "true"
        var USER_SMS_ALERT = ""
        var SHOW_DESIGNATION = ""
        var SHOW_DEPARTMENT = ""
        var EMAIL = ""
        var MOBILENUMBER = ""

        val CONTACTS1 =
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
                     "countryCode":""
                  },
                  {
                     "name":"Himanshu Mahajan",
                     "number":"+919175346961",
                     "customerName":"",
                     "initials":"",
                     "designation":"Supervisor",
                     "department":"Service",
                     "customer":"Customer",
                     "countryCode":""
                  }
            ]
       } """.trimIndent()

        val WEBUSERS1 = """{
            "webUser":[
            {
                "name":"Ankush Belorkar",
                "userName":"abelorkar@e-emphasys.com",
                "initials":"",
                "designation":"Technician",
                "department":"",
                "customer":"",
                "countryCode":""
            },
            {
                "name":"Hardik Kothari",
                "userName":"hkothari",
                "initials":"",
                "designation":"Supervisor",
                "department":"",
                "customer":"",
                "countryCode":""
            }
            ]
        }
        """.trimIndent()

        val CONTACTS = ""
        val WEBUSERS = ""

    }
}