package com.example.drinkcourt.fragment

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.utils.Utils
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.drinkcourt.R
import com.example.drinkcourt.adapter.CetakAdapter
import com.example.drinkcourt.adapter.ListBottomAdapter
import com.example.drinkcourt.adapter.ListTransaksiAdapter
import com.example.drinkcourt.async.AsyncBluetoothEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrinter
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.FragmentTransaksiBinding
import com.example.drinkcourt.model.Cetak
import com.example.drinkcourt.model.SubTransaksi
import com.example.drinkcourt.model.Transaksi
import com.example.drinkcourt.mvvm.MainViewModel
import com.example.drinkcourt.utils.SessionLogin
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransaksiFragment : Fragment() {

    private var _binding: FragmentTransaksiBinding? = null
    private val binding get() = _binding!!

    private lateinit var listTransaksiAdapter: ListTransaksiAdapter
    private lateinit var connect: Connect
    private lateinit var sessionLogin: SessionLogin
    private lateinit var listTransaksi : MutableList<Transaksi>
    private lateinit var listCetak: MutableList<Cetak>
    private lateinit var listTable: MutableList<String>
    private lateinit var selectPrinter: TextView

    private var selectedDevice : BluetoothConnection? = null

    companion object{
        private const val REQUEST_ENABLE_BT = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransaksiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connect = Connect()
        sessionLogin = SessionLogin(requireContext())

        binding.refreshPesanan.setOnRefreshListener {
            binding.refreshPesanan.setRefreshing(false)
            showList(null)
        }

        setPermission()

        showList(null)

        binding.etSearch.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etSearch.setOnEditorActionListener(TextView.OnEditorActionListener{_,actionId,_ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                showList(binding.etSearch.text.toString())
                binding.imgClear.visibility = View.VISIBLE
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        binding.imgClear.setOnClickListener {
            binding.etSearch.setText("")
            binding.imgClear.visibility = View.GONE
            showList(null)
        }
    }

    private fun showList(key: String?){
        val progressBarAwal = ProgressDialog(context)
        progressBarAwal.setTitle("Mohon tunggu!!")
        progressBarAwal.setMessage("Sedang mengambil data..")
        progressBarAwal.setCanceledOnTouchOutside(false)
        progressBarAwal.show()

        binding.includeLayout.root.visibility = View.GONE
        binding.shimmerRecyclerView.visibility = View.VISIBLE
        binding.rlSearch.visibility = View.VISIBLE

        binding.shimmerRecyclerView.layoutManager = GridLayoutManager(context, requireView().resources.getInteger(R.integer.grid_items))
        binding.shimmerRecyclerView.setHasFixedSize(false)
        binding.shimmerRecyclerView.hideShimmerAdapter()
        listTransaksi = mutableListOf()
        lifecycleScope.launch {

            val result = if (key.isNullOrEmpty()){
                withContext(Dispatchers.IO){
                    loadItems()
                }
            }else{
                withContext(Dispatchers.IO){
                    loadItemsBySearch(key)
                }
            }

            when (result){
                "Berhasil" ->{
                    if (listTransaksi.size > 0){
                        listTransaksiAdapter = ListTransaksiAdapter(requireContext())
                        listTransaksiAdapter.setData(listTransaksi)
                        binding.shimmerRecyclerView.adapter = listTransaksiAdapter
                        listTransaksiAdapter.setOnItemClickCallback(object : ListTransaksiAdapter.OnItemClickCallback{
                            override fun onItemClicked(transaksi: Transaksi) {
                                val dialog = Dialog(context!!)
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                dialog.setContentView(R.layout.bottom_pesanan)

                                val tvMeja = dialog.findViewById<TextView>(R.id.tvMejaBottom)
                                val rv = dialog.findViewById<RecyclerView>(R.id.rvListBottom)
                                val btnUpdate = dialog.findViewById<RelativeLayout>(R.id.btnUpdateTable)
                                val btnClose = dialog.findViewById<LinearLayout>(R.id.llBotomClose)

                                rv.layoutManager = LinearLayoutManager(context!!)
                                rv.setHasFixedSize(false)

                                var listBottomAdapter1: ListBottomAdapter

                                tvMeja.text = transaksi.noMeja.toString()
                                btnUpdate.setOnClickListener {
                                    val dialogMeja = Dialog(context!!)
                                    dialogMeja.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialogMeja.setContentView(R.layout.dialog_table_update)
                                    dialogMeja.setCanceledOnTouchOutside(false)

                                    val spinnerMeja = dialogMeja.findViewById<AppCompatSpinner>(R.id.spinnerUpdate)
                                    val updateMeja = dialogMeja.findViewById<TextView>(R.id.tvUpdateMeja)
                                    val batalMeja = dialogMeja.findViewById<TextView>(R.id.tvBatalMeja)

                                    var selectTable = ""
                                    val adapterTable = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, listTable)
                                    adapterTable.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    spinnerMeja.adapter = adapterTable
                                    spinnerMeja.setSelection(adapterTable.getPosition(transaksi.noMeja.toString()))

                                    spinnerMeja.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                            selectTable = listTable[position]
                                        }
                                        override fun onNothingSelected(p: AdapterView<*>?) {}
                                    }

                                    updateMeja.setOnClickListener {
                                        lifecycleScope.launch {
                                            val progressBar = ProgressDialog(context)
                                            progressBar.setTitle("Mohon tunggu!!")
                                            progressBar.setMessage("Sedang mengubah meja..")
                                            progressBar.setCanceledOnTouchOutside(false)
                                            progressBar.show()

                                            val resultMeja = withContext(Dispatchers.IO){
                                                ubahMeja(transaksi.salesId!!.toInt(), selectTable)
                                            }

                                            if (resultMeja == "Berhasil"){
                                                tvMeja.text = selectTable
//                                                Snackbar.make(binding.root, "Gagal ubah meja!!",Snackbar.LENGTH_SHORT).show()
                                            }else{
                                                Snackbar.make(binding.root, resultMeja,Snackbar.LENGTH_SHORT).show()
                                            }
                                            showList(null)
                                            progressBar.dismiss()
                                            dialogMeja.dismiss()
                                        }
                                    }

                                    batalMeja.setOnClickListener {
                                        dialogMeja.dismiss()
                                    }

                                    dialogMeja.show()
                                    dialogMeja.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    dialogMeja.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                    dialogMeja.window?.attributes?.windowAnimations = R.style.DialogAnimation
                                    dialogMeja.window?.setGravity(Gravity.CENTER)
                                }

                                val listBottom = mutableListOf<SubTransaksi>()
                                CoroutineScope(Dispatchers.Main + Job()).launch {
                                    try {
                                        val jsonArray = JSONArray(transaksi.detail)
                                        for (i in 0 until jsonArray.length()){
                                            val getObject = jsonArray.getJSONObject(i)
                                            val subTransaksi = SubTransaksi()
                                            subTransaksi.meja = transaksi.noMeja
                                            subTransaksi.namaMenu = getObject.getString("MenuName")
                                            subTransaksi.menuId = getObject.getString("MenuID")
                                            subTransaksi.qty = getObject.getInt("Qty")
                                            subTransaksi.pref = getObject.getString("Request")
                                            subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                                            subTransaksi.maxQty = getObject.getInt("Qty")
                                            listBottom.add(subTransaksi)
                                        }
                                    }catch (e: JSONException){
                                        e.printStackTrace()
                                    }
                                    listBottomAdapter1 = ListBottomAdapter(listBottom, context!!, false)
                                    rv.adapter = listBottomAdapter1
                                    listBottomAdapter1.setOnItemClickCallback(object: ListBottomAdapter.OnItemClickCallback{
                                        override fun onItemClicked(subTransaksi: SubTransaksi, position:Int) {
                                            CoroutineScope(Dispatchers.Main + Job()).launch {
                                                val progressBar = ProgressDialog(context)
                                                progressBar.setTitle("Mohon tunggu!!")
                                                progressBar.setMessage("Sedang mengirim pesanan..")
                                                progressBar.setCanceledOnTouchOutside(false)
                                                progressBar.show()
                                                val result1 = withContext(Dispatchers.IO){
                                                    sendPengiriman(subTransaksi.meja.toString(), subTransaksi.menuId!!.toInt(), subTransaksi.pref.toString(), subTransaksi.qty!!.toInt())
                                                }
                                                when(result1){
                                                    "Berhasil" ->{
                                                        progressBar.dismiss()
                                                        listBottomAdapter1.kondition = true

                                                        subTransaksi.maxQty = subTransaksi.maxQty!! - subTransaksi.qty!!
                                                        subTransaksi.qty = subTransaksi.maxQty

                                                        if (subTransaksi.maxQty == 0){
                                                            listBottom.removeAt(position)
                                                            if (listBottom.size == 0){
                                                                lifecycleScope.launch {

                                                                    val resultCetak = withContext(Dispatchers.IO){
                                                                        loadBill(transaksi.salesId!!.toInt())
                                                                    }
                                                                    when(resultCetak){
                                                                        "Berhasil" -> {
                                                                            withContext(Dispatchers.Main){
                                                                                val dialogCetak = Dialog(context!!)
                                                                                dialogCetak.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                                                                dialogCetak.setContentView(R.layout.dialog_cetak)
                                                                                dialogCetak.setCanceledOnTouchOutside(false)
                                                                                selectPrinter    = dialogCetak.findViewById<TextView>(R.id.tvSelectPrinter)
                                                                                val rvListCetak  = dialogCetak.findViewById<RecyclerView>(R.id.rvListPesananSelesai)
                                                                                val selectMetode = dialogCetak.findViewById<AppCompatSpinner>(R.id.spinnerTableCetak)
                                                                                val btnCetak     = dialogCetak.findViewById<RelativeLayout>(R.id.btnCetak)
                                                                                val tvCetak      = dialogCetak.findViewById<TextView>(R.id.tvCetak)
                                                                                val llUlang      = dialogCetak.findViewById<LinearLayout>(R.id.llBawahUlang)
                                                                                val btnCancel    = dialogCetak.findViewById<RelativeLayout>(R.id.btnTutup)
                                                                                val btnReprint   = dialogCetak.findViewById<RelativeLayout>(R.id.btnCetakUlang)
                                                                                val tvQtyTotal   = dialogCetak.findViewById<TextView>(R.id.tvQtyCetakTotal)
                                                                                val tvTotalCetak = dialogCetak.findViewById<TextView>(R.id.tvTotalCetak)

                                                                                llUlang.visibility = View.GONE

                                                                                rvListCetak.layoutManager = LinearLayoutManager(context)
                                                                                rvListCetak.setHasFixedSize(false)

                                                                                selectPrinter.setOnClickListener {
                                                                                    checkBluetoothPermissions()
                                                                                }

                                                                                tvQtyTotal.text = listCetak.sumOf { it.qtyCetak!!.toInt() }.toString()
                                                                                tvTotalCetak.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() }).replace(",",".")

                                                                                val arrayMetod = arrayOf("PILIH METODE PEMBAYARAN", "TUNAI", "QRIS", "OVO", "DEBIT", "TRANSFER")
                                                                                var selectMetod = ""
                                                                                val adapterTipe = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayMetod)
                                                                                adapterTipe.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                                                                selectMetode.adapter = adapterTipe

                                                                                selectMetode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                                                                    override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                                                                        selectMetod = arrayMetod[position]
                                                                                    }
                                                                                    override fun onNothingSelected(p: AdapterView<*>?) {}
                                                                                }

                                                                                btnCetak.setOnClickListener {
                                                                                    if (selectMetod == "PILIH METODE PEMBAYARAN"){
                                                                                        Snackbar.make(binding.root, "Pilih metode pembayaran terlebih dahulu!!",Snackbar.LENGTH_SHORT).show()
                                                                                    }else{
                                                                                        if (selectedDevice == null){
                                                                                            Toast.makeText(context, "Pilih printer terlebih dahulu!!",Toast.LENGTH_SHORT).show()
                                                                                        }else{
                                                                                            lifecycleScope.launch {
                                                                                                val cetakStruk = withContext(Dispatchers.IO){
                                                                                                    printBluetooth(transaksi.noMeja.toString(), selectMetod)
                                                                                                }
                                                                                                if (cetakStruk == "Berhasil"){
                                                                                                    withContext(Dispatchers.IO){
                                                                                                        sendTbPrinter(transaksi.salesId!!.toInt(), selectMetod)
                                                                                                    }
                                                                                                    Log.e("hasil print", "berhasil")
                                                                                                    llUlang.visibility = View.VISIBLE
                                                                                                    btnCetak.visibility = View.GONE
                                                                                                    Toast.makeText(context, "Berhasil mencetak struk!!",Toast.LENGTH_SHORT).show()
                                                                                                }else{
                                                                                                    tvCetak.text = "REPRINT"
                                                                                                    Toast.makeText(context, "Gagal mencetak struk, silahkan coba lagi!!",Toast.LENGTH_SHORT).show()
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }

                                                                                btnReprint.setOnClickListener {
                                                                                    if (selectMetod == "PILIH METODE PEMBAYARAN"){
                                                                                        Snackbar.make(binding.root, "Pilih metode pembayaran terlebih dahulu!!",Snackbar.LENGTH_SHORT).show()
                                                                                    }else{
                                                                                        if (selectedDevice == null){
                                                                                            Toast.makeText(context, "Pilih printer terlebih dahulu!!",Toast.LENGTH_SHORT).show()
                                                                                        }else{
                                                                                            lifecycleScope.launch {
                                                                                                val cetakStruk = withContext(Dispatchers.IO){
                                                                                                    printBluetooth(transaksi.noMeja.toString(), selectMetod)
                                                                                                }
                                                                                                if (cetakStruk == "Berhasil"){
                                                                                                    llUlang.visibility = View.VISIBLE
                                                                                                    btnCetak.visibility = View.GONE
                                                                                                    Toast.makeText(context, "Berhasil mencetak struk!!",Toast.LENGTH_SHORT).show()
                                                                                                }else{
                                                                                                    Toast.makeText(context, "Gagal mencetak struk, silahkan coba lagi!!",Toast.LENGTH_SHORT).show()
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }

                                                                                btnCancel.setOnClickListener {
                                                                                    dialogCetak.dismiss()
                                                                                }

                                                                                val cetakAdapter = CetakAdapter(listCetak)
                                                                                rvListCetak.adapter = cetakAdapter

                                                                                dialogCetak.show()
                                                                                dialogCetak.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                                                                dialogCetak.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                                                                dialogCetak.window?.attributes?.windowAnimations = R.style.DialogAnimation
                                                                                dialogCetak.window?.setGravity(Gravity.CENTER)
//                                                                                builderDialog.show().window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                                                            }
                                                                        }
                                                                        "Gagal" -> {

                                                                        }
                                                                        else -> {

                                                                        }
                                                                    }
                                                                }
                                                                showList(null)
                                                                dialog.dismiss()

                                                            }
                                                            listBottomAdapter1.notifyItemRemoved(position)
                                                        }
                                                        listBottomAdapter1.notifyDataSetChanged()
                                                    }
                                                    "Gagal"-> {
                                                        progressBar.dismiss()
                                                        Toast.makeText(context, "Tidak terhubung keserver", Toast.LENGTH_SHORT).show()
                                                    }
                                                    else -> {
                                                        progressBar.dismiss()
                                                        Toast.makeText(context, result1, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    })

                                    dialog.setOnCancelListener {
                                        if (listBottomAdapter1.kondition){
                                            showList(null)
                                        }
                                        listBottomAdapter1.kondition = false
                                    }
                                }

                                btnClose.setOnClickListener{
                                    dialog.dismiss()
                                }

                                dialog.show()
                                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                                dialog.window?.setGravity(Gravity.BOTTOM)
                            }
                        })
                    }else{
                        binding.includeLayout.root.visibility = View.VISIBLE
                        binding.shimmerRecyclerView.visibility = View.GONE
//                        binding.rlSearch.visibility = View.GONE
                    }
                }
                "Gagal" -> {
                    binding.includeLayout.root.visibility = View.VISIBLE
                    binding.shimmerRecyclerView.visibility = View.GONE
//                    binding.rlSearch.visibility = View.GONE
                    Snackbar.make(binding.root, "Tidak terhubung ke server", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    binding.includeLayout.root.visibility = View.VISIBLE
                    binding.shimmerRecyclerView.visibility = View.GONE
//                    binding.rlSearch.visibility = View.GONE
                    Snackbar.make(binding.root, result, Snackbar.LENGTH_SHORT).show()
                }
            }
            binding.refreshPesanan.setRefreshing(false)
            progressBarAwal.dismiss()
        }
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
                    selectPrinter.text = spanString
                }
                val alert = alertDialog.create()
                alert.setCanceledOnTouchOutside(true)
                alert.show()
            }
        }
    }

    private suspend fun printBluetooth(meja: String, method: String):String {
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
            ).execute(getAsyncEscPosPrinter(selectedDevice!!, meja, method)).toString()
            result.await()
        }
    }

    private fun setPermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH), 101)
            } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_ADMIN), 102)
            } else {
                checkBluetoothEnabled()
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 103)
            } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), 104)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != AppCompatActivity.RESULT_OK) {
                Snackbar.make(binding.root, "Bluetooth wajib dihidupkan!!",Snackbar.LENGTH_SHORT).show()
                checkBluetoothEnabled()
            }
        }
    }

    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private suspend fun getAsyncEscPosPrinter(printerConnection: DeviceConnection, meja: String, method:String): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
            try {
                printer.addTextToPrint("" +
                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logoc, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                        "[L]\n" +
//                        "[C]<b><font size='normal'>Cemara Food Court\n" +
//                        "[C]<b><font size='normal'>Kota Medan</font></b>\n" +
//                        "[C]\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        "[L]Tanggal Cetak[R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu Cetak  [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]BY           [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listCetak.joinToString("\n") { it -> "[L]${it.menuName}\n" +
                                "[L]${it.qtyCetak}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.price)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.totalCetak)}"} +
                        "\n[L]================================\n" +
                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
                        if(listCetak.sumOf { it.disc!!.toInt()} > 0){ "[L]DISC         [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak[0].disc)}\n"
//                                    "[L]NET TOTAL         : [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n"
                        } else { "" }+
                        "[L]ITEMS        [R]: [R]${listCetak.sumOf { it.qtyCetak!!.toInt() }}\n" +
                        "[L]$method      [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n" +
                        "[L]\n" +
                        "[C]<b><font size='normal'>Terimakasih Telah Melakukan\n"+
                        "[C]Pembayaran</font></b>\n" +
                        "[L]\n" +
                        "[L]\n" +
                        "[L]\n" +
                        "[C]<b><font size='normal'>PESANAN SELESAI</font></b>\n" +
//                        "[C]<b><font size='normal'>Kota Medan</font></b>\n" +
//                        "[C]\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        "[L]Tanggal      [R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu        [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]BY           [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        listCetak.joinToString("\n") { it -> "[L]${it.menuName}\n" +
                                "[L]${it.qtyCetak}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.price)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.totalCetak)}"} +
                        "\n[L]================================\n" +
                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
                        if(listCetak.sumOf { it.disc!!.toInt()} > 0){ "[L]DISC         [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak[0].disc)}\n"
//                                    "[L]NET TOTAL         : [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n"
                        } else { "" }+
                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
                        "[L]ITEMS        [R]: [R]${listCetak.sumOf { it.qtyCetak!!.toInt() }}\n" +
                        "[L]$method      [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n" +
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

    private fun loadBill(salesId: Int): String{
        val conn = connect.connection(requireContext())
        listCetak = mutableListOf()
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_QueryForBill @SalesID = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, salesId)
                val rs = preparedStatement.executeQuery()
                listCetak.clear()
                while (rs.next()){
                    val cetak = Cetak()
                    cetak.menuName = rs.getString("MenuName")
                    cetak.salesId = rs.getInt("SalesID")
                    cetak.qtyCetak = rs.getInt("ItemQty")
                    cetak.totalCetak = rs.getInt("LineTotal")
                    cetak.price = rs.getInt("Price")
                    cetak.disc = rs.getInt("Disc1") + rs.getInt("Disc2")
                    listCetak.add(cetak)
                }
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun ubahMeja(salesId: Int, noMeja: String): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_TransferTable @SalesID = ?, @NewTableCode = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, salesId)
                preparedStatement.setString(2, noMeja)
                preparedStatement.executeQuery()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun sendPengiriman(tableCode: String, menuId: Int, request: String, qty: Int): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_MarkForDelivery @TableCode = ?, @MenuID = ?, @Request = ?, @Qty = ?, @User = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, tableCode)
                preparedStatement.setInt(2, menuId)
                preparedStatement.setString(3, request)
                preparedStatement.setInt(4, qty)
                preparedStatement.setString(5, sessionLogin.getUserLogin())
                preparedStatement.executeQuery()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun sendTbPrinter(meja: Int, method: String): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_MarkTBPrinted @SalesId = ?, @By = ?, @TBMethod= ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, meja)
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

    private fun loadItems(): String{
        val conn = connect.connection(requireContext())
        listTable = mutableListOf()
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_QueryForChecker"
                val preparedStatement = conn.createStatement()
                val rs = preparedStatement.executeQuery(query)
                listTransaksi.clear()
                while (rs.next()){
                    val transaksi = Transaksi()
                    transaksi.noMeja = rs.getString("TableCode")
                    transaksi.date = rs.getString("TableLastMod")
                    transaksi.detail = rs.getString("DetailJson")
                    transaksi.salesId = rs.getInt("SalesID")
                    transaksi.group = rs.getString("GroupName")
//                    Log.e("cek json", rs.getString("DetailJson"))
                    listTransaksi.add(transaksi)
                }

                val queryTable = "EXEC USP_J_Mobile_QueryStatic @Action = 'Table'"
                val stTable = conn.createStatement()
                val rsTable = stTable.executeQuery(queryTable)
                listTable.clear()
                listTable.add("Pilih Meja")
                while (rsTable.next()){
                    listTable.add(rsTable.getString("TableCode"))
                }

                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun loadItemsBySearch(key: String): String{
        val conn = connect.connection(requireContext())
        listTable = mutableListOf()
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_QueryForChecker @Phrase = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, key)
                val rs = preparedStatement.executeQuery()
                listTransaksi.clear()
                while (rs.next()){
                    val transaksi = Transaksi()
                    transaksi.noMeja = rs.getString("TableCode")
                    transaksi.date = rs.getString("TableLastMod")
                    transaksi.detail = rs.getString("DetailJson")
                    transaksi.salesId = rs.getInt("SalesID")
                    transaksi.group = rs.getString("GroupName")
//                    Log.e("cek json", rs.getString("DetailJson"))
                    listTransaksi.add(transaksi)
                }

                val queryTable = "EXEC USP_J_Mobile_QueryStatic @Action = 'Table'"
                val stTable = conn.createStatement()
                val rsTable = stTable.executeQuery(queryTable)
                listTable.clear()
                listTable.add("Pilih Meja")
                while (rsTable.next()){
                    listTable.add(rsTable.getString("TableCode"))
                }

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