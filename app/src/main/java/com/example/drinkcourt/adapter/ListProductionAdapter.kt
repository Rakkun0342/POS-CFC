package com.example.drinkcourt.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Items
import com.example.drinkcourt.model.SubTransaksi
import com.example.drinkcourt.model.Transaksi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ListProductionAdapter(private val context: Context): RecyclerView.Adapter<ListProductionAdapter.ViewHolder>() {

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
        private val tvMeja = itemView.findViewById<TextView>(R.id.tvNomorMejaTransaksi)
        private val rvSub = itemView.findViewById<RecyclerView>(R.id.rvSub)
        private val cv = itemView.findViewById<CardView>(R.id.cvPesanan)
        fun bind(transaksi: Transaksi){

            if (transaksi.group == "MERAH"){
                tvMeja.setTextColor(itemView.resources.getColor(android.R.color.holo_red_light))
            }else if (transaksi.group == "BIRU"){
                tvMeja.setTextColor(itemView.resources.getColor(android.R.color.holo_blue_light))
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
                        listSub.add(subTransaksi)
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
                rvSub.adapter = ListSubProduction(listSub)
            }
            itemView.setOnClickListener { onItemClickCallback!!.onItemClicked(transaksi) }
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