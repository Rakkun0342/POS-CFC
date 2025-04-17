package com.example.drinkcourt.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Bill
import com.example.drinkcourt.model.Cetak
import java.text.NumberFormat
import java.util.Locale

class CetakAdapter(private val listCetak: MutableList<Cetak>): RecyclerView.Adapter<CetakAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(cetak: Cetak)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvMenuCetak)
        private val tvQty = itemView.findViewById<TextView>(R.id.tvQtyCetak)
        private val tvTotal = itemView.findViewById<TextView>(R.id.tvTotalCetak)
        fun bind(cetak: Cetak) {
            tvName.text = cetak.menuName
            tvQty.text = cetak.qtyCetak.toString()
            Log.e("hasil", cetak.menuName.toString())
            tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(cetak.totalCetak).replace(",",".")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_cetak, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listCetak.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listCetak[position])
    }
}