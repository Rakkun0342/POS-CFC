package com.example.drinkcourt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Bill
import com.example.drinkcourt.model.Items
import java.text.NumberFormat
import java.util.Locale

class ListBillAdapter(private val listBill: MutableList<Bill>): RecyclerView.Adapter<ListBillAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(bill: Bill)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvNameMenu)
        private val tvStock = itemView.findViewById<TextView>(R.id.tvStock)
        private val llBgItems = itemView.findViewById<LinearLayout>(R.id.llBgItemList)
        fun bind(bill: Bill) {
            tvName.textSize = 30f
            tvStock.textSize = 20f
            tvName.text = bill.table
            tvStock.text = "Rp. ${NumberFormat.getNumberInstance(Locale.getDefault()).format(bill.netTotal).replace(",",".")}"

            llBgItems.background = itemView.resources.getDrawable(R.drawable.bg_list)

            if (bill.completed == 1) {
                llBgItems.background = itemView.resources.getDrawable(R.drawable.bg_list_completed)
            }

            itemView.setOnClickListener { onItemClickCallback?.onItemClicked(bill) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_items, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listBill.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listBill[position])
    }
}