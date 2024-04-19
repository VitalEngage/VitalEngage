package com.eemphasys.vitalconnectdev.data

class LoginConstants   {
    companion object{

        var BASE_URL = "https://xappsweb.e-emphasys.com/VitalConnect/NewWebApi/"
        /*var TENANT_CODE = "VitalConnectDev"
        var CLIENT_ID = "169JNr3JkBOMBopDincd2desazfFO+lQMtt8rdKJqlRSgSINwFlO17STwfJuYa3BatHv7t2NnnOh1B36KspC68oEkZZU1x3dJQhtsXzLqXa7LC2zMlOMcd5Q74UNeLxG56I7f3wePyWaFzrXmoweTsLuRALkYgLhLMR5ruw3KG+PpwprhOrLh2psdIkkg5wB1A+J13AJ9gd0ed7EFgrbVDh4q5HlFy9nUXmWbBhZ0RuqC0+5BuxmEwCRiwnlvyJiu6+hc296OYKGI+etIpnj2Z+mWdCmlFkN+qxnulRGeIWppIOQLRFssK54zCS5kqJVtdNEBPD/plGFxKX8Ij5IUN4+id8Pey2QejP82N7gJSJIzwm6NrT3sRfmNx09dlQ1"
        var CLIENT_SECRET = "cBTnK1QObPwWoGbY0Fv6doJaRnmwvnDcW0fWHtx7NDqKuQ0RQaKX6cx2kDE3xbgfylzGnCQjQirer/4Yy98nyzvWhiejUlDPzuWhYAYGBqLO5y1cfBIy3OtkRk0dTRu1Kf8LAO4XSV302Z+rKwvtHt8oe2sU5tkjC8pXUsRR3vk/tNHXd6WoRPEa7gpoueSflzXhzaMc+0CwgDYtrHmgYNYvajZn1SdAPEdB1jtgvMacr/xQQNQa8fquiJGCSpQykN3Rc07UXH7fdfUJtjSv1H1KVMC9a73bsoxI1j0SBtG0ObB73KxU+yoeoZ+uHxRzRmuf4CBNnA9jJa1QD7yWOUQdiaO3qFGLmbP695fDeik="
        var PROXY_NUMBER = "+16592993578"*/
        var TENANT_CODE = "VitalConnectNonAzureAd"
        var CLIENT_ID = "169JNr3JkBOMBopDincd2desazfFO+lQMtt8rdKJqlRSgSINwFlO17STwfJuYa3BatHv7t2NnnOh1B36KspC68oEkZZU1x3dJQhtsXzLqXa7LC2zMlOMcd5Q74UNeLxGNB9nejUoGpEdRmrNycXdtWf4IRY6Rn2z0JZ1/VwvIx89K46+vIW9EfyH2oHj7EHDg0w3vBMxYA591rPt/k+YT4u38/JT5TGaQTkg8Nlu7YTgHcAOV4nDEUTSNcAKvLkujeQJjajvsx+tCMTLzgm1HE5kZ6JRz9TnOwCTVHQy/9yvX3/2TbjIersyq3oSF3g0a5MgQZI9/b8VDRtax0doEJVPsrMKtNPxAlfKrKmzNrVsReyXJwVolsWDI3h2QCoVPM94Q8z5vJGObEpiuBaPI0qb7gQ3gOcgleSmjTjuo88eMdCsfI1XjZeLathRf9A6RkFIrboqLAJB6SkOqOAWbQVsM+YcLqMsW5YX4yy6RNhxcvB7pwlrZpAzLHMsI1vg"
        var CLIENT_SECRET = "cBTnK1QObPwWoGbY0Fv6doJaRnmwvnDcW0fWHtx7NDrwb1GF7oDhtFjtV6JBFXQ5xDb4WJ2ln9S7I4X7TpKH7L+LZ3Khs1zQO29ubgtX/J8C9fQI9b7kvaOPJJOpOquu8O/4RH1zA1A3qNMvrMtvargWPJzxiEBrcF34pk2j32doR8K8gBANerOG1zWpyDTIReOFVD5AUwdWp96xXlDEcM/SuHtrG9tMN1ookxnAC0bZ5fIMB5pDy5frXOSu+dkbC8JixVFGh3KlP3G1fu5dIAjAvHkqC5JRxYZig1MRVTxG3HatjFBBxlxkomVXjCihcvN1GWPzVHtw4MXJ8G/vtxcj/4dBUJ9L3T3QKWpqKwwoGrWqJkNOeOfw+b1OQBJWuLZ9WYa+Cz1dTp2izK7x4MBkcYLnK82iTWSi3fTK+gTyEZeiyp+JSDwPD2+DSsEBaWh2mPgcSiKVUPBdWFJzHw=="
        var PROXY_NUMBER = "+19208755699"
        var PRODUCT = "eLog"
        var CURRENT_USER =""
        var FRIENDLY_NAME = ""
        var TWILIO_TOKEN = ""
        var AUTH_TOKEN = ""
        var FULL_NAME =""

        val CONTACTS =
            """{
            "contacts" : 
            [
                  {
                     "name":"Ankush Belorkar",
                     "number":"+919422855735",
                     "customerName":"",
                     "customField1":"",
                     "customField2":"",
                     "customField3":"",
                     "customField4":""
                  },
                  {
                     "name":"Himanshu Mahajan",
                     "number":"+919175346961",
                     "customerName":"",
                     "customField1":"",
                     "customField2":"",
                     "customField3":"",
                     "customField4":""
                  }
            ]
       } """.trimIndent()

        val WEBUSERS = """{
            "webUser":[
            {
                "name":"Ankush Belorkar",
                "userName":"abelorkar@e-emphasys.com",
                "customField1":"",
                "customField2":"",
                "customField3":"",
                "customField4":""
            },
            {
                "name":"TestUser qb",
                "userName":"qb",
                "customField1":"",
                "customField2":"",
                "customField3":"",
                "customField4":""
            }
            ]
        }
        """.trimIndent()

        val CONTACTS1 = ""
        val WEBUSERS1 = ""

    }
}