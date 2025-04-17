package com.example.drinkcourt.adapter

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Checker
import com.example.drinkcourt.model.Items
import com.example.drinkcourt.model.Transaksi
import java.text.NumberFormat
import java.util.Locale

class ListCheckerAdapter (): RecyclerView.Adapter<ListCheckerAdapter.ViewHolder>() {

    private val listChecker: MutableList<Checker> = mutableListOf()

    fun setData(item: MutableList<Checker>){
        listChecker.clear()
        listChecker.addAll(item)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: Checker)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvNameMenuChecker)
        private val tvStock = itemView.findViewById<TextView>(R.id.tvStockChecker)
        private val tvNote = itemView.findViewById<TextView>(R.id.tvNoteItemsChecker)
        private val bgCard = itemView.findViewById<CardView>(R.id.llBgItemListChecker)
        fun bind(items: Checker) {

            bgCard.setCardBackgroundColor(itemView.resources.getColor(R.color.green_light))

            when (items.status){
                2 -> {
                    bgCard.setCardBackgroundColor(itemView.resources.getColor(android.R.color.holo_red_light))
                }
                1 -> {
                    bgCard.setCardBackgroundColor(itemView.resources.getColor(android.R.color.holo_orange_light))
                }
            }

            if (items.requestChecker != ""){
                tvNote.visibility = View.VISIBLE
                tvNote.text = items.requestChecker
            }else{
                tvNote.visibility = View.GONE
            }

            tvName.textSize = 24f
            tvStock.textSize = 36f
            tvName.text = items.menuNameChecker
            tvStock.text = items.qtyChecker.toString()

            itemView.setOnClickListener { onItemClickCallback?.onItemClicked(items) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_checker, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listChecker.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listChecker[position])
    }
}