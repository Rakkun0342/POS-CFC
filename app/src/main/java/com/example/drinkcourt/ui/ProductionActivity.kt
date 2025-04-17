package com.example.drinkcourt.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.drinkcourt.adapter.ListCheckerAdapter
import com.example.drinkcourt.adapter.ListProductionAdapter
import com.example.drinkcourt.adapter.ListTransaksiAdapter
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.ActivityProductionBinding
import com.example.drinkcourt.model.Checker
import com.example.drinkcourt.model.Transaksi
import com.example.drinkcourt.mvvm.MainViewModel
import com.example.drinkcourt.utils.SessionLogin
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductionActivity : AppCompatActivity() {

    private lateinit var sessionLogin: SessionLogin
    private  lateinit var binding: ActivityProductionBinding

    private var connect: Connect = Connect()
    private var listChecker : MutableList<Checker> = mutableListOf()
    private lateinit var listCheckerAdapter: ListCheckerAdapter

    private lateinit var mainViewModel: MainViewModel

    private var getUserLogin = ""


    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            mainViewModel.loadItemsProduction(this@ProductionActivity, getUserLogin)
            showList()
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sessionLogin = SessionLogin(this)

        if (!sessionLogin.isLoggedIn()){
            sessionLogin.noLogin()
            finishAffinity()
        }

        binding.shimmerRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.shimmerRecyclerView.setHasFixedSize(false)
        binding.shimmerRecyclerView.showShimmerAdapter()

        binding.includeLayout.root.visibility = View.VISIBLE
        binding.shimmerRecyclerView.visibility = View.GONE

        listCheckerAdapter = ListCheckerAdapter()

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            getUserLogin = if (sessionLogin.getMobileRole() == "PRODUCTION" || sessionLogin.getMobileRole() == "ADMIN"){
                ""
            }else{
                sessionLogin.getMobileRole().replace("PRODUCTION_", "")
            }

            withContext(Dispatchers.IO){
                mainViewModel.loadItemsProduction(this@ProductionActivity, getUserLogin)
            }
        }

        mainViewModel.getDataProduction().observe(this, Observer { items ->
            if (items != null){
                listChecker.clear()
                listChecker.addAll(items)
                listCheckerAdapter.setData(listChecker)
                showList()
            }
        })

        binding.shimmerRecyclerView.adapter = listCheckerAdapter

        binding.refreshPesanan.setOnRefreshListener {
            mainViewModel.loadItemsProduction(this, getUserLogin)
            showList()
        }

        showList()
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, 15000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun showList(){
        if (listChecker.size != 0){
            binding.includeLayout.root.visibility = View.GONE
            binding.shimmerRecyclerView.visibility = View.VISIBLE
            binding.refreshPesanan.setRefreshing(false)
            binding.shimmerRecyclerView.hideShimmerAdapter()
        }else{
            binding.includeLayout.root.visibility = View.VISIBLE
            binding.shimmerRecyclerView.visibility = View.GONE
        }
    }
}