package com.example.drinkcourt.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.drinkcourt.ui.LoginActivity

class SessionLogin(var context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    var PRIVATE_MODE = 0

    fun createLoginSession(userId: Int, nama: String, mobile:String) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_NAMA, nama)
        editor.putInt(KEY_ID, userId)
        editor.putString(KEY_ROLE, mobile)
        editor.commit()
    }

    fun savePrinter(printer: String){
        editor.putString(KEY_PRINTER, printer)
        editor.commit()
    }

    fun noLogin() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun logoutUser() {
        editor.clear()
        editor.commit()
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isLoggedIn(): Boolean = pref.getBoolean(IS_LOGIN, false)
    fun getUserLogin(): String = pref.getString(KEY_NAMA, "").toString()
    fun getUserIdLogin(): Int = pref.getInt(KEY_ID, 0)
    fun getMobileRole(): String = pref.getString(KEY_ROLE, "").toString()
    fun getPrinterSave(): String = pref.getString(KEY_PRINTER, "").toString()

    companion object {
        private const val PREF_NAME = "Prefrence"
        private const val IS_LOGIN = "IsLoggedIn"
        const val KEY_NAMA = "NAMA"
        const val KEY_ID = "ID"
        const val KEY_ROLE = "ROLE"
        const val KEY_PRINTER = "Printer"
    }

    init {
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }
}