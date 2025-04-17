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
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
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
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.drinkcourt.R
import com.example.drinkcourt.adapter.CetakAdapter
import com.example.drinkcourt.adapter.GvAdapter
import com.example.drinkcourt.adapter.ListBottomAdapter
import com.example.drinkcourt.adapter.ListTransaksiAdapter
import com.example.drinkcourt.async.AsyncBluetoothEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrint
import com.example.drinkcourt.async.AsyncEscPosPrinter
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.databinding.FragmentCheckerV2Binding
import com.example.drinkcourt.databinding.FragmentTransaksiBinding
import com.example.drinkcourt.model.Cetak
import com.example.drinkcourt.model.Produc
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

class CheckerV2Fragment : Fragment() {

    private var _binding: FragmentCheckerV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var listTransaksiAdapter: ListTransaksiAdapter
    private lateinit var connect: Connect

    private lateinit var sessionLogin: SessionLogin
    private var listTransaksi : MutableList<Transaksi> = mutableListOf()
    private lateinit var listCetakOne: MutableList<Cetak>
    private var listUserRunner: ArrayList<String> = arrayListOf()

    private var selectRunner = ""
    private var getUserLogin = ""
    private var getMobileRole = ""

    private var resultOpen = ""

    private var selectedDevice : BluetoothConnection? = null

    companion object{
        private const val REQUEST_ENABLE_BT = 1
    }

    private lateinit var mainViewModel: MainViewModel
    private var onClickSales = 0
    private var resultQty = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckerV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connect = Connect()
        sessionLogin  = SessionLogin(requireContext())

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                loadItemsUser()
            }
            listUserRunner.sortBy{ it.toString() }
        }

        getMobileRole = sessionLogin.getMobileRole()

        binding.shimmerRecyclerView.layoutManager = GridLayoutManager(context, requireView().resources.getInteger(R.integer.grid_items))
        binding.shimmerRecyclerView.setHasFixedSize(false)
        binding.shimmerRecyclerView.showShimmerAdapter()

        listTransaksiAdapter = ListTransaksiAdapter(requireContext())

        lifecycleScope.launch {

            getUserLogin = if (sessionLogin.getMobileRole() == "CHECKER" || sessionLogin.getMobileRole() == "ADMIN" || sessionLogin.getMobileRole() == "VIEWER"){
                ""
            }else{
                sessionLogin.getMobileRole().replace("CHECKER_", "")
            }

            val progressBar = ProgressDialog(context)
            progressBar.setMessage("Mohon tunggu...")
            progressBar.setCanceledOnTouchOutside(false)
            progressBar.show()

            withContext(Dispatchers.IO){
                mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
            }
            showList()

            progressBar.dismiss()
        }

        mainViewModel.getDataChecker().observe(viewLifecycleOwner, Observer { items ->
            if (items != null){
                listTransaksi.clear()
                listTransaksi.addAll(items)
                listTransaksiAdapter.setData(listTransaksi)
                showList()
            }
        })

        binding.shimmerRecyclerView.adapter = listTransaksiAdapter

        setPermission()

        binding.refreshPesananChecker.setOnRefreshListener {
            lifecycleScope.launch {
//                binding.shimmerRecyclerView.visibility = View.GONE
                binding.refreshPesananChecker.setRefreshing(true)
                withContext(Dispatchers.IO){
                    mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
                }
                showList()
            }
        }

        binding.etSearch.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etSearch.setOnEditorActionListener(TextView.OnEditorActionListener{_,actionId,_ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                mainViewModel.loadItemsForCheckerSearch(requireContext(), binding.etSearch.text.toString(), getUserLogin)
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
            mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
            showList()
        }

        binding.tvPilihPrinter.setOnClickListener {
            checkBluetoothPermissions()
        }
    }

    private fun loadItemsUser(): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val queryUser = "EXEC USP_J_Mobile_QueryStatic @Action = 'User'"
                val preparedStatementUser = conn.createStatement()
                val rsUser = preparedStatementUser.executeQuery(queryUser)
                listUserRunner.clear()
                while (rsUser.next()){
                    listUserRunner.add(rsUser.getString("UserName"))
                }
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
        return "Gagal"
    }

    private fun showList(){
        lifecycleScope.launch {

            val sizeItem = listTransaksi.size
            if (sizeItem > 0){

                (activity as AppCompatActivity).supportActionBar?.title = "CHECKER  " + if (sizeItem > 0){
                    "$sizeItem ITEMS"
                }else{
                    ""
                }

                binding.shimmerRecyclerView.visibility = View.VISIBLE
                binding.refreshPesananChecker.setRefreshing(false)
                binding.shimmerRecyclerView.hideShimmerAdapter()
                listTransaksiAdapter.setOnItemClickCallback(object : ListTransaksiAdapter.OnItemClickCallback{
                    override fun onItemClicked(transaksi: Transaksi) {
                        if (getMobileRole == "VIEWER"){
                            Toast.makeText(context, "Viewer tidak diijinkan untuk membuka!!", Toast.LENGTH_LONG).show()
                        }else{
                            if (getMobileRole == "ADMIN"){

                                selectRunner = ""

                                onClickSales = transaksi.salesId!!.toInt()

                                val dialog = Dialog(context!!)
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                dialog.setContentView(R.layout.bottom_pesanan)
                                dialog.setCanceledOnTouchOutside(false)

                                val tvMeja= dialog.findViewById<TextView>(R.id.tvMejaBottom)
                                val rv = dialog.findViewById<RecyclerView>(R.id.rvListBottom)
                                val tvSelectRunner= dialog.findViewById<TextView>(R.id.tvPilihRunner)
                                val btnUpdate= dialog.findViewById<RelativeLayout>(R.id.btnUpdateTable)
                                val llBottom= dialog.findViewById<LinearLayout>(R.id.llBotom)
                                val llBottomClose= dialog.findViewById<LinearLayout>(R.id.llBotomClose)
                                val btnCloseBottom= dialog.findViewById<RelativeLayout>(R.id.btnCloseChecker)
                                val btnKirim= dialog.findViewById<RelativeLayout>(R.id.btnKirimBottom)
                                val btnCetakBottom= dialog.findViewById<RelativeLayout>(R.id.btnCetakBottom)
                                val btnReprint= dialog.findViewById<RelativeLayout>(R.id.btnReprint)

                                btnCetakBottom.visibility = View.GONE
                                llBottom.visibility = View.VISIBLE
                                llBottomClose.visibility = View.VISIBLE

                                rv.visibility = View.VISIBLE
                                rv.layoutManager = LinearLayoutManager(context!!)
                                rv.setHasFixedSize(false)

                                var listBottomAdapter1: ListBottomAdapter

                                tvMeja.text = transaksi.noMeja.toString()
                                tvSelectRunner.text = "PILIH RUNNER"

                                val openBill: MutableList<Cetak> = mutableListOf()

                                btnUpdate.setOnClickListener {
                                    val dialogCetak = Dialog(requireContext())
                                    dialogCetak.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                    dialogCetak.setContentView(R.layout.dialog_list_table)
                                    val listQty = dialogCetak.findViewById<GridView>(R.id.list_view_meja)
                                    val tvJudulRunner = dialogCetak.findViewById<TextView>(R.id.tvJudulMeja)
                                    val btnCloseMeja = dialogCetak.findViewById<ImageView>(R.id.btnCloseMeja)

                                    tvJudulRunner.text = "PILIH RUNNER"

                                    listQty.numColumns = 4
                                    val adapter = GvAdapter(listUserRunner, requireContext(), true)
                                    listQty.adapter = adapter

                                    listQty.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
                                        tvSelectRunner.text = listUserRunner[position]
                                        selectRunner = listUserRunner[position]
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

                                val listBottom = mutableListOf<SubTransaksi>()
                                CoroutineScope(Dispatchers.Main + Job()).launch {
                                    try {
                                        val jsonArray = JSONArray(transaksi.detail)
                                        for (i in 0 until jsonArray.length()){
                                            val getObject = jsonArray.getJSONObject(i)
                                            val subTransaksi = SubTransaksi()
                                            subTransaksi.detailId = getObject.getInt("DetailID")
                                            subTransaksi.meja = transaksi.noMeja
                                            subTransaksi.namaMenu = getObject.getString("MenuName")
                                            subTransaksi.menuId   = getObject.getString("MenuID")
                                            subTransaksi.qty      = getObject.getInt("Qty")
                                            subTransaksi.pref     = getObject.getString("Request")
                                            subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                                            subTransaksi.maxQty   = getObject.getInt("Qty")
                                            listBottom.add(subTransaksi)
                                        }
                                    }catch (e: JSONException){
                                        e.printStackTrace()
                                    }
                                    listBottomAdapter1 = ListBottomAdapter(listBottom, context!!, true)
                                    rv.adapter = listBottomAdapter1
                                    listBottomAdapter1.setOnItemClickCallback(object: ListBottomAdapter.OnItemClickCallback{
                                        override fun onItemClicked(subTransaksi: SubTransaksi, position:Int) {
                                            if (selectRunner == ""){
                                                Toast.makeText(context, "Silahkan pilih runner", Toast.LENGTH_SHORT).show()
                                            }else{
                                                val progressBar1 = ProgressDialog(context)
                                                progressBar1.setTitle("Mohon tunggu!!")
                                                progressBar1.setMessage("Sedang mengirim pesanan..")
                                                progressBar1.setCanceledOnTouchOutside(false)
                                                progressBar1.setCancelable(false)
                                                progressBar1.show()

                                                CoroutineScope(Dispatchers.Main + Job()).launch {
                                                    val result1 = withContext(Dispatchers.IO){
                                                        sendPengirimanV2(subTransaksi.detailId!!.toInt(), transaksi.group.toString(), subTransaksi.qty!!.toInt(), selectRunner, getUserLogin)
                                                    }
                                                    when(result1){
                                                        "1" ->{
                                                            btnCetakBottom.visibility = View.VISIBLE

                                                            if (resultQty > 0){
                                                                Toast.makeText(context, "${subTransaksi.namaMenu} sudah dikeluarkan $resultQty", Toast.LENGTH_SHORT).show()
                                                            }

                                                            openBill.add(Cetak(null, subTransaksi.namaMenu, (subTransaksi.qty!! - resultQty), null, null, null))

                                                            subTransaksi.maxQty = subTransaksi.maxQty!! - (subTransaksi.qty!! - resultQty)
                                                            subTransaksi.qty = subTransaksi.maxQty

                                                            withContext(Dispatchers.IO){
                                                                mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
                                                            }

                                                            progressBar1.dismiss()

                                                            if (subTransaksi.maxQty == 0){
                                                                listBottom.removeAt(position)
                                                                if (listBottom.size == 0){
                                                                    lifecycleScope.launch {
                                                                        if (selectedDevice != null){
                                                                            withContext(Dispatchers.IO){
                                                                                printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString(), openBill)
                                                                            }
                                                                        }
                                                                        Toast.makeText(context, "Berhasil mencetak struk!!", Toast.LENGTH_SHORT).show()
                                                                        rv.visibility = View.GONE
                                                                        btnKirim.visibility = View.GONE
                                                                        btnCetakBottom.visibility = View.GONE
                                                                        btnReprint.visibility = View.VISIBLE
                                                                    }
                                                                }
                                                                listBottomAdapter1.notifyItemRemoved(position)
                                                            }
                                                            listBottomAdapter1.notifyDataSetChanged()
                                                        }
                                                        "0"-> {
                                                            progressBar1.dismiss()
                                                            withContext(Dispatchers.IO){
                                                                mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
                                                            }
                                                            Toast.makeText(context, "${subTransaksi.namaMenu} sudah dikeluarkan", Toast.LENGTH_SHORT).show()
                                                        }
                                                        "Gagal"-> {
                                                            progressBar1.dismiss()
                                                            Toast.makeText(context, "Tidak terhubung keserver", Toast.LENGTH_SHORT).show()
                                                        }
                                                        else -> {
                                                            progressBar1.dismiss()
                                                            Toast.makeText(context, result1, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    })

                                    btnKirim.setOnClickListener {
                                        if (selectRunner == ""){
                                            Toast.makeText(context, "Silahkan pilih runner", Toast.LENGTH_SHORT).show()
                                        }else{

                                            val progressBar2 = ProgressDialog(context)
                                            progressBar2.setTitle("Mohon tunggu!!")
                                            progressBar2.setCanceledOnTouchOutside(false)
                                            progressBar2.setCancelable(false)
                                            progressBar2.max = listBottom.size
                                            progressBar2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                                            progressBar2.show()
                                            var terkirim = 0

                                            CoroutineScope(Dispatchers.Main + Job()).launch {
                                                val itemsToRemove = mutableListOf<Int>()

                                                try {
                                                    val iterator = listBottom.iterator()
                                                    while (iterator.hasNext()) {
                                                        val item = iterator.next()
                                                        val result1 = withContext(Dispatchers.IO) {
                                                            sendPengirimanV2(item.detailId!!.toInt(), transaksi.group.toString(), item.qty!!.toInt(), selectRunner, getUserLogin
                                                            )
                                                        }

                                                        when (result1) {
                                                            "1" -> {
                                                                btnCetakBottom.visibility = View.VISIBLE

                                                                if (resultQty > 0) { Toast.makeText(context, "${item.namaMenu} sudah dikeluarkan $resultQty", Toast.LENGTH_SHORT).show() }

                                                                withContext(Dispatchers.IO) {
                                                                    openBill.add(
                                                                        Cetak(
                                                                            null,
                                                                            item.namaMenu,
                                                                            (item.qty!!.toInt() - resultQty),
                                                                            null,
                                                                            null,
                                                                            null
                                                                        )
                                                                    )
                                                                }

                                                                item.maxQty = item.maxQty!! - (item.qty!! - resultQty)
                                                                item.qty = item.maxQty

                                                                withContext(Dispatchers.IO) {
                                                                    mainViewModel.loadItemsForChecker(requireContext(), getUserLogin
                                                                    )
                                                                }

                                                                if (item.maxQty == 0) {
                                                                    itemsToRemove.add(terkirim)
                                                                }

                                                                progressBar2.progress = terkirim
                                                                terkirim++

                                                                listBottomAdapter1.notifyDataSetChanged()
                                                            }

                                                            "0" -> {
                                                                progressBar2.progress = terkirim
                                                                terkirim++
                                                                withContext(Dispatchers.IO) { mainViewModel.loadItemsForChecker(requireContext(), getUserLogin) }
                                                                Toast.makeText(context, "${item.namaMenu} sudah dikeluarkan", Toast.LENGTH_SHORT).show()
                                                            }

                                                            "Gagal" -> {
                                                                Toast.makeText(context, "Tidak terhubung keserver", Toast.LENGTH_SHORT).show()
                                                                return@launch
                                                            }

                                                            else -> {
                                                                Toast.makeText(context, result1, Toast.LENGTH_SHORT).show()
                                                                return@launch
                                                            }
                                                        }
                                                    }
                                                } finally {
                                                    withContext(Dispatchers.IO){
                                                        for (index in itemsToRemove.reversed()) {
                                                            withContext(Dispatchers.Main){
                                                                listBottom.removeAt(index)
                                                                listBottomAdapter1.notifyItemRemoved(index)
                                                            }
                                                        }
                                                    }

                                                    if (listBottom.size == 0) {
                                                        if (selectedDevice != null) {
                                                            withContext(Dispatchers.IO) {
                                                                printBluetoothOrder(
                                                                    transaksi.noMeja.toString(),
                                                                    transaksi.lastMod.toString(),
                                                                    openBill
                                                                )
                                                            }
                                                        }
                                                        Toast.makeText(context, "Berhasil mencetak struk!!", Toast.LENGTH_SHORT).show()
                                                        rv.visibility = View.GONE
                                                        btnKirim.visibility = View.GONE
                                                        btnCetakBottom.visibility = View.GONE
                                                        btnReprint.visibility = View.VISIBLE
                                                    }

                                                    progressBar2.dismiss()
                                                }
                                            }
                                        }
                                    }

                                    btnReprint.setOnClickListener {

                                        val progressBarReprint = ProgressDialog(context)
                                        progressBarReprint.setTitle("Mohon tunggu!!")
                                        progressBarReprint.setMessage("Sedang mencetak ulang..")
                                        progressBarReprint.setCanceledOnTouchOutside(false)
                                        progressBarReprint.show()

                                        lifecycleScope.launch {

                                            withContext(Dispatchers.IO){
                                                listBottom.forEach {
                                                    openBill.add(Cetak(null, it.namaMenu, it.qty, null, null, null))
                                                }
                                            }

                                            withContext(Dispatchers.Main){
                                                progressBarReprint.dismiss()
                                                lifecycleScope.launch {
                                                    if (selectedDevice != null){
                                                        withContext(Dispatchers.IO){
                                                            printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString(), openBill)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    btnCetakBottom.setOnClickListener {
                                        lifecycleScope.launch {
                                            if (selectedDevice != null){
                                                withContext(Dispatchers.IO){
                                                    printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString(), openBill)
                                                }
                                            }
                                        }
                                    }

                                    btnCloseBottom.setOnClickListener {
                                        dialog.dismiss()
                                    }
                                }

                                dialog.setOnKeyListener { _, keyCode, event ->
                                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                                        dialog.dismiss()
                                        true
                                    } else {
                                        false
                                    }
                                }

                                dialog.show()
                                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                                dialog.window?.setGravity(Gravity.BOTTOM)
                            }else{
                                if (selectedDevice == null){
                                    Toast.makeText(context, "Anda wajib memilih printer!!", Toast.LENGTH_LONG).show()
                                }
                                else{
                                    val progressBarBottom = ProgressDialog(context)
                                    progressBarBottom.setMessage("Mohon tunggu..")
                                    progressBarBottom.setCanceledOnTouchOutside(false)
                                    progressBarBottom.show()
                                    lifecycleScope.launch {
                                        val result = withContext(Dispatchers.IO){
                                            checkSales(transaksi.salesId!!.toInt(), getUserLogin)
                                        }

                                        when (result){
                                            "Berhasil" -> {
                                                progressBarBottom.dismiss()
                                                if (resultOpen == "1"){
                                                    Toast.makeText(context, "Meja sedang di buka!!", Toast.LENGTH_SHORT).show()
                                                }
                                                else{
                                                    withContext(Dispatchers.IO){
                                                        openSales(transaksi.salesId!!.toInt(), getUserLogin)
                                                    }

                                                    selectRunner = ""

                                                    onClickSales = transaksi.salesId!!.toInt()

                                                    val dialog = Dialog(context!!)
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

                                                    btnCetakBottom.visibility = View.GONE
                                                    llBottom.visibility = View.VISIBLE
                                                    llBottomClose.visibility = View.VISIBLE

                                                    rv.visibility = View.VISIBLE
                                                    rv.layoutManager = LinearLayoutManager(context!!)
                                                    rv.setHasFixedSize(false)

                                                    var listBottomAdapter1: ListBottomAdapter

                                                    tvMeja.text = transaksi.noMeja.toString()
                                                    tvSelectRunner.text = "PILIH RUNNER"

                                                    val openBill: MutableList<Cetak> = mutableListOf()

                                                    btnUpdate.setOnClickListener {
                                                        val dialogCetak = Dialog(requireContext())
                                                        dialogCetak.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                                        dialogCetak.setContentView(R.layout.dialog_list_table)
                                                        val listQty = dialogCetak.findViewById<GridView>(R.id.list_view_meja)
                                                        val tvJudulRunner = dialogCetak.findViewById<TextView>(R.id.tvJudulMeja)
                                                        val btnCloseMeja = dialogCetak.findViewById<ImageView>(R.id.btnCloseMeja)

                                                        tvJudulRunner.text = "PILIH RUNNER"

                                                        listQty.numColumns = 4
                                                        val adapter = GvAdapter(listUserRunner, requireContext(), true)
                                                        listQty.adapter = adapter

                                                        listQty.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
                                                            tvSelectRunner.text = listUserRunner[position]
                                                            selectRunner = listUserRunner[position]
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

                                                    val listBottom = mutableListOf<SubTransaksi>()
                                                    CoroutineScope(Dispatchers.Main + Job()).launch {
                                                        try {
                                                            val jsonArray = JSONArray(transaksi.detail)
                                                            for (i in 0 until jsonArray.length()){
                                                                val getObject = jsonArray.getJSONObject(i)
                                                                val subTransaksi = SubTransaksi()
                                                                subTransaksi.meja = transaksi.noMeja
                                                                subTransaksi.detailId = getObject.getInt("DetailID")
                                                                subTransaksi.namaMenu = getObject.getString("MenuName")
                                                                subTransaksi.menuId   = getObject.getString("MenuID")
                                                                subTransaksi.qty      = getObject.getInt("Qty")
                                                                subTransaksi.pref     = getObject.getString("Request")
                                                                subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                                                                subTransaksi.maxQty   = getObject.getInt("Qty")
                                                                listBottom.add(subTransaksi)
                                                            }
                                                        }catch (e: JSONException){
                                                            e.printStackTrace()
                                                        }
                                                        listBottomAdapter1 = ListBottomAdapter(listBottom, context!!, true)
                                                        rv.adapter = listBottomAdapter1
                                                        listBottomAdapter1.setOnItemClickCallback(object: ListBottomAdapter.OnItemClickCallback{
                                                            override fun onItemClicked(subTransaksi: SubTransaksi, position:Int) {
                                                                if (selectRunner == ""){
                                                                    Toast.makeText(context, "Silahkan pilih runner", Toast.LENGTH_SHORT).show()
                                                                }else{
                                                                    val progressBar1 = ProgressDialog(context)
                                                                    progressBar1.setTitle("Mohon tunggu!!")
                                                                    progressBar1.setMessage("Sedang mengirim pesanan..")
                                                                    progressBar1.setCanceledOnTouchOutside(false)
                                                                    progressBar1.setCancelable(false)
                                                                    progressBar1.show()

                                                                    CoroutineScope(Dispatchers.Main + Job()).launch {
                                                                        val result1 = withContext(Dispatchers.IO){
                                                                            sendPengirimanV2(subTransaksi.detailId!!.toInt(), transaksi.group.toString(), subTransaksi.qty!!.toInt(), selectRunner, getUserLogin)
                                                                        }
                                                                        when(result1){
                                                                            "1" ->{
                                                                                btnCetakBottom.visibility = View.VISIBLE

                                                                                if (resultQty > 0){
                                                                                    Toast.makeText(context, "${subTransaksi.namaMenu} sudah dikeluarkan $resultQty", Toast.LENGTH_SHORT).show()
                                                                                }

                                                                                openBill.add(Cetak(null, subTransaksi.namaMenu, (subTransaksi.qty!! - resultQty), null, null, null))

                                                                                subTransaksi.maxQty = subTransaksi.maxQty!! - (subTransaksi.qty!! - resultQty)
                                                                                subTransaksi.qty = subTransaksi.maxQty

                                                                                withContext(Dispatchers.IO){
                                                                                    mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
                                                                                }

                                                                                progressBar1.dismiss()

                                                                                if (subTransaksi.maxQty == 0){
                                                                                    listBottom.removeAt(position)
                                                                                    if (listBottom.size == 0){
                                                                                        lifecycleScope.launch {
                                                                                            withContext(Dispatchers.IO){
                                                                                                printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString(), openBill)
                                                                                            }
                                                                                            Toast.makeText(context, "Berhasil mencetak struk!!", Toast.LENGTH_SHORT).show()
                                                                                            rv.visibility = View.GONE
                                                                                            btnKirim.visibility = View.GONE
                                                                                            btnCetakBottom.visibility = View.GONE
                                                                                            btnReprint.visibility = View.VISIBLE
                                                                                        }
                                                                                    }
                                                                                    listBottomAdapter1.notifyItemRemoved(position)
                                                                                }
                                                                                listBottomAdapter1.notifyDataSetChanged()
                                                                            }
                                                                            "0"-> {
                                                                                progressBar1.dismiss()
                                                                                withContext(Dispatchers.IO){
                                                                                    mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
                                                                                }
                                                                                Toast.makeText(context, "${subTransaksi.namaMenu} sudah dikeluarkan", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                            "Gagal"-> {
                                                                                progressBar1.dismiss()
                                                                                Toast.makeText(context, "Tidak terhubung keserver", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                            else -> {
                                                                                progressBar1.dismiss()
                                                                                Toast.makeText(context, result1, Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        })

                                                        btnKirim.setOnClickListener {
                                                            if (selectRunner == ""){
                                                                Toast.makeText(context, "Silahkan pilih runner", Toast.LENGTH_SHORT).show()
                                                            }else{

                                                                val progressBar2 = ProgressDialog(context)
                                                                progressBar2.setTitle("Mohon tunggu!!")
                                                                progressBar2.setCanceledOnTouchOutside(false)
                                                                progressBar2.setCancelable(false)
                                                                progressBar2.max = listBottom.size
                                                                progressBar2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                                                                progressBar2.show()
                                                                var terkirim = 0

                                                                CoroutineScope(Dispatchers.Main + Job()).launch {
                                                                    val itemsToRemove = mutableListOf<Int>()

                                                                    try {
                                                                        val iterator = listBottom.iterator()
                                                                        while (iterator.hasNext()) {
                                                                            val item = iterator.next()

                                                                            val result1 = withContext(Dispatchers.IO) {
                                                                                sendPengirimanV2(item.detailId!!.toInt(), transaksi.group.toString(), item.qty!!.toInt(), selectRunner, getUserLogin)
                                                                            }

                                                                            when (result1) {
                                                                                "1" -> {
                                                                                    btnCetakBottom.visibility = View.VISIBLE

                                                                                    if (resultQty > 0) { Toast.makeText(context, "${item.namaMenu} sudah dikeluarkan $resultQty", Toast.LENGTH_SHORT).show() }

                                                                                    withContext(Dispatchers.IO) {
                                                                                        openBill.add(Cetak(null, item.namaMenu, (item.qty!!.toInt() - resultQty), null, null, null))
                                                                                    }

                                                                                    item.maxQty = item.maxQty!! - (item.qty!! - resultQty)
                                                                                    item.qty = item.maxQty

                                                                                    withContext(Dispatchers.IO) {
                                                                                        mainViewModel.loadItemsForChecker(requireContext(), getUserLogin)
                                                                                    }

                                                                                    if (item.maxQty == 0) {
                                                                                        itemsToRemove.add(terkirim)
                                                                                    }

                                                                                    progressBar2.progress = terkirim
                                                                                    terkirim++

                                                                                    listBottomAdapter1.notifyDataSetChanged()
                                                                                }

                                                                                "0" -> {
                                                                                    progressBar2.progress = terkirim
                                                                                    terkirim++
                                                                                    withContext(Dispatchers.IO) { mainViewModel.loadItemsForChecker(requireContext(), getUserLogin) }
                                                                                    Toast.makeText(context, "${item.namaMenu} sudah dikeluarkan", Toast.LENGTH_SHORT).show()
                                                                                }

                                                                                "Gagal" -> {
                                                                                    Toast.makeText(context, "Tidak terhubung keserver", Toast.LENGTH_SHORT).show()
                                                                                    return@launch
                                                                                }

                                                                                else -> {
                                                                                    Toast.makeText(context, result1, Toast.LENGTH_SHORT).show()
                                                                                    return@launch
                                                                                }
                                                                            }
                                                                        }
                                                                    } finally {
                                                                        withContext(Dispatchers.IO){
                                                                            for (index in itemsToRemove.reversed()) {
                                                                                withContext(Dispatchers.Main){
                                                                                    listBottom.removeAt(index)
                                                                                    listBottomAdapter1.notifyItemRemoved(index)
                                                                                }
                                                                            }
                                                                        }

                                                                        if (listBottom.size == 0) {
                                                                            withContext(Dispatchers.IO) {
                                                                                printBluetoothOrder(
                                                                                    transaksi.noMeja.toString(),
                                                                                    transaksi.lastMod.toString(),
                                                                                    openBill
                                                                                )
                                                                            }
                                                                            Toast.makeText(context, "Berhasil mencetak struk!!", Toast.LENGTH_SHORT).show()
                                                                            rv.visibility = View.GONE
                                                                            btnKirim.visibility = View.GONE
                                                                            btnCetakBottom.visibility = View.GONE
                                                                            btnReprint.visibility = View.VISIBLE
                                                                        }
                                                                        progressBar2.dismiss()
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        btnReprint.setOnClickListener {

                                                            val progressBarReprint = ProgressDialog(context)
                                                            progressBarReprint.setTitle("Mohon tunggu!!")
                                                            progressBarReprint.setMessage("Sedang mencetak ulang..")
                                                            progressBarReprint.setCanceledOnTouchOutside(false)
                                                            progressBarReprint.show()

                                                            lifecycleScope.launch {

                                                                withContext(Dispatchers.IO){
                                                                    listBottom.forEach {
                                                                        openBill.add(Cetak(null, it.namaMenu, it.qty, null, null, null))
                                                                    }
                                                                }

                                                                withContext(Dispatchers.Main){
                                                                    progressBarReprint.dismiss()
                                                                    lifecycleScope.launch {
                                                                        withContext(Dispatchers.IO){
                                                                            printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString(), openBill)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        btnCetakBottom.setOnClickListener {
                                                            lifecycleScope.launch {
                                                                withContext(Dispatchers.IO){
                                                                    printBluetoothOrder(transaksi.noMeja.toString(), transaksi.lastMod.toString(), openBill)
                                                                }
                                                            }
                                                        }

                                                        btnCloseBottom.setOnClickListener {
                                                            dialog.dismiss()
                                                            closeSales(transaksi.salesId!!.toInt(), getUserLogin)
                                                        }
                                                    }

                                                    dialog.setOnKeyListener { _, keyCode, event ->
                                                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                                                            closeSales(onClickSales, getUserLogin)
                                                            dialog.dismiss()
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }

                                                    dialog.show()
                                                    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                                    dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                                                    dialog.window?.setGravity(Gravity.BOTTOM)
                                                }
                                            }
                                            "Gagal" -> {
                                                progressBarBottom.dismiss()
                                                Toast.makeText(context, "Silahkan cek kembali server!!", Toast.LENGTH_LONG).show()
                                            }else -> {
                                            progressBarBottom.dismiss()
                                            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                        }
                                        }
                                    }
                                }
                            }
                        }

                    }
                })
            }else{
                (activity as AppCompatActivity).supportActionBar?.title = "CHECKER"
            }
            binding.refreshPesananChecker.setRefreshing(false)
        }
    }

    private fun openSales(id: Int, group: String){
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "EXEC USP_J_Sales_CheckedOpen @Action = 'Open', @SalesId = ?, @Group = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                st.setString(2, group)
                st.execute()
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
    }

    private fun closeSales(id: Int, group: String){
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "EXEC USP_J_Sales_CheckedOpen @Action = 'Close', @SalesId = ?, @Group = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                st.setString(2, group)
                st.execute()
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
    }

    private fun checkSales(id: Int, group: String): String{
        val conn = connect.connection(requireContext())
        resultOpen = ""
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_CheckedOpen @Action = 'Check', @SalesId = ?, @Group = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                st.setString(2, group)
                val rs = st.executeQuery()
                while (rs.next()){
                    resultOpen = rs.getString("IsOpen")
                }
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
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
                    binding.tvPilihPrinter?.text = spanString
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

    private suspend fun printBluetooth(by: String, meja: String, listCetak: MutableList<Cetak>):String {
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
            ).execute(getAsyncEscPosPrinter(by, selectedDevice!!, meja, listCetak)).toString()
            result.await()
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

    private suspend fun getAsyncEscPosPrinter(by: String, printerConnection: DeviceConnection, meja: String, listCetak: MutableList<Cetak>): AsyncEscPosPrinter {
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
                        "[L]BY           [R]: [R]${by}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listCetak.joinToString("\n") { it -> "[L]${it.menuName}\n" +
                                "[L]${it.qtyCetak}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.price)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.totalCetak)}"} +
                        "\n[L]================================\n" +
                        "[L]TOTAL        [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() })}\n" +
                        if(listCetak.sumOf { it.disc!!.toInt()} > 0){ "[L]DISC         [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak[0].disc)}\n"
                                    "[L]NET TOTAL         : [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n"
                        } else { "" }+
                        "[L]ITEMS        [R]: [R]${listCetak.sumOf { it.qtyCetak!!.toInt() }}\n" +
//                        "[L]$method      [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n" +
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
//                        "[L]$method      [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n" +
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

    private suspend fun printBluetoothOrder(meja: String, by: String, listCetak: MutableList<Cetak>):String {
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
            ).execute(getAsyncEscPosPrinterOrder(selectedDevice!!, meja, by, listCetak)).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinterOrder(printerConnection: DeviceConnection, meja: String, by:String, listCetak: MutableList<Cetak>): AsyncEscPosPrinter {
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

    private fun sendPengirimanV2(detailId: Int, tableGroup: String, qty: Int, runner:String, group: String): String{
        Log.e("hasil", "$detailId $tableGroup $qty $runner $group")
        val conn = connect.connection(requireContext())
        if (conn != null){
            try {
                val query = "EXEC USP_J_Sales_MarkForChecker_V2 @DetailID = ?, @User = ?, @TableGroup = ?, @Qty = ?, @Runner = ?, @Group = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, detailId)
                preparedStatement.setString(2, sessionLogin.getUserLogin())
                preparedStatement.setString(3, tableGroup)
                preparedStatement.setInt(4, qty)
                preparedStatement.setString(5, runner)
                preparedStatement.setString(6, group)
                val rs = preparedStatement.executeQuery()
                while (rs.next()){
                    resultQty = rs.getInt("Qty")
                    Log.e("hasil result", rs.getString("Result"))
                    return rs.getString("Result")
                }
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun sendPengiriman(tableCode: String, menuId: Int, request: String, qty: Int, runner:String, group: String): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_MarkForChecker @TableCode = ?, @MenuId = ?, @Request = ?, @Qty = ?, @User = ?, @Runner = ?, @TableGroup = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, tableCode)
                preparedStatement.setInt(2, menuId)
                preparedStatement.setString(3, request)
                preparedStatement.setInt(4, qty)
                preparedStatement.setString(5, sessionLogin.getUserLogin())
                preparedStatement.setString(6, runner)
                preparedStatement.setString(7, group)
                preparedStatement.executeQuery()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun sendPengirimanSemua(runner:String, salesId: Int, group: String): String{
        val conn = connect.connection(requireContext())
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_MarkForChecker @User = ?, @Runner = ?, @SalesId = ?, @Group = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, sessionLogin.getUserLogin())
                preparedStatement.setString(2, runner)
                preparedStatement.setInt(3, salesId)
                preparedStatement.setString(4, group)
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