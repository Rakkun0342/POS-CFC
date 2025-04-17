package com.example.drinkcourt.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.drinkcourt.ui.LoginActivity

class ConfigIP(var context: Context) {
    var pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, 0)
    private var editor: SharedPreferences.Editor = pref.edit()

    fun createConnection(ip: String, user:String, pas: String, db:String) {
        editor.putString(KEY_IP, ip)
        editor.putString(KEY_USER, user)
        editor.putString(KEY_PAS, pas)
        editor.putString(KEY_DB, db)
        editor.commit()
    }

    fun getiP(): String = pref.getString(KEY_IP, "").toString()
    fun getUser(): String = pref.getString(KEY_USER, "").toString()
    fun getPas(): String = pref.getString(KEY_PAS, "").toString()
    fun getDb(): String = pref.getString(KEY_DB, "").toString()

    companion object {
        private const val PREF_NAME = "CONFIG"
        const val KEY_IP = "IP"
        const val KEY_USER = "USER"
        const val KEY_PAS = "PAS"
        const val KEY_DB = "DB"
    }

}