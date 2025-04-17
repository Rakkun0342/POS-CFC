package com.example.drinkcourt.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.drinkcourt.R
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.ActivityConfigIpBinding
import com.example.drinkcourt.databinding.ActivityMainBinding
import com.example.drinkcourt.utils.ConfigIP
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class ConfigIpActivity : AppCompatActivity() {

    private lateinit var configIP: ConfigIP
    private lateinit var connect: Connect
    private  lateinit var binding: ActivityConfigIpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigIpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        configIP = ConfigIP(this)
        connect = Connect()

        binding.btnConnect.setOnClickListener{
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    configIP.createConnection(binding.etIP.text.toString(), binding.etUser.text.toString(), binding.etPass.text.toString(), binding.etDatabase.text.toString())
                }
                val conn = connect.connection(this@ConfigIpActivity)
                if (conn != null){
                    finish()
                }else{
                    Snackbar.make(binding.root, "Connection if failed, please check your connection!!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}