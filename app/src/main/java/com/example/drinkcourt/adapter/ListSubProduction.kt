package com.example.drinkcourt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.SubTransaksi

class ListSubProduction(private val listTransaksi: MutableList<SubTransaksi>): RecyclerView.Adapter<ListSubProduction.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMenu = itemView.findViewById<TextView>(R.id.tvMenuPengantaran)
        private val tvQty = itemView.findViewById<TextView>(R.id.tvQtPengantaran)
        private val tvNote = itemView.findViewById<TextView>(R.id.tvNotePengantaran)
        fun bind(subTransaksi: SubTransaksi, position: Int) {
//            if (position == 4){
//                tvMenu.text = "Lainnya....n"
//                tvQty.visibility = View.GONE
//            }else if (position < 4){
                tvMenu.setBackgroundColor(itemView.resources.getColor(R.color.transparant))
                if (subTransaksi.higlight == true){
                    tvMenu.setBackgroundColor(itemView.resources.getColor(R.color.orange_yelow));
                }
                tvQty.text = subTransaksi.qty.toString()
                if (subTransaksi.pref != ""){
                    tvNote.visibility = View.VISIBLE
                    tvMenu.text = subTransaksi.namaMenu
                    tvNote.text = subTransaksi.pref
                }else{
                    tvNote.visibility = View.GONE
                    tvMenu.text = subTransaksi.namaMenu
                }
                when (subTransaksi.colorStatus){
                    0 -> {
                        tvQty.setTextColor(itemView.context.resources.getColor(android.R.color.black))
                        tvMenu.setTextColor(itemView.context.resources.getColor(android.R.color.black))
                    }
                    1 -> {
                        tvQty.setTextColor(itemView.context.resources.getColor(android.R.color.holo_orange_dark))
                        tvMenu.setTextColor(itemView.context.resources.getColor(android.R.color.holo_orange_dark))
                    }
                    2 -> {
                        tvQty.setTextColor(itemView.context.resources.getColor(android.R.color.holo_red_light))
                        tvMenu.setTextColor(itemView.context.resources.getColor(android.R.color.holo_red_light))
                    }
                }
//            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_pengantaran, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listTransaksi.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listTransaksi[position], position)
    }
}