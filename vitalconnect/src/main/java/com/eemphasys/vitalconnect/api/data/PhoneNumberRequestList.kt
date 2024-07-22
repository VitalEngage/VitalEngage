package com.eemphasys.vitalconnect.api.data



data class PhoneNumberRequestList(
//    @SerializedName("phoneNumber")
//    @JsonAdapter(PhoneNumberListSerializer::class)
    val mobileNumbers: List<String>

)

//class PhoneNumberListSerializer : JsonSerializer<List<String>> {
//    override fun serialize(
//        src: List<String>?,
//        typeOfSrc: Type?,
//        context: JsonSerializationContext?
//    ): JsonElement {
//        val jsonArray = JsonArray()
//        src?.forEach { phoneNumber ->
//            jsonArray.add(JsonPrimitive("\"$phoneNumber\""))
//        }
//        return jsonArray
//    }
//}