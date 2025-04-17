package com.example.drinkcourt.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.History
import com.example.drinkcourt.model.SubTransaksi
import com.example.drinkcourt.model.Transaksi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListHistoryAdapter(private val context: Context, private val listTransaksi: MutableList<History>): RecyclerView.Adapter<ListHistoryAdapter.ViewHolder>(),
    Filterable {

    private var onItemClickCallback: OnItemClickCallback? = null
    private val searchItem = ArrayList<History>(listTransaksi)

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(transaksi: History)
    }

    inner class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView) {
        private val tvLastMod = itemView.findViewById<TextView>(R.id.tvLastModHistory)
        private val tvMeja = itemView.findViewById<TextView>(R.id.tvNomorMejaHistory)
        private val tvCashir = itemView.findViewById<TextView>(R.id.tvUserCashir)
        private val rvSub = itemView.findViewById<RecyclerView>(R.id.rvSubHistory)
        private val cv = itemView.findViewById<CardView>(R.id.cvPesananHistory)
        fun bind(transaksi: History){

            tvCashir.text = transaksi.lastMod

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateMod    = dateFormat.parse(transaksi.date.toString())
            val dateComplet = dateFormat2.parse(transaksi.recorded.toString())

            val range = dateComplet!!.time - dateMod!!.time
            val seconds = range / 1000
            val minutes: Long = seconds / 60

            tvLastMod.text = (if (minutes > 0 ){
                "($minutes menit)   " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateMod)
            } else {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateMod)
            }).toString()

            when (transaksi.group) {
                "MERAH" -> {
                    tvMeja.setTextColor(itemView.resources.getColor(android.R.color.holo_red_light))
                }
                "BIRU" -> {
                    tvMeja.setTextColor(itemView.resources.getColor(android.R.color.holo_blue_light))
                }
                "TAKE AWAY" -> {
                    tvMeja.setTextColor(itemView.resources.getColor(android.R.color.black))
                }
                "TENANT" -> {
                    tvMeja.setTextColor(itemView.resources.getColor(android.R.color.holo_green_dark))
                }
                else -> {
                    tvMeja.setTextColor(itemView.resources.getColor(android.R.color.holo_orange_dark))
                }
            }

            tvMeja.text = transaksi.noMeja.toString()

            rvSub.layoutManager = LinearLayoutManager(context)
            rvSub.setHasFixedSize(false)

            val listSub = mutableListOf<SubTransaksi>()
            CoroutineScope(Dispatchers.Main + Job()).launch {
                try {
                    val jsonArray = JSONArray(transaksi.detail)
                    for (i in 0 until jsonArray.length()){
                        val getObject = jsonArray.getJSONObject(i)
                        val subTransaksi = SubTransaksi()
                        subTransaksi.namaMenu = getObject.getString("MenuName")
                        subTransaksi.menuId = getObject.getString("MenuID")
                        subTransaksi.qty = getObject.getInt("Qty")
                        subTransaksi.pref = getObject.getString("Request")
                        subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                        subTransaksi.higlight = getObject.getBoolean("Highlight")
                        subTransaksi.checker = getObject.getString("Checker")
                        subTransaksi.record = getObject.getString("RecordedDateTime")
                        listSub.add(subTransaksi)
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
                rvSub.adapter = ListSubTransaksi(listSub)
            }
            itemView.setOnClickListener { onItemClickCallback!!.onItemClicked(transaksi) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listTransaksi.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listTransaksi[position])
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList = ArrayList<History>()

                if (p0!!.isBlank() or p0.isEmpty()){
                    filteredList.addAll(searchItem)
                }else{
                    val filterPatern = p0.toString().toLowerCase(Locale.ROOT).trim()

                    searchItem.forEach{
                        if (it.noMeja!!.toString().toLowerCase(Locale.ROOT).contains(filterPatern)){
                            filteredList.add(it)
                        }
                    }
                }

                val result = FilterResults()
                result.values = filteredList
                return result
            }

            override fun publishResults(p0: CharSequence, p1: FilterResults) {
                listTransaksi.clear()
                listTransaksi.addAll(p1.values as List<History>)
                notifyDataSetChanged()
            }
        }
    }
}