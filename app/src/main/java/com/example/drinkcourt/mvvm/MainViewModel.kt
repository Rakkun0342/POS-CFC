package com.example.drinkcourt.mvvm

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.model.Checker
import com.example.drinkcourt.model.History
import com.example.drinkcourt.model.Produc
import com.example.drinkcourt.model.Transaksi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.sql.SQLException

class MainViewModel() : ViewModel() {
    val orderList = MutableLiveData<MutableList<Produc>>()
    private val listTransaksi = MutableLiveData<MutableList<Transaksi>>()
    private val listHistory = MutableLiveData<MutableList<History>>()
    private val listProduction = MutableLiveData<MutableList<Checker>>()

    fun getOrders(): LiveData<MutableList<Produc>> {
        return orderList
    }

    fun getDataHistory():LiveData<MutableList<History>> {
        return listHistory
    }

    fun getDataChecker():LiveData<MutableList<Transaksi>> {
        return listTransaksi
    }

    fun getDataProduction():LiveData<MutableList<Checker>> {
        return listProduction
    }

    fun loadItemsForChecker(context: Context, group: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val conn = Connect().connection(context)
            val list: MutableList<Transaksi> = mutableListOf()
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForChecker @CatFlags = ?"
                    val preparedStatement = conn.prepareStatement(query)
                    preparedStatement.setString(1, group)
                    val rs = preparedStatement.executeQuery()
                    list.clear()
                    while (rs.next()){
                        val transaksi = Transaksi()
                        transaksi.noMeja = rs.getString("TableCode")
                        transaksi.date = rs.getString("TableLastMod")
                        transaksi.detail = rs.getString("DetailJson")
                        transaksi.salesId = rs.getInt("SalesID")
                        transaksi.group = rs.getString("GroupName")
                        transaksi.lastMod = rs.getString("LastModBy")
                        list.add(transaksi)
                    }
                    withContext(Dispatchers.Main) {
                        listTransaksi.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadItemsForCheckerSearch(context: Context, key: String, group: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val list: MutableList<Transaksi> = mutableListOf()
            val conn = Connect().connection(context)
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForChecker @Phrase = ?, @CatFlags = ?"
                    val preparedStatement = conn.prepareStatement(query)
                    preparedStatement.setString(1, key)
                    preparedStatement.setString(2, group)
                    val rs = preparedStatement.executeQuery()
                    list.clear()
                    while (rs.next()){
                        val transaksi = Transaksi()
                        transaksi.noMeja = rs.getString("TableCode")
                        transaksi.date = rs.getString("TableLastMod")
                        transaksi.detail = rs.getString("DetailJson")
                        transaksi.salesId = rs.getInt("SalesID")
                        transaksi.group = rs.getString("GroupName")
                        transaksi.lastMod = rs.getString("LastModBy")
                        list.add(transaksi)
                    }
                    withContext(Dispatchers.Main) {
                        listTransaksi.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadItemsForHistory(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val conn = Connect().connection(context)
            val list: MutableList<History> = mutableListOf()
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForHistory"
                    val preparedStatement = conn.createStatement()
                    val rs = preparedStatement.executeQuery(query)
                    list.clear()
                    while (rs.next()){
                        val transaksi = History()
                        transaksi.noMeja = rs.getString("TableCode")
                        transaksi.date = rs.getString("TableLastMod")
                        transaksi.detail = rs.getString("DetailJson")
                        transaksi.salesId = rs.getInt("SalesID")
                        transaksi.group = rs.getString("GroupName")
                        transaksi.lastMod = rs.getString("LastModBy")
                        transaksi.recorded = rs.getString("RecordedDateTime")
                        list.add(transaksi)
                    }
                    withContext(Dispatchers.Main) {
                        listHistory.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadItemsForHistorySearch(context: Context, key: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val list: MutableList<History> = mutableListOf()
            val conn = Connect().connection(context)
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForHistory @Phrase = ?"
                    val preparedStatement = conn.prepareStatement(query)
                    preparedStatement.setString(1, key)
                    val rs = preparedStatement.executeQuery()
                    list.clear()
                    while (rs.next()){
                        val transaksi = History()
                        transaksi.noMeja = rs.getString("TableCode")
                        transaksi.date = rs.getString("TableLastMod")
                        transaksi.detail = rs.getString("DetailJson")
                        transaksi.salesId = rs.getInt("SalesID")
                        transaksi.group = rs.getString("GroupName")
                        transaksi.lastMod = rs.getString("LastModBy")
                        transaksi.recorded = rs.getString("RecordedDateTime")
                        list.add(transaksi)
                    }
                    withContext(Dispatchers.Main) {
                        listHistory.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadItemsProduction(context: Context, group: String): String{
        GlobalScope.launch(Dispatchers.IO) {
            val listCheck: MutableList<Checker> = mutableListOf()
            val conn = Connect().connection(context)
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForProduction @CatFlags = ?"
                    val preparedStatement = conn.prepareStatement(query)
                    preparedStatement.setString(1, group)
                    val rs = preparedStatement.executeQuery()
                    listCheck.clear()
                    while (rs.next()){
                        val checker = Checker()
                        checker.menuIdChecker = rs.getInt("MenuID")
                        checker.menuCodeChecker = rs.getString("MenuCode")
                        checker.menuNameChecker = rs.getString("MenuName")
                        checker.requestChecker = rs.getString("Request")
                        checker.qtyChecker = rs.getInt("Qty")
                        checker.status = rs.getInt("ColorStatus")
                        listCheck.add(checker)
                    }
                    withContext(Dispatchers.Main) {
                        listProduction.value = listCheck
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
        return "Gagal"
    }
}