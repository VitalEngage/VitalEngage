package com.eemphasys.vitalconnectdev.data

import android.content.Context
import androidx.preference.PreferenceManager
import com.eemphasys_enterprise.commonmobilelib.EETLog
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class CredentialStorage(applicationContext: Context) {

    var identity by stringPreference()

    var password by stringPreference()


    var fcmToken by stringPreference()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    private fun stringPreference() = object : ReadWriteProperty<Any?, String> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return sharedPreferences.getString(property.name, "")!!
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            sharedPreferences.edit()
                .putString(property.name, value)
                .apply()
        }
    }

    fun isEmpty(): Boolean = identity.isEmpty() || password.isEmpty()

    fun clearCredentials() {
        EETLog.saveUserJourney(this::class.java.simpleName + "Clear Credentials function ")
        sharedPreferences.edit().clear().apply()
    }

    fun storeCredentials(identity: String, password:String) {
        EETLog.saveUserJourney(this::class.java.simpleName + "store Credentials function ")
        this.identity = identity
        this.password = password
    }
}
