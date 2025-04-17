package com.example.drinkcourt.fragment

import android.Manifest
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
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.example.drinkcourt.R
import com.example.drinkcourt.adapter.ListBottomHistoryAdapter
import com.example.drinkcourt.adapter.ListHistoryAdapter
import com.example.drinkcourt.async.AsyncBluetoothEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrinter
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.FragmentHistoryBinding
import com.example.drinkcourt.model.Cetak
import com.example.drinkcourt.model.History
import com.example.drinkcourt.model.SubTransaksi
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var listTransaksiAdapter: ListHistoryAdapter
    private lateinit var connect: Connect

    private lateinit var sessionLogin: SessionLogin
    private var listTransaksi : MutableList<History> = mutableListOf()
    private lateinit var listCetak: MutableList<Cetak>
    private var getMobileRole = ""

    private var selectedDevice : BluetoothConnection? = null

    companion object{
        private const val REQUEST_ENABLE_BT = 1
    }

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connect = Connect()
        sessionLogin = SessionLogin(requireContext())

        getMobileRole = sessionLogin.getMobileRole()

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.shimmerRecyclerView.layoutManager = GridLayoutManager(requireContext(), requireView().resources.getInteger(R.integer.grid_items))
        binding.shimmerRecyclerView.setHasFixedSize(false)
        binding.shimmerRecyclerView.hideShimmerAdapter()

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.loadItemsForHistory(requireContext())
            }
            showList()
        }

        mainViewModel.getDataHistory().observe(viewLifecycleOwner, Observer { items ->
            if (items != null){
                listTransaksi.clear()
                listTransaksi.addAll(items)
                listTransaksiAdapter = ListHistoryAdapter(requireContext(), listTransaksi)
                binding.shimmerRecyclerView.adapter = listTransaksiAdapter
                listTransaksiAdapter.notifyDataSetChanged()
                showList()
                Log.e("Hasil", listTransaksi.toString())
            }
        })

        setPermission()

        binding.refreshPesananChecker.setOnRefreshListener {
            mainViewModel.loadItemsForHistory(requireContext())
            binding.refreshPesananChecker.setRefreshing(false)
            showList()
        }

        binding.etSearchMeja.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etSearchMeja.setOnEditorActionListener(TextView.OnEditorActionListener{_,actionId,_ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                listTransaksiAdapter.filter.filter(binding.etSearchMeja.text.toString())
                binding.imgClearMeja.visibility = View.VISIBLE
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        binding.imgClearMeja.setOnClickListener {
            binding.etSearchMeja.setText("")
            binding.imgClearMeja.visibility = View.GONE
            listTransaksiAdapter.filter.filter("")
        }

        binding.etSearch.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etSearch.setOnEditorActionListener(TextView.OnEditorActionListener{_,actionId,_ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                mainViewModel.loadItemsForHistorySearch(requireContext(), binding.etSearch.text.toString())
                showList()
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
            mainViewModel.loadItemsForHistory(requireContext())
            showList()
        }

        binding.tvPilihPrinter.setOnClickListener {
            checkBluetoothPermissions()
        }
    }

    private fun showList(){

        lifecycleScope.launch {
            val sizeItem = listTransaksi.size
            if (sizeItem > 0){
                (activity as AppCompatActivity).supportActionBar?.title = "HISTORY  " + if (sizeItem > 0){
                    "$sizeItem ITEMS"
                }else{
                    ""
                }
                binding.refreshPesananChecker.setRefreshing(false)
                binding.shimmerRecyclerView.hideShimmerAdapter()
                listTransaksiAdapter.setOnItemClickCallback(object : ListHistoryAdapter.OnItemClickCallback{
                    override fun onItemClicked(transaksi: History) {

                        if (getMobileRole == "ADMIN"){
                            val dialog = Dialog(requireContext())
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                            dialog.setContentView(R.layout.bottom_pesanan)
                            dialog.setCanceledOnTouchOutside(false)

                            val tvMeja = dialog.findViewById<TextView>(R.id.tvMejaBottom)
                            val rv = dialog.findViewById<RecyclerView>(R.id.rvListBottom)
                            val tvSelectRunner = dialog.findViewById<TextView>(R.id.tvPilihRunner)
                            val btnUpdate = dialog.findViewById<RelativeLayout>(R.id.btnUpdateTable)
                            val llBottom = dialog.findViewById<LinearLayout>(R.id.llBotom)
                            val llBottomClose = dialog.findViewById<LinearLayout>(R.id.llBotomClose)
                            val btnCloseBottom = dialog.findViewById<RelativeLayout>(R.id.btnCloseChecker)
                            val btnKirim = dialog.findViewById<RelativeLayout>(R.id.btnKirimBottom)
                            val btnCetakBottom = dialog.findViewById<RelativeLayout>(R.id.btnCetakBottom)
                            val btnReprint = dialog.findViewById<RelativeLayout>(R.id.btnReprint)

                            btnKirim.visibility = View.GONE
                            btnReprint.visibility = View.GONE
                            btnCetakBottom.visibility = View.VISIBLE

                            btnUpdate.visibility = View.VISIBLE
                            tvSelectRunner.textSize = 22f
                            tvSelectRunner.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(transaksi.date.toString()))

                            llBottom.visibility = View.VISIBLE
                            llBottomClose.visibility = View.VISIBLE

                            rv.visibility = View.VISIBLE
                            rv.layoutManager = LinearLayoutManager(requireContext())
                            rv.setHasFixedSize(false)

                            var listBottomAdapter1: ListBottomHistoryAdapter

                            tvMeja.text = transaksi.noMeja.toString()
//                            tvSelectRunner.text = "PILIH RUNNER"

                            val listBottom = mutableListOf<SubTransaksi>()
                            CoroutineScope(Dispatchers.Main + Job()).launch {
                                try {
                                    val jsonArray = JSONArray(transaksi.detail)
                                    for (i in 0 until jsonArray.length()){
                                        val getObject = jsonArray.getJSONObject(i)
                                        val subTransaksi = SubTransaksi()
                                        subTransaksi.meja = transaksi.noMeja
                                        subTransaksi.namaMenu = getObject.getString("MenuName")
                                        subTransaksi.menuId   = getObject.getString("MenuID")
                                        subTransaksi.qty      = getObject.getInt("Qty")
                                        subTransaksi.pref     = getObject.getString("Request")
                                        subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                                        subTransaksi.maxQty   = getObject.getInt("Qty")
                                        subTransaksi.checker = getObject.getString("Checker")
                                        subTransaksi.record = getObject.getString("RecordedDateTime")
                                        listBottom.add(subTransaksi)
                                    }
                                }catch (e: JSONException){
                                    e.printStackTrace()
                                }
                                listBottomAdapter1 = ListBottomHistoryAdapter(listBottom, transaksi.date.toString(), true)
                                rv.adapter = listBottomAdapter1

                                btnCetakBottom.setOnClickListener {

                                    if(selectedDevice != null){
                                        val progressBarReprint = ProgressDialog(context)
                                        progressBarReprint.setTitle("Mohon tunggu!!")
                                        progressBarReprint.setMessage("Sedang mengambil data...")
                                        progressBarReprint.setCanceledOnTouchOutside(false)
                                        progressBarReprint.show()

                                        lifecycleScope.launch {
                                            val resultCetak = withContext(
                                                Dispatchers.IO){
                                                loadBill(transaksi.salesId!!.toInt())
                                            }

                                            when(resultCetak){
                                                "Berhasil" -> {
                                                    progressBarReprint.dismiss()
                                                    withContext(Dispatchers.IO){
                                                        printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString())
                                                    }
                                                }
                                                "Gagal" -> {
                                                    progressBarReprint.dismiss()
                                                    Toast.makeText(requireContext(), "Gagal mengambil data!!",
                                                        Toast.LENGTH_SHORT).show()
                                                }
                                                else -> {
                                                    progressBarReprint.dismiss()
                                                    Toast.makeText(requireContext(), resultCetak,
                                                        Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }

                                btnCloseBottom.setOnClickListener {
                                    dialog.dismiss()
                                }
                            }

                            dialog.show()
                            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                            dialog.window?.setGravity(Gravity.BOTTOM)
                        }
                        else{
                            if (selectedDevice == null){
                                Toast.makeText(requireContext(), "Anda wajib memilih printer!!", Toast.LENGTH_SHORT).show()
                            }
                            else{
                                val dialog = Dialog(requireContext())
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                dialog.setContentView(R.layout.bottom_pesanan)
                                dialog.setCanceledOnTouchOutside(false)

                                val tvMeja = dialog.findViewById<TextView>(R.id.tvMejaBottom)
                                val rv = dialog.findViewById<RecyclerView>(R.id.rvListBottom)
                                val tvSelectRunner = dialog.findViewById<TextView>(R.id.tvPilihRunner)
                                val btnUpdate = dialog.findViewById<RelativeLayout>(R.id.btnUpdateTable)
                                val llBottom = dialog.findViewById<LinearLayout>(R.id.llBotom)
                                val llBottomClose = dialog.findViewById<LinearLayout>(R.id.llBotomClose)
                                val btnCloseBottom = dialog.findViewById<RelativeLayout>(R.id.btnCloseChecker)
                                val btnKirim = dialog.findViewById<RelativeLayout>(R.id.btnKirimBottom)
                                val btnCetakBottom = dialog.findViewById<RelativeLayout>(R.id.btnCetakBottom)
                                val btnReprint = dialog.findViewById<RelativeLayout>(R.id.btnReprint)

                                btnKirim.visibility = View.GONE
                                btnReprint.visibility = View.GONE
                                btnCetakBottom.visibility = View.VISIBLE

                                btnUpdate.visibility = View.VISIBLE
                                tvSelectRunner.textSize = 22f
                                tvSelectRunner.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(transaksi.date.toString()))

                                llBottom.visibility = View.VISIBLE
                                llBottomClose.visibility = View.VISIBLE

                                rv.visibility = View.VISIBLE
                                rv.layoutManager = LinearLayoutManager(requireContext())
                                rv.setHasFixedSize(false)

                                var listBottomAdapter1: ListBottomHistoryAdapter

                                tvMeja.text = transaksi.noMeja.toString()
//                                tvSelectRunner.text = "PILIH RUNNER"

                                val listBottom = mutableListOf<SubTransaksi>()
                                CoroutineScope(Dispatchers.Main + Job()).launch {
                                    try {
                                        val jsonArray = JSONArray(transaksi.detail)
                                        for (i in 0 until jsonArray.length()){
                                            val getObject = jsonArray.getJSONObject(i)
                                            val subTransaksi = SubTransaksi()
                                            subTransaksi.meja = transaksi.noMeja
                                            subTransaksi.namaMenu = getObject.getString("MenuName")
                                            subTransaksi.menuId   = getObject.getString("MenuID")
                                            subTransaksi.qty      = getObject.getInt("Qty")
                                            subTransaksi.pref     = getObject.getString("Request")
                                            subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                                            subTransaksi.maxQty   = getObject.getInt("Qty")
                                            subTransaksi.checker = getObject.getString("Checker")
                                            subTransaksi.record = getObject.getString("RecordedDateTime")
                                            listBottom.add(subTransaksi)
                                        }
                                    }catch (e: JSONException){
                                        e.printStackTrace()
                                    }
                                    listBottomAdapter1 = ListBottomHistoryAdapter(listBottom, transaksi.date.toString(), true)
                                    rv.adapter = listBottomAdapter1


                                    btnCetakBottom.setOnClickListener {
                                        if (getMobileRole == "VIEWER"){
                                            Toast.makeText(context, "Viewer tidak diijinkan untuk ngeprint!!", Toast.LENGTH_LONG).show()
                                        }else{
                                            val progressBarReprint = ProgressDialog(context)
                                            progressBarReprint.setTitle("Mohon tunggu!!")
                                            progressBarReprint.setMessage("Sedang mengambil data...")
                                            progressBarReprint.setCanceledOnTouchOutside(false)
                                            progressBarReprint.show()

                                            lifecycleScope.launch {
                                                val resultCetak = withContext(
                                                    Dispatchers.IO){
                                                    loadBill(transaksi.salesId!!.toInt())
                                                }

                                                when(resultCetak){
                                                    "Berhasil" -> {
                                                        progressBarReprint.dismiss()
                                                        withContext(Dispatchers.IO){
                                                            printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString())
                                                        }
                                                    }
                                                    "Gagal" -> {
                                                        progressBarReprint.dismiss()
                                                        Toast.makeText(requireContext(), "Gagal mengambil data!!",
                                                            Toast.LENGTH_SHORT).show()
                                                    }
                                                    else -> {
                                                        progressBarReprint.dismiss()
                                                        Toast.makeText(requireContext(), resultCetak,
                                                            Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }

                                    }


                                    btnCloseBottom.setOnClickListener {
                                        dialog.dismiss()
                                    }
                                }

                                dialog.show()
                                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                                dialog.window?.setGravity(Gravity.BOTTOM)
                            }
                        }
                    }
                })
            }else{
                (activity as AppCompatActivity).supportActionBar?.title = "HISTORY"
            }
            binding.refreshPesananChecker.setRefreshing(false)
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
                    binding.tvPilihPrinter.text = spanString
                }
                val alert = alertDialog.create()
                alert.setCanceledOnTouchOutside(true)
                alert.show()
            }
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
                    Snackbar.make(binding.root, "Bluetooth Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }

            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Admin Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }

            103 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Connect Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }

            104 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Scan Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != AppCompatActivity.RESULT_OK) {
                Snackbar.make(binding.root, "Bluetooth wajib dihidupkan!!", Snackbar.LENGTH_SHORT).show()
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

    private suspend fun printBluetoothOrder(meja: String, by: String):String {
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
            ).execute(getAsyncEscPosPrinterOrder(selectedDevice!!, meja, by)).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinterOrder(printerConnection: DeviceConnection, meja: String, by:String): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
            try {
                printer.addTextToPrint("" +
//                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logoc, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
//                        "[L]\n" +
                        "[C]<b><font size='big-3'>$meja</font></b>\n" +
                        "[C]\n" +
//                        "[C]<b><font size='normal'>Kota Medan</font></b>\n" +
//                        "[C]\n" +
//                        "[L]No Meja      [R]: [R]$meja\n" +
                        "[L]Tanggal      [R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu        [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]Cashir       [R]: [R]${by}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listCetak.joinToString("\n") { it -> "[L]${it.menuName}\n" +
                                "[L]${it.qtyCetak}"} +
                        "\n[L]================================\n" +
//                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
                        "[L]ITEMS        [R]: [R]${listCetak.sumOf { it.qtyCetak!!.toInt() }}\n" +
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

    private fun getCurrentDate(format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }
}