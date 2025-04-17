package com.example.drinkcourt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Bill
import com.example.drinkcourt.model.BillBottom
import java.text.NumberFormat
import java.util.Locale

class ListBottomBillAdapter(private val listBill: MutableList<BillBottom>): RecyclerView.Adapter<ListBottomBillAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(bill: BillBottom)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvMenuBottomBill)
        private val tvQty = itemView.findViewById<TextView>(R.id.tvQuantyBottomBill)
        private val tvAnt = itemView.findViewById<TextView>(R.id.tvAntBottomBill)
        private val tvCatatan = itemView.findViewById<TextView>(R.id.tvCatatanBottomBill)
        fun bind(bill: BillBottom) {
//            if(bill.request == ""){
//                tvCatatan.visibility = View.GONE
//            }else{
//                tvCatatan.text = bill.request
//                tvCatatan.visibility = View.VISIBLE
//            }

            tvName.text = bill.menuName
            tvQty.text = bill.qty.toString()
            tvAnt.text = bill.total.toString()

            itemView.setOnClickListener { onItemClickCallback?.onItemClicked(bill) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_cetak_bottom, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listBill.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listBill[position])
    }
}