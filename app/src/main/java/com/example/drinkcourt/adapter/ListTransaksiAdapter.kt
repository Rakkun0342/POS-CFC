package com.example.drinkcourt.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
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

class ListTransaksiAdapter( private val context: Context): RecyclerView.Adapter<ListTransaksiAdapter.ViewHolder>() {

    private val listTransaksi: MutableList<Transaksi> = mutableListOf()

    fun setData(item: MutableList<Transaksi>){
        listTransaksi.clear()
        listTransaksi.addAll(item)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(transaksi: Transaksi)
    }

    inner class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView) {
        private val tvLastMod = itemView.findViewById<TextView>(R.id.tvLastModTransaksi)
        private val tvMeja = itemView.findViewById<TextView>(R.id.tvNomorMejaTransaksi)
        private val rvSub = itemView.findViewById<RecyclerView>(R.id.rvSub)
        private val cv = itemView.findViewById<CardView>(R.id.cvPesanan)
        fun bind(transaksi: Transaksi){

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateMod    = dateFormat.parse(transaksi.date.toString())

            val range = Date().time - dateMod!!.time
            val seconds = range / 1000
            val minutes: Long = seconds / 60
//            Log.e("hasil menit", minutes.toString())

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

            itemView.setOnClickListener { onItemClickCallback!!.onItemClicked(transaksi) }

            rvSub.layoutManager = LinearLayoutManager(context)
            rvSub.setHasFixedSize(false)

            val listSub = mutableListOf<SubTransaksi>()
            CoroutineScope(Dispatchers.Main + Job()).launch {
                try {
                    val jsonArray = JSONArray(transaksi.detail)
                    for (i in 0 until jsonArray.length()){
                        val getObject = jsonArray.getJSONObject(i)
                        val subTransaksi = SubTransaksi()
                        subTransaksi.detailId = getObject.getInt("DetailID")
                        subTransaksi.namaMenu = getObject.getString("MenuName")
                        subTransaksi.menuId = getObject.getString("MenuID")
                        subTransaksi.qty = getObject.getInt("Qty")
                        subTransaksi.pref = getObject.getString("Request")
                        subTransaksi.colorStatus = getObject.getInt("ColorStatus")
                        subTransaksi.higlight = getObject.getBoolean("Highlight")
                        listSub.add(subTransaksi)
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
                rvSub.adapter = ListSubTransaksi(listSub)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listTransaksi.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listTransaksi[position])
    }
}