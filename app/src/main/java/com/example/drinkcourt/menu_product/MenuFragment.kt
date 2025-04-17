package com.example.drinkcourt.menu_product

import android.app.ProgressDialog
import android.content.ContentValues
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.drinkcourt.R
import com.example.drinkcourt.adapter.ListItemAdapter
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.FragmentMenuBinding
import com.example.drinkcourt.db.DbContract
import com.example.drinkcourt.db.DbQuery
import com.example.drinkcourt.model.Items
import com.example.drinkcourt.mvvm.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MenuFragment : Fragment() {

    private lateinit var listItemAdapter: ListItemAdapter

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainViewModel: MainViewModel

    private lateinit var connect: Connect
    private lateinit var dbQuery: DbQuery
    private lateinit var listMenu: MutableList<Items>

    companion object {
        const val ARG_SECTION_NUMBER = "section_number"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbQuery = DbQuery.getInstance(requireContext())
        connect = Connect()

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        showList()
    }

    private fun showList(){
        binding.shimmerRecyclerView.layoutManager = GridLayoutManager(context, requireView().resources.getInteger(R.integer.number_of_grid_items))
        binding.shimmerRecyclerView.setHasFixedSize(false)
        binding.shimmerRecyclerView.showShimmerAdapter()
        val index = arguments?.getInt(ARG_SECTION_NUMBER, 0)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO){
                loadMenu(index!!.toInt())
            }
            when (result){
                "Berhasil" -> {
                    binding.shimmerRecyclerView.hideShimmerAdapter()
                    listItemAdapter = ListItemAdapter(listMenu)
                    binding.shimmerRecyclerView.adapter = listItemAdapter
                    listItemAdapter.setOnItemClickCallback(object : ListItemAdapter.OnItemClickCallback{
                        override fun onItemClicked(items: Items) {
                            val progressBar = ProgressDialog(context)
                            progressBar.setMessage("Mohon tunggu..")
                            progressBar.setCanceledOnTouchOutside(false)
                            progressBar.show()

                            lifecycleScope.launch {
                                val result2 = withContext(Dispatchers.IO){
                                    menuStatus(items.id!!.toInt())
                                }
                                when (result2) {
                                    "0" -> {
                                        progressBar.dismiss()
                                        val cv = ContentValues()
                                        cv.put(DbContract.baseColumns.ID, UUID.randomUUID().toString())
                                        cv.put(DbContract.baseColumns.ID_PRODUCT, items.id)
                                        cv.put(DbContract.baseColumns.MENU_ID_PRODUCT, items.menuId)
                                        cv.put(DbContract.baseColumns.NAME_PRODUCT, items.nama)
                                        cv.put(DbContract.baseColumns.HARGA_PRODUCT, items.harga)
                                        cv.put(DbContract.baseColumns.QUANTY_PRODUCT, 1)
                                        cv.put(DbContract.baseColumns.NOTE_PRODUCT, "")
                                        cv.put(DbContract.baseColumns.TOTAL_PRODUCT, items.harga)
                                        cv.put(DbContract.baseColumns.DATE_PRODUCT, formateDate())
                                        lifecycleScope.launch{
                                            val resultCek = withContext(Dispatchers.IO){
                                                dbQuery.cekQueryForPesanan(items.id.toString())
                                            }

                                            if (resultCek == "1"){

                                                val resultInsert = withContext(Dispatchers.IO){
                                                    dbQuery.insert(cv)
                                                }

                                                if (resultInsert > 0){

                                                    val post = withContext(Dispatchers.IO){
                                                        dbQuery.getItemsProduc()
                                                    }

                                                    mainViewModel.orderList.postValue(post)

                                                }else{
                                                    Snackbar.make(binding.root, "Gagal Ditambahkan!", Snackbar.LENGTH_SHORT).show()
                                                }
                                            }else{
                                                val resultGetUid = withContext(Dispatchers.IO){
                                                    dbQuery.queryGetUID(items.id.toString())
                                                }
                                                val resultUpdate = withContext(Dispatchers.IO){

                                                    val qty = dbQuery.queryGetQty(resultGetUid)

                                                    val cvUpdate = ContentValues()
                                                    cvUpdate.put(DbContract.baseColumns.QUANTY_PRODUCT, qty + 1)
                                                    cvUpdate.put(DbContract.baseColumns.TOTAL_PRODUCT, (qty + 1) * items.harga!!)

                                                    return@withContext dbQuery.update(resultGetUid, cvUpdate)
                                                }

                                                if (resultUpdate > 0){
                                                    val post = withContext(Dispatchers.IO){
                                                        dbQuery.getItemsProduc()
                                                    }
                                                    mainViewModel.orderList.postValue(post)
                                                }
                                            }
                                        }
                                    }
                                    "1" -> {
                                        progressBar.dismiss()
                                        Snackbar.make(binding.root, "SOLD OUT", Snackbar.LENGTH_SHORT).show()
                                        showList()
                                    }
                                    else -> {
                                        progressBar.dismiss()
                                        Snackbar.make(binding.root, result2, Snackbar.LENGTH_SHORT).show()
                                        showList()
                                    }
                                }
                            }
                        }
                    })
                }
                "Gagal" -> {
                    Snackbar.make(binding.root, "Gagal menampilkan menu!!", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    Snackbar.make(binding.root, result, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formateDate():String{
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun menuStatus(id: Int): String{
        val conn = connect.connection(requireContext())
        var result = ""
        if (conn != null){
            return try {
                val query = "EXEC USP_J_MenuStatus_Disable @MenuID = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                val rs = st.executeQuery()
                while (rs.next()){
                    result = rs.getString("TodayDisabled")
                }
                result
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return result
    }

    private fun loadMenu(position: Int): String{
        listMenu = mutableListOf()
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "EXEC USP_J_Mobile_QueryStatic @Action = 'Menu', @CatId = ? "
                val st = conn.prepareStatement(query)
                st.setInt(1, position)
                val rs = st.executeQuery()
                listMenu.clear()
                while (rs.next()){
                    if (rs.getInt("HideInOrder") != 1){
                        val produc = Items()
                        produc.id = rs.getInt("MenuID")
                        produc.menuId = rs.getString("MenuCode")
                        produc.nama = rs.getString("MenuName")
                        produc.harga = rs.getInt("Price")
                        produc.todayDisable = rs.getInt("TodayDisabled")
                        listMenu.add(produc)
                    }
                }
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }
}