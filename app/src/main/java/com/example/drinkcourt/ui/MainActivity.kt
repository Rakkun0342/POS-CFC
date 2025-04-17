package com.example.drinkcourt.ui

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.drinkcourt.R
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.ActivityMainBinding
import com.example.drinkcourt.fragment.CheckerV2Fragment
import com.example.drinkcourt.fragment.HistoryFragment
import com.example.drinkcourt.fragment.OrderFragment
import com.example.drinkcourt.fragment.TransaksiFragment
import com.example.drinkcourt.utils.SessionLogin
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var sessionLogin: SessionLogin
    private lateinit var binding: ActivityMainBinding
    private lateinit var connect: Connect

    companion object{
        private const val REQUEST_ENABLE_BT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        connect = Connect()

        sessionLogin = SessionLogin(this)
        setSupportActionBar(binding.toolbar)

        if (!sessionLogin.isLoggedIn()){
            sessionLogin.noLogin()
            finishAffinity()
        }

        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.tvNameNav).text = sessionLogin.getUserLogin()
        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.tvJobsNav).text = sessionLogin.getMobileRole()

        binding.navView.setNavigationItemSelectedListener(this)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.open_nav,
            R.string.close_nav
        )

        if (sessionLogin.getMobileRole() == "WAITER"){
            binding.navView.menu.getItem(2).isVisible = false
            binding.navView.menu.getItem(3).isVisible = false
            binding.navView.menu.getItem(4).isVisible = false
        }else if (sessionLogin.getMobileRole().contains("CHECKER")){
            binding.navView.menu.getItem(0).isVisible = false
            binding.navView.menu.getItem(1).isVisible = false
            binding.navView.menu.getItem(3).isVisible = false
            binding.navView.menu.getItem(4).isVisible = false
        }else if (sessionLogin.getMobileRole() == "VIEWER"){
            binding.navView.menu.getItem(0).isVisible = false
            binding.navView.menu.getItem(1).isVisible = false
            binding.navView.menu.getItem(4).isVisible = false
        }else if (sessionLogin.getMobileRole() == "RUNNER"){
            binding.navView.menu.getItem(0).isVisible = false
            binding.navView.menu.getItem(1).isVisible = false
            binding.navView.menu.getItem(2).isVisible = false
            binding.navView.menu.getItem(3).isVisible = false
            binding.navView.menu.getItem(4).isVisible = false
        }else if (sessionLogin.getMobileRole() == "ADMIN"){
            binding.navView.menu.getItem(0).isVisible = true
            binding.navView.menu.getItem(1).isVisible = true
            binding.navView.menu.getItem(2).isVisible = true
            binding.navView.menu.getItem(3).isVisible = true
            binding.navView.menu.getItem(4).isVisible = true
        }

        Log.e("Hasil menu", binding.navView.menu.getItem(3).title.toString())

        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        if (savedInstanceState == null) {
            if (sessionLogin.getMobileRole() == "WAITER"){
                replaceFragment(OrderFragment(), "BUAT PESANAN")
                binding.navView.setCheckedItem(R.id.nav_orders)
            }else if (sessionLogin.getMobileRole().contains("CHECKER")){
                replaceFragment(CheckerV2Fragment(), "CHECKER")
                binding.navView.setCheckedItem(R.id.nav_checker)
            }else if (sessionLogin.getMobileRole().contains("VIEWER")){
                replaceFragment(CheckerV2Fragment(), "CHECKER")
                binding.navView.setCheckedItem(R.id.nav_checker)
            }else if (sessionLogin.getMobileRole() == "RUNNER"){
                Toast.makeText(this, "You have don't acces!!", Toast.LENGTH_LONG).show()
            }else if (sessionLogin.getMobileRole() == "ADMIN"){
                replaceFragment(HistoryFragment(), "HISTORY")
                binding.navView.setCheckedItem(R.id.nav_history)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 101)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), 102)
            } else {
                checkBluetoothEnabled()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 103)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 104)
            } else {
                checkBluetoothEnabled()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth is denied, Please granted permission", Snackbar.LENGTH_SHORT).show()
                }
            }

            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Admin Is Denied, Please granted permission", Snackbar.LENGTH_SHORT).show()
                }
            }

            103 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Connect Is Denied, Please granted permission", Snackbar.LENGTH_SHORT).show()
                }
            }

            104 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Scan Is Denied, Please granted permission", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Snackbar.make(binding.root, "Bluetooth wajib dihidupkan!!",Snackbar.LENGTH_SHORT).show()
                checkBluetoothEnabled()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
         supportActionBar?.title = title
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_orders -> replaceFragment(OrderFragment(), item.title.toString())
            R.id.nav_transaksi -> replaceFragment(TransaksiFragment(), item.title.toString())
            R.id.nav_history -> replaceFragment(HistoryFragment(), item.title.toString())
            R.id.nav_open -> {
                val progressBar = ProgressDialog(this)
                progressBar.setMessage("Mohon tunggu..")
                progressBar.show()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO){
                        closeSales()
                    }
                    progressBar.dismiss()
                }
            }
            R.id.nav_checker -> replaceFragment(CheckerV2Fragment(), item.title.toString())
            R.id.nav_logout -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Apakah anda mau logout?")
                builder.setPositiveButton("Ya"){_,_->
                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO){
                            closeLogin(sessionLogin.getUserIdLogin())
                        }
                        when (result){
                            "berhasil" -> {
                                Log.e("hasil", result)
                                sessionLogin.logoutUser()
                                finishAffinity()
                            }
                            "gagal" -> {
                                Toast.makeText(this@MainActivity, "Gagal Logout", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                builder.setNegativeButton("Batal"){p,_->
                    p.dismiss()
                }
                builder.create().show()
            }
        }
        item.isChecked = true
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun closeSales(){
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "EXEC USP_J_Sales_CheckedOpen @Action = 'CloseAll'"
                val st = conn.prepareStatement(query)
                st.execute()
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
    }

    private fun closeLogin(id: Int): String{
        val conn = connect.connection(this)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_User_IsLogin @Action = 'Close', @UserId = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                st.execute()
                "berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "gagal"
    }
}