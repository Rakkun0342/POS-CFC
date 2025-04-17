package com.example.drinkcourt.ui

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.toHalf
import androidx.lifecycle.lifecycleScope
import com.example.drinkcourt.R
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.ActivityLoginBinding
import com.example.drinkcourt.utils.ConfigIP
import com.example.drinkcourt.utils.SessionLogin
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.codec.digest.DigestUtils
import java.math.BigInteger
import java.security.MessageDigest
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var session: SessionLogin
    private lateinit var configIP: ConfigIP
    private lateinit var connect: Connect
    private lateinit var binding: ActivityLoginBinding

    private var user = ""
    private var atLog = ""
    private var userId = 0
    private var mobileRules = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        session = SessionLogin(this)
        configIP = ConfigIP(this)
        connect = Connect()

        setUpCodeInput()
        setPermission()

        if (session.isLoggedIn()){
            if (session.getMobileRole().contains("PRODUCTION")){
                val intent = Intent(this@LoginActivity, ProductionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }else{
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            finishAffinity()
        }
        binding.btnLogIn.setOnClickListener(this)
        binding.btnSettingConfig.setOnClickListener(this)
    }

    private fun setPermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 101)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), 102)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 103)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 104)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Is Denied",Snackbar.LENGTH_SHORT).show()
                }
            }

            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Admin Is Denied",Snackbar.LENGTH_SHORT).show()
                }
            }

            103 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Connect Is Denied",Snackbar.LENGTH_SHORT).show()
                }
            }

            104 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Scan Is Denied",Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setUpCode():String {
        val code = reverseWordsAndChars2(binding.et1.text.toString() + binding.et2.text.toString() + binding.et3.text.toString() + binding.et4.text.toString() + binding.et5.text.toString() + binding.et6.text.toString())
        Log.e("cek guid", code)
        val conn = Connect().connection(this)
        if (conn != null) {
            return try {
                val query = "EXEC USP_J_User_Query @UserName = ?, @ByPIN=1"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, code)
                val resultSet = preparedStatement.executeQuery()
                while (resultSet.next()){
                    userId = resultSet.getInt("UserID")
                    atLog = resultSet.getString("ATLog")
                    user = resultSet.getString("UserName")
                    mobileRules = resultSet.getString("MobileRole")
                }
                "Berhasil"
            } catch (e: SQLException) {
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun setUpCodeInput(){
        binding.et1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et2.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.et2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et1.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et3.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.et3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et2.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et4.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.et4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et3.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et5.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.et5.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et4.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et6.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.et6.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et5.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnLogIn -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Mohon Tunggu")
                progressDialog.setMessage("Sedang konfirmasi pin...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val getDate = SimpleDateFormat("dd/MMM/yyyy hh:mm").format(Date())

                val loginTop = "<size=12><b>LOGIN DATE $getDate</b>"
                val onChangeLog = "<size=10>" +
                        "<br><b>Id</b>: <u>${Build.ID}" +
                        "<br><b>DeviceId</b>: <u>${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}" +
                        "<br><b>Model</b>: <u>${Build.MODEL}</u>"

                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO){
                        setUpCode()
                    }
                    val audit = if (atLog != ""){
                        "$loginTop$onChangeLog<br><br>$atLog"
                    }else{
                        loginTop + onChangeLog
                    }
                    Log.e("cek hasil", result)
                    Log.e("cek user", user)
                    when(result){
                        "Berhasil" -> {
                            if (user != ""){
                                val result2 = withContext(Dispatchers.IO){
                                    checkLogin(userId)
                                }
                                when (result2){
                                    "0" -> {
                                        withContext(Dispatchers.IO){
                                            openLogin(userId, audit)
                                        }

                                        progressDialog.dismiss()
                                        session.createLoginSession(userId, user, mobileRules)
                                        finishAffinity()

                                        if (mobileRules == "PRODUCTION"){
                                            val intent = Intent(this@LoginActivity, ProductionActivity::class.java)
                                            startActivity(intent)
                                        }else{
                                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                            startActivity(intent)
                                        }
                                    }
                                    "1" -> {
                                        progressDialog.dismiss()
                                        Toast.makeText(this@LoginActivity, "Akun kamu sudah login di perangkat lain", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        progressDialog.dismiss()
                                        Toast.makeText(this@LoginActivity, result2, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }else{
                                progressDialog.dismiss()
                                Snackbar.make(binding.root, "Pin kamu salah", Snackbar.LENGTH_LONG).show()
                            }
                        }
                        "Gagal" -> {
                            progressDialog.dismiss()
                            Snackbar.make(binding.root, "Tidak terhubung ke server", Snackbar.LENGTH_SHORT).show()
                        }
                        else -> {
                            progressDialog.dismiss()
                            Snackbar.make(binding.root, result, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            R.id.btnSettingConfig -> {
                val builder = AlertDialog.Builder(this)
                val dialog_ = layoutInflater.inflate(R.layout.confirm_ip, null)
                val pinPass = dialog_.findViewById<EditText>(R.id.etPassDialog)
                builder.setTitle("Masukan Pin")
                builder.setView(dialog_)
                builder.setPositiveButton("Ya"){_,_,->
                    val pass = pinPass.text.toString()
                    if(pass == "557788"){
                        val intent = Intent(this, ConfigIpActivity::class.java)
                        startActivity(intent)
                    }else{
                        Toast.makeText(this, "Pin Salah", Toast.LENGTH_LONG).show()
                    }
                }
                builder.setNegativeButton("Tidak"){p,_->
                    p.dismiss()
                }
                builder.create().show()
            }
        }
    }

    private fun reverseWordsAndChars2(input: String): String {
        val parts = formatAsGuid(md5(input)).split("-")
        val reversedFirstThreeWords = parts.take(3).reversed().joinToString("-").reversed()
        val restOfWords = parts.drop(3).joinToString("-")
        return "${reverseEveryTwoChars(reversedFirstThreeWords)}-$restOfWords"
    }

    private fun reverseEveryTwoChars(input: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < input.length) {
            if (input[i] == '-') {
                result.append(input[i])
                i++
            } else {
                if (i + 1 < input.length) {
                    result.append(input[i + 1]).append(input[i])
                } else {
                    result.append(input[i])
                }
                i += 2
            }
        }
        return result.toString()
    }

    private fun formatAsGuid(hash: String): String {
        return "${hash.substring(0, 8)}-${hash.substring(8, 12)}-${hash.substring(12, 16)}-${hash.substring(16, 20)}-${hash.substring(20)}"
    }

    private fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun checkLogin(id: Int): String{
        val conn = connect.connection(this)
        var result = ""
        if (conn != null){
            return try {
                val query = "EXEC USP_J_User_IsLogin @Action = 'Check', @UserId = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                val rs = st.executeQuery()
                while (rs.next()){
                    result = rs.getString("IsLogin")
                }
                result
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return result
    }

    private fun openLogin(id: Int, atLog: String){
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "EXEC USP_J_User_IsLogin @Action = 'Open', @UserId = ?, @ATLog = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                st.setString(2, atLog)
                st.execute()
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
    }
}