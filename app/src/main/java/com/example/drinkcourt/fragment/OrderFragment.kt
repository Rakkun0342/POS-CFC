package com.example.drinkcourt.fragment

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Binder
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.VISIBLE
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.marginStart
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.drinkcourt.R
import com.example.drinkcourt.adapter.GvAdapter
import com.example.drinkcourt.adapter.OrderProductAdapter
import com.example.drinkcourt.adapter.PagerAdapter
import com.example.drinkcourt.async.AsyncBluetoothEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrinter
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.FragmentOrderBinding
import com.example.drinkcourt.db.DbContract
import com.example.drinkcourt.db.DbQuery
import com.example.drinkcourt.model.GroupMenu
import com.example.drinkcourt.model.Produc
import com.example.drinkcourt.mvvm.MainViewModel
import com.example.drinkcourt.utils.SessionLogin
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class OrderFragment : Fragment() {

    private var _binding:FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var listGroupMenu: MutableList<GroupMenu>
    private lateinit var listTableId: MutableList<String>
    private lateinit var listTable: MutableList<String>
    private var selectTable = ""
    private var selectTableName = ""
    private var arrayMetod: ArrayList<String> = arrayListOf()

    private lateinit var orderProductAdapter: OrderProductAdapter
    private lateinit var connect: Connect
    private lateinit var dbQuery: DbQuery
    private lateinit var builder: AlertDialog.Builder
    private lateinit var sessionLogin: SessionLogin

    private lateinit var mainViewModel: MainViewModel

    private var selectedDevice : BluetoothConnection? = null

    private lateinit var tvSelectedDevice: TextView

    companion object{
        private const val REQUEST_ENABLE_BT = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbQuery = DbQuery.getInstance(requireContext())
        sessionLogin = SessionLogin(requireContext())
        connect = Connect()

        setUpPager()
        setUpPesanan()

        lifecycleScope.launch(Dispatchers.IO) {
            loadPayment()
        }

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        mainViewModel.getOrders().observe(viewLifecycleOwner, Observer { orders ->
            Log.e(context.toString(), orders.toString())
            binding.includeLayout.root.visibility = View.GONE
            binding.clPesanan.visibility = View.VISIBLE
            showList(orders)
        })

        binding.btnVoid.setOnClickListener{
            builder = AlertDialog.Builder(context)
            builder.setTitle("Apakah anda mau menghapus semua daftar pesanan?")
            builder.setPositiveButton("Ya"){_,_->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO){
                        dbQuery.delete()
                    }
                    if (result > 0){
                        setUpPesanan()
                    }
                }
            }
            builder.setNegativeButton("Tidak"){p,_->
                p.dismiss()
            }
            builder.create().show()
        }

        binding.tvPrinterOrders.setOnClickListener {
            checkBluetoothPermissions()
        }

        binding.tvPilihMeja.setOnClickListener {
            val dialogCetak = Dialog(requireContext())
            dialogCetak.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogCetak.setContentView(R.layout.dialog_list_table)

            val listQty = dialogCetak.findViewById<GridView>(R.id.list_view_meja)
            val btnCloseMeja = dialogCetak.findViewById<ImageView>(R.id.btnCloseMeja)

            listQty.numColumns = 5
            val adapter = GvAdapter(listTable, requireContext(), true)
            listQty.adapter = adapter

            listQty.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
                binding.tvPilihMeja.text = listTable[position]
                selectTable = listTableId[position]
                selectTableName = listTable[position]
                dialogCetak.dismiss()
            }

            btnCloseMeja.setOnClickListener {
                dialogCetak.dismiss()
            }

            dialogCetak.show()
            dialogCetak.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialogCetak.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogCetak.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialogCetak.window?.setGravity(Gravity.CENTER)
        }

        binding.btnInput.setOnClickListener {
            val selectMeja = binding.tvPilihMeja.text.toString()
//            Log.e("cek printer", selectTableName.toString())
            if (binding.tvPilihMeja.text.toString() == "PILIH MEJA" || selectedDevice == null){
                Snackbar.make(binding.root, "Pilih meja dan printer terlebih dahulu", Snackbar.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultJson = withContext(Dispatchers.IO){
                        dbQuery.getItemsProduc()
                    }
                    val jsonString = Json.encodeToString(resultJson)

                    val dialogPayment = Dialog(requireContext())
                    dialogPayment.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialogPayment.setContentView(R.layout.dialog_payment)
                    dialogPayment.setCanceledOnTouchOutside(false)

                    val qtyPayment = dialogPayment.findViewById<TextView>(R.id.tvQtyPayment)
                    tvSelectedDevice = dialogPayment.findViewById<TextView>(R.id.tvSelectPrinterPayment)
                    val totalPayment = dialogPayment.findViewById<TextView>(R.id.tvTotalPayment)
                    val spinnerPayment = dialogPayment.findViewById<AppCompatSpinner>(R.id.spinnerPayment)
                    val llCash = dialogPayment.findViewById<LinearLayout>(R.id.llCashPayment)
                    val etCast = dialogPayment.findViewById<EditText>(R.id.etCashPayment)
                    val tvKembalian = dialogPayment.findViewById<TextView>(R.id.tvKembalian)
                    val btnCetakPayment = dialogPayment.findViewById<RelativeLayout>(R.id.btnCetakPayment)
                    val btnCetakUlang = dialogPayment.findViewById<RelativeLayout>(R.id.btnCetakUlangPayment)
                    val btnClosePayment = dialogPayment.findViewById<ImageView>(R.id.btnClosePayment)
                    val imgQris = dialogPayment.findViewById<ImageView>(R.id.imgQris)

                    val totalPay = resultJson.sumOf { it.total!!.toInt() }

                    qtyPayment.text = resultJson.sumOf { it.quanty!!.toInt() }.toString()
                    totalPayment.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalPay)

                    var selectMetod = "TUNAI"
                    val adapterTipe = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayMetod)
                    adapterTipe.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerPayment.adapter = adapterTipe
                    spinnerPayment.setSelection(0)
                    spinnerPayment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                            selectMetod = arrayMetod[position]
                            when (selectMetod){
                                "TUNAI" -> {
                                    llCash.visibility = View.VISIBLE
                                    imgQris.visibility = View.GONE
                                }
                                "QRIS" -> {
                                    llCash.visibility = View.GONE
                                    imgQris.visibility = View.VISIBLE
                                }
                                else -> {
                                    llCash.visibility = View.GONE
                                    imgQris.visibility = View.GONE
                                }
                            }
                        }
                        override fun onNothingSelected(p: AdapterView<*>?) {}
                    }

                    if (selectMetod == "TUNAI"){
                        llCash.visibility = View.VISIBLE
                    }

                    etCast.setText(totalPay.toString())

                    val textWatcher = object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val cashChange = etCast.text.toString().toIntOrNull() ?: 0
                            val resultChange = cashChange - totalPay
                            tvKembalian.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(resultChange)
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    }

                    etCast.addTextChangedListener(textWatcher)

                    var salesIdResult = 0

                    var remarks = ""

                    btnCetakPayment.setOnClickListener{
                        if (selectMetod == "TUNAI" && tvKembalian.text.toString().replace(".","").toInt() < 0 ){
                            Toast.makeText(context, "Kembalian tidak boleh minus!!", Toast.LENGTH_SHORT).show()
                        }else{
                            btnCetakPayment.visibility = View.GONE
                            val progressBarPay = ProgressDialog(context)
                            progressBarPay.setMessage("Sedang mengirim!!")
                            progressBarPay.setCanceledOnTouchOutside(false)
                            progressBarPay.show()
                            lifecycleScope.launch {
                                val resultSend = withContext(Dispatchers.IO){
                                    sendItemsPesanan(jsonString, sessionLogin.getUserLogin(), selectTable, selectMetod)
                                }
                                if (resultSend != 0){
                                    withContext(Dispatchers.IO){
                                        printBluetooth2(selectMeja, resultJson, selectMetod, etCast.text.toString().toInt(), false)
                                        dbQuery.delete()
                                    }
                                    setUpPesanan()
                                    remarks += "${selectMetod} DATE ${getCurrentDate("yyyy-MM-dd HH:mm:ss")},"
                                    salesIdResult = resultSend
                                    binding.tvPilihMeja.text = "PILIH MEJA"
                                    btnCetakUlang.visibility = View.VISIBLE
                                    btnCetakPayment.visibility = View.GONE
                                    tvSelectedDevice.visibility = View.VISIBLE
                                    progressBarPay.dismiss()
                                }else{
                                    progressBarPay.dismiss()
                                    btnCetakPayment.visibility = View.VISIBLE
                                    Toast.makeText(context, "Pesanan gagal dibuat", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    btnCetakUlang.setOnClickListener{
                        if (selectMetod == "TUNAI" && tvKembalian.text.toString().replace(".","").toInt() < 0 ){
                            Toast.makeText(context, "Kembalian tidak boleh minus!!", Toast.LENGTH_SHORT).show()
                        }else {
                            btnCetakPayment.visibility = View.GONE
                            val progressBarPay = ProgressDialog(context)
                            progressBarPay.setMessage("Sedang mengirim!!")
                            progressBarPay.setCanceledOnTouchOutside(false)
                            progressBarPay.show()
                            lifecycleScope.launch {
                                val resultReprint = withContext(Dispatchers.IO){
                                    sendItemsPesananReprint(salesIdResult, sessionLogin.getUserLogin(), selectMetod, remarks)
                                }

                                when(resultReprint){
                                    "Berhasil"->{
                                        withContext(Dispatchers.IO){
                                            printBluetooth2(selectMeja, resultJson, selectMetod, etCast.text.toString().toInt(), true)
                                        }
                                        setUpPesanan()
                                        remarks += "${selectMetod} DATE ${getCurrentDate("yyyy-MM-dd HH:mm:ss")},"
                                        binding.tvPilihMeja.text = "PILIH MEJA"
                                        btnCetakUlang.visibility = View.VISIBLE
                                        btnCetakPayment.visibility = View.GONE
                                        tvSelectedDevice.visibility = View.VISIBLE
                                        progressBarPay.dismiss()
                                    }
                                    "Gagal" -> {
                                        progressBarPay.dismiss()
                                        Toast.makeText(context, "Pesanan gagal dibuat", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        progressBarPay.dismiss()
                                        Toast.makeText(context, resultReprint, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }

                    tvSelectedDevice.setOnClickListener{
                        checkBluetoothPermissions2()
                    }

                    btnClosePayment.setOnClickListener{
                        dialogPayment.dismiss()
                    }

                    dialogPayment.show()
                    dialogPayment.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    dialogPayment.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialogPayment.window?.attributes?.windowAnimations = R.style.DialogAnimation
                    dialogPayment.window?.setGravity(Gravity.CENTER)
                }
            }
        }
    }

    private fun loadPayment(){
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "SELECT * FROM J_Lookup WHERE TName = 'Receipt.PaymentMethod'"
                val preparedStatement = conn.createStatement()
                val rs = preparedStatement.executeQuery(query)
                arrayMetod.clear()
                arrayMetod.add("TUNAI")
                while (rs.next()){
                    arrayMetod.add(rs.getString("Code"))
                }
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
    }

    private fun sendItemsPesanan(json: String, by: String, table: String, method: String):Int{
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "EXEC USP_J_Sales_CreateFromJSON @JSON = ?, @By = ?, @TableId = ?, @PaymentMethod = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, json)
                preparedStatement.setString(2, by)
                preparedStatement.setString(3, table)
                preparedStatement.setString(4, method)
                val rs = preparedStatement.executeQuery()
                while (rs.next()){
                    return rs.getInt("SalesId").toInt()
                }
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
        return 0
    }

    private fun sendItemsPesananReprint(salesId: Int, by: String, method: String, remarks: String):String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_ReprintPayment @SalesID = ?, @By = ?, @PaymentMethod = ?, @Remarks = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, salesId)
                preparedStatement.setString(2, by)
                preparedStatement.setString(3, method)
                preparedStatement.setString(4, remarks)
                preparedStatement.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun setUpPager(){
        lifecycleScope.launch{
            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Mohon tunggu sebentar")
            progressDialog.setMessage("Sedang memuat ulang..")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
            val result = withContext(Dispatchers.IO) {
                loadGroup()
            }
            when (result){
                "Berhasil" -> {
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Log.e("size", listGroupMenu.size.toString())
                        val pagerAdapter = PagerAdapter(requireActivity(), listGroupMenu.size)
                        binding.vpCategory.adapter = pagerAdapter
                        TabLayoutMediator(binding.tabKategory, binding.vpCategory){tab, position ->
                            requireActivity().supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            tab.text = listGroupMenu[position].catName.toString()
                        }.attach()
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

    private fun loadGroup(): String{
        listGroupMenu = mutableListOf<GroupMenu>()
        listTable = mutableListOf()
        listTableId = mutableListOf()
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "SELECT CatID, CatIndex, CatName, CatFlags FROM J_Cat ORDER BY CatID"
                val st = conn.createStatement()
                val rs = st.executeQuery(query)
                listGroupMenu.clear()
                while (rs.next()){
                    val catId = rs.getInt("CatID")
                    val catName = rs.getString("CatName")
                    listGroupMenu.add(GroupMenu(catId,catName))
                }

                val queryTable = "EXEC USP_J_Mobile_QueryStatic @Action = 'Table'"
                val stTable = conn.createStatement()
                val rsTable = stTable.executeQuery(queryTable)
                listTable.clear()
                listTableId.clear()
                while (rsTable.next()){
                    listTable.add(rsTable.getString("TableCode"))
                    listTableId.add(rsTable.getString("TableId"))
                }
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun setUpPesanan(){

        lifecycleScope.launch {
            val cek = withContext(Dispatchers.IO){
                dbQuery.cekItems()
            }

            binding.includeLayout.root.visibility = View.VISIBLE
            binding.clPesanan.visibility = View.GONE

            binding.rvMenuPesanan.layoutManager = LinearLayoutManager(context)
            binding.rvMenuPesanan.setHasFixedSize(false)

            if (cek > 0){

                binding.includeLayout.root.visibility = View.GONE
                binding.clPesanan.visibility = View.VISIBLE

                val result = withContext(Dispatchers.IO){
                    dbQuery.getItemsProduc()
                }

                showList(result)
            }
        }
    }

    private fun showList(list: MutableList<Produc>){
        orderProductAdapter = OrderProductAdapter(list)
        binding.rvMenuPesanan.adapter = orderProductAdapter
        setTextTotal(list.sumOf { it.quanty!!.toInt() }, list.sumOf { it.total!!.toInt() })
        orderProductAdapter.setOnItemClickCallbackQty(object : OrderProductAdapter.OnItemClickCallbackQty{
            override fun onItemClicked(items: Produc) {
                val dialogQty = Dialog(requireContext())
                dialogQty.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogQty.setContentView(R.layout.dialog_qty)

                val etQty = dialogQty.findViewById<EditText>(R.id.tvQtyJumlah)
                val btnSimpan = dialogQty.findViewById<RelativeLayout>(R.id.btnSimpanQty)

                etQty.setText(items.quanty.toString())

                btnSimpan.setOnClickListener{
                    if (etQty.text.toString() <= "0" || etQty.text.toString() == ""){
                        Toast.makeText(context!!, "Tidak boleh sama dengan 0 atau kosong", Toast.LENGTH_SHORT).show()
                    }else{
                        lifecycleScope.launch {
                            items.quanty = etQty.text.toString().toInt()
                            items.total = items.harga!! * items.quanty!!

                            val cv = ContentValues()
                            cv.put(DbContract.baseColumns.QUANTY_PRODUCT, items.quanty)
                            cv.put(DbContract.baseColumns.TOTAL_PRODUCT, items.total)
                            val resultRemove = withContext(Dispatchers.IO){
                                dbQuery.update(items.uid.toString(), cv)
                            }

                            Log.e(context.toString(), resultRemove.toString())
                            if (resultRemove > 0){
                                setTextTotal(
                                    list.sumOf { it.quanty!!.toInt() },
                                    list.sumOf { it.total!!.toInt() })
                                orderProductAdapter.notifyDataSetChanged()
                                dialogQty.dismiss()
                            }
                        }
                    }

                }

                dialogQty.show()
                dialogQty.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                dialogQty.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogQty.window?.attributes?.windowAnimations = R.style.DialogAnimation
                dialogQty.window?.setGravity(Gravity.CENTER)
            }
        })

        orderProductAdapter.setOnItemClickButtonCallback(object : OrderProductAdapter.OnItemClickButtonCallback{
            override fun onAddButtonClicked(item: Produc) {
                lifecycleScope.launch {

                    item.quanty = item.quanty!! + 1
                    item.total = item.harga!! * item.quanty!!

                    Log.e("hasil quanty", item.quanty.toString())
                    Log.e("hasil total", item.total.toString())

                    val cv = ContentValues()
                    cv.put(DbContract.baseColumns.QUANTY_PRODUCT, item.quanty)
                    cv.put(DbContract.baseColumns.TOTAL_PRODUCT, item.total)

                    val resultAdd = withContext(Dispatchers.IO){
                        dbQuery.update(item.uid.toString(), cv)
                    }

                    Log.e(context.toString(), item.quanty.toString())
                    if (resultAdd > 0){
                        setTextTotal(
                            list.sumOf { it.quanty!!.toInt() },
                            list.sumOf { it.total!!.toInt() })
                        orderProductAdapter.notifyDataSetChanged()
                    }
                }
            }
        })

        orderProductAdapter.setOnItemClickRemoveCallback(object : OrderProductAdapter.OnItemClickRemoveCallback{
            override fun onRemoveButtonlicked(items: Produc, position: Int) {
                if (items.quanty!! > 0 ){
                    lifecycleScope.launch {

                        items.quanty = items.quanty!! - 1
                        items.total = items.harga!! * items.quanty!!

                        val cv = ContentValues()
                        cv.put(DbContract.baseColumns.QUANTY_PRODUCT, items.quanty)
                        cv.put(DbContract.baseColumns.TOTAL_PRODUCT, items.total)
                        val resultRemove = withContext(Dispatchers.IO){
                            dbQuery.update(items.uid.toString(), cv)
                        }

                        if (items.quanty == 0){
                            withContext(Dispatchers.IO){
                                dbQuery.deleteById(items.uid.toString())
                            }
                            list.removeAt(position)
                            if (list.size == 0){
                                binding.includeLayout.root.visibility = View.VISIBLE
                                binding.clPesanan.visibility = View.GONE
                            }
                            orderProductAdapter.notifyItemRemoved(position)
                        }
                        Log.e(context.toString(), resultRemove.toString())
                        if (resultRemove > 0){
                            setTextTotal(
                                list.sumOf { it.quanty!!.toInt() },
                                list.sumOf { it.total!!.toInt() })
                            orderProductAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })

        orderProductAdapter.setOnItemClickCallback(object : OrderProductAdapter.OnItemClickCallback{
            override fun onItemClicked(items: Produc) {
                lifecycleScope.launch {
                    val dialogCatatan = Dialog(context!!)
                    dialogCatatan.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialogCatatan.setContentView(R.layout.dialog_catatan)
                    val etCatatan = dialogCatatan.findViewById<TextView>(R.id.edit_catatan)
                    val listCatatan = dialogCatatan.findViewById<GridView>(R.id.list_view)
                    val chipGroup = dialogCatatan.findViewById<ChipGroup>(R.id.chipGroup)
                    val tvJudul = dialogCatatan.findViewById<TextView>(R.id.tvJudulCatatan)
                    val tvKirim = dialogCatatan.findViewById<TextView>(R.id.tvUpdateCatatan)
                    val tvBatal = dialogCatatan.findViewById<TextView>(R.id.tvBatalCatatan)
                    val btnClose = dialogCatatan.findViewById<ImageView>(R.id.btnCloseCatatan)

                    chipGroup.visibility = View.VISIBLE
                    tvJudul.text = items.nama

                    val pref = withContext(Dispatchers.IO){
                        loadPrefMenu(items.menuId.toString())
                    }

                    items.catatan?.split(",")!!.forEach {
                        if (it.trim() != ""){
                            val chip = Chip(context)

                            chip.text = it.trim()

                            chip.isCloseIconVisible = true

                            chip.setOnCloseIconClickListener{
                                chipGroup.removeView(chip)
                            }
                            chipGroup.addView(chip)
                        }
                    }

                    btnClose.setOnClickListener{
                        dialogCatatan.dismiss()
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, pref.split(", ").toList())
                    listCatatan.adapter = adapter

                    var arrayCatatan = ""
                    listCatatan.onItemClickListener = OnItemClickListener { _, _, position, _ ->

                        val chip = Chip(context)

                        val getItemsPosition = adapter.getItem(position)
                        chip.text = getItemsPosition

                        chip.isCloseIconVisible = true

                        chip.setOnCloseIconClickListener{
                            chipGroup.removeView(chip)
                        }

                        chipGroup.addView(chip)
                        if (etCatatan.text.isNotEmpty()){
                            arrayCatatan += ", $getItemsPosition"
                        } else {
                            arrayCatatan = getItemsPosition.toString()
                        }
                    }

                    tvKirim.setOnClickListener{
                        lifecycleScope.launch {
                            var chipArray = ""
                            withContext(Dispatchers.IO){
                                for (i in 0 until chipGroup.childCount) {
                                    val dataChips = (chipGroup.getChildAt(i) as Chip).text.toString()
                                    chipArray += if (chipArray == ""){
                                        dataChips
                                    }else{
                                        ", $dataChips"
                                    }
                                }
                            }
                            chipArray += if (chipArray == ""){
                                etCatatan.text
                            }else{
                                if (etCatatan.text.isNotEmpty()){
                                    ", ${etCatatan.text}"
                                } else {
                                    ""
                                }
                            }
                            val cv = ContentValues()
                            cv.put(DbContract.baseColumns.NOTE_PRODUCT, chipArray.toUpperCase())
                            val addNote = withContext(Dispatchers.IO){
                                dbQuery.update(items.uid.toString(), cv)
                            }
                            if (addNote > 0){
                                dialogCatatan.dismiss()
                                items.catatan = chipArray.toUpperCase()
                                orderProductAdapter.notifyDataSetChanged()
                            }
                        }
                    }

                    tvBatal.setOnClickListener {
                        dialogCatatan.dismiss()
                    }

                    dialogCatatan.show()
                    dialogCatatan.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    dialogCatatan.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialogCatatan.window?.attributes?.windowAnimations = R.style.DialogAnimation
                    dialogCatatan.window?.setGravity(Gravity.CENTER)
                }
            }
        })
    }

    private fun loadPrefMenu(menuCode: String):String{
        val conn = connect.connection(requireContext())
        var result = ""
        if (conn != null){
            try {
                val query = "EXEC USP_J_Mobile_QueryStatic @Action = 'Menu.Pref', @MenuCode = ? "
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, menuCode)
                val rs = preparedStatement.executeQuery()
                while (rs.next()){
                    result = rs.getString("Prefs")
                }
                return result
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
        return result
    }

    private fun setTextTotal(quanty: Int, total: Int){
        binding.tvQuantyBawah.text = quanty.toString()
        binding.tvTotalBawah.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(total)
    }

    private fun checkBluetoothPermissions() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent,201)
        }else{
            if (bluetoothDevicesList != null) {
                val items = arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                items[0] = "Default printer"
                for ((i, device) in bluetoothDevicesList.withIndex()) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    items[i + 1] = device.device.name
                }

                val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Bluetooth printer selection")
                alertDialog.setItems(
                    items
                ) { _, i1 ->
                    val index = i1 - 1
                    selectedDevice = if (index == -1) {
                        null
                    } else {
                        bluetoothDevicesList[index]
                    }
//                    val item = menuP.getItem(0)
                    val spanString = SpannableString(items[i1])
                    spanString.setSpan(
                        ForegroundColorSpan(Color.BLACK),
                        0,
                        spanString.length,
                        0
                    )
                    binding.tvPrinterOrders.text = spanString
                }
                val alert = alertDialog.create()
                alert.setCanceledOnTouchOutside(true)
                alert.show()
            }
        }
    }

    private fun checkBluetoothPermissions2() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent,201)
        }else{
            if (bluetoothDevicesList != null) {
                val items = arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                items[0] = "Default printer"
                for ((i, device) in bluetoothDevicesList.withIndex()) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    items[i + 1] = device.device.name
                }

                val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Bluetooth printer selection")
                alertDialog.setItems(
                    items
                ) { _, i1 ->
                    val index = i1 - 1
                    selectedDevice = if (index == -1) {
                        null
                    } else {
                        bluetoothDevicesList[index]
                    }
//                    val item = menuP.getItem(0)
                    val spanString = SpannableString(items[i1])
                    spanString.setSpan(
                        ForegroundColorSpan(Color.BLACK),
                        0,
                        spanString.length,
                        0
                    )
                    tvSelectedDevice.text = spanString
                }
                val alert = alertDialog.create()
                alert.setCanceledOnTouchOutside(true)
                alert.show()
            }
        }
    }

    private suspend fun printBluetooth(meja: String, listPesanan: MutableList<Produc>):String {
        return withContext(Dispatchers.Main){
            val result = CompletableDeferred<String>()
            AsyncBluetoothEscPosPrint(
                requireContext(),
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(asyncEscPosPrinter: AsyncEscPosPrinter, codeException: Int) {
                        result.complete("Gagal")
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                        result.complete("Berhasil")
                    }
                }
            ).execute(getAsyncEscPosPrinter(selectedDevice!!, meja, listPesanan)).toString()
            result.await()
        }
    }

    private suspend fun printBluetooth2(meja: String, listPesanan: MutableList<Produc>, method: String, cash: Int, reprint: Boolean):String {
        return withContext(Dispatchers.Main){
            val result = CompletableDeferred<String>()
            AsyncBluetoothEscPosPrint(
                requireContext(),
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(asyncEscPosPrinter: AsyncEscPosPrinter, codeException: Int) {
                        result.complete("Gagal")
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                        result.complete("Berhasil")
                    }
                }
            ).execute(getAsyncEscPosPrinter2(selectedDevice!!, meja, listPesanan, method, cash, reprint)).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinter(printerConnection: DeviceConnection, meja: String, listPesanan: MutableList<Produc>): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
            try {
                printer.addTextToPrint("" +
//                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logoc, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
//                        "[L]\n" +
                        "[C]<b><font size='normal'>ORDERS</font></b>\n" +
//                        "[C]<b><font size='normal'>Kota Medan</font></b>\n" +
//                        "[C]\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        "[L]Tanggal      [R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu        [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]BY           [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
                                "[L]${it.quanty}"} +
                        "\n[L]================================\n" +
//                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
                        "[L]ITEMS        [R]: [R]${listPesanan.sumOf { it.quanty!!.toInt() }}\n" +
//                        "[L]$method      [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n" +
//                        "[L]\n" +
//                        "[C]<b><font size='normal'>Terimakasih Telah Melakukan\n"+
//                        "[C]Pembayaran</font></b>\n" +
                        "[L]\n")
            }catch (e: EscPosEncodingException){
                e.toString()
                Log.e("cek printer", e.toString())
            }
            printer
        }
    }

    private suspend fun getAsyncEscPosPrinter2(printerConnection: DeviceConnection, meja: String, listPesanan: MutableList<Produc>, method: String, cash: Int, reprint: Boolean): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
            try {
                printer.addTextToPrint("\n" +
//                        "[C]<b><font size='normal'>PESANAN</font></b>\n" +
//                        "[L]No Meja      [R]: [R]$meja\n" +
//                        "[L]Tanggal      [R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
//                        "[L]Waktu        [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
//                        "[L]CASHIR       [R]: [R]${sessionLogin.getUserLogin()}\n" +
//                        "[L]\n" +
//                        "[L]================================\n" +
//                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
//                                "[L]${it.quanty}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.harga)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.total)}"} +
//                        "\n[L]================================\n" +
////                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
//                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}\n" +
//                        "[L]ITEMS        [R]: [R]${listPesanan.sumOf { it.quanty!!.toInt() }}\n" +
//                        "[L]$method      [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf {it.total!!.toInt()})}\n" +
//                        "[L]\n" +
//                        "[L]\n" +
//                        "[L]\n" +
                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logoc, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                        "[L]\n" +
                        if (reprint){
                            "[C]<b><font size='normal'>==REPRINT==</font></b>\n"
                        }else{
                            ""
                        }+
                        "\n[C]<b><font size='normal'>Teh Gratis (07:00 - 11:00 WIB)\n" +
                        "[C]@cemarafoodcourt</font></b>\n"+
                        "[L]\n" +
//                        "[C]<b><font size='normal'>Cemara Food Court\n" +
//                        "[C]<b><font size='normal'>Kota Medan</font></b>\n" +
//                        "[C]\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        "[L]Tanggal Cetak[R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu Cetak  [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]CASHIR       [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
                                "[L]${it.quanty}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.harga)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.total)}"} +
                        "\n[L]================================\n" +
                        "[L]<b><font size='tall'>TOTAL [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}</font></b>\n" +
//                        if(listPesanan.sumOf { it.disc!!.toInt()} > 0){ "[L]DISC         [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak[0].disc)}\n"
//                            "[L]NET TOTAL         : [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n"
//                        } else { "" }+
                        "[L]ITEMS    [R]${listPesanan.sumOf { it.quanty!!.toInt() }}\n" +
                        if (method == "TUNAI"){
                            "[L]$method  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(cash)}\n" +
                            "[L]KEMBALIAN[R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(cash - listPesanan.sumOf { it.total!!.toInt() })}\n"
                        }else{
                            "[L]$method  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}\n" +
                            "\n"
                        } +
                        "[L]\n" +
                        "[C]<b><font size='normal'>Terimakasih Telah Melakukan\n"+
                        "[C]Pembayaran</font></b>\n" +
                        "[L]\n" +
                        "[C]<font size='normal'>Mohon Ditunggu\n"+
                        "[C]Pesanan Anda Sedang Diproses</font>\n" +
                        "[L]\n" +
//                        "[C]<qrcode>"+"https://www.instagram.com/cemarafoodcourt"+"</qrcode>\n" +
                        "[L]\n")
            }catch (e: EscPosEncodingException){
                e.toString()
                Log.e("cek printer", e.toString())
            }
            printer
        }
    }

    private fun sendTbPrinter(salesId: Int, method: String): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_MarkTBPrinted @SalesId = ?, @By = ?, @TBMethod= ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, salesId)
                preparedStatement.setString(2, sessionLogin.getUserLogin())
                preparedStatement.setString(3, method)
                preparedStatement.executeQuery()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun getCurrentDate(format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }
}