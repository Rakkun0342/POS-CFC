package com.example.drinkcourt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Produc
import java.text.NumberFormat
import java.util.Locale

class OrderProductAdapter(private val listProduc: MutableList<Produc>):RecyclerView.Adapter<OrderProductAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null
    private var onItemClickCallbackQty: OnItemClickCallbackQty? = null
    private var onItemClickButtonCallback: OnItemClickButtonCallback? = null
    private var onItemClickRemoveCallback: OnItemClickRemoveCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }
    fun setOnItemClickCallbackQty(onItemClickCallbackQty: OnItemClickCallbackQty) {
        this.onItemClickCallbackQty = onItemClickCallbackQty
    }

    fun setOnItemClickButtonCallback(onItemClickButtonCallback: OnItemClickButtonCallback) {
        this.onItemClickButtonCallback = onItemClickButtonCallback
    }

    fun setOnItemClickRemoveCallback(onItemClickRemoveCallback: OnItemClickRemoveCallback) {
        this.onItemClickRemoveCallback = onItemClickRemoveCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: Produc)
    }

    interface OnItemClickCallbackQty {
        fun onItemClicked(items: Produc)
    }

    interface OnItemClickButtonCallback {
        fun onAddButtonClicked(item: Produc)
    }

    interface OnItemClickRemoveCallback {
        fun onRemoveButtonlicked(items: Produc, position: Int)
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private var tvName = itemView.findViewById<TextView>(R.id.tvNameProduc)
        private var tvBiaya = itemView.findViewById<TextView>(R.id.tvBiayaProduc)
        private var tvQuanty = itemView.findViewById<TextView>(R.id.tvQuantyProduc)
        private var btnAdd = itemView.findViewById<ImageView>(R.id.btnAddProduc)
        private var btnRemove = itemView.findViewById<ImageView>(R.id.btnRemoveProduc)
        private var tvCatatan = itemView.findViewById<TextView>(R.id.tvCatatanProduc)
        private var llCatatan = itemView.findViewById<LinearLayout>(R.id.llCatatanOrder)
        fun bind(produc: Produc, position: Int){
            tvName.text = produc.nama
            tvQuanty.text = produc.quanty.toString()
            tvBiaya.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(produc.total)
            tvCatatan.visibility = View.GONE
            llCatatan.visibility = View.GONE
            if (produc.catatan != ""){
                tvCatatan.visibility = View.VISIBLE
                llCatatan.visibility = View.VISIBLE
                tvCatatan.text = produc.catatan
            }

            tvQuanty.setOnClickListener{onItemClickCallbackQty?.onItemClicked(produc)}
            btnAdd.setOnClickListener{onItemClickButtonCallback?.onAddButtonClicked(produc)}
            btnRemove.setOnClickListener{onItemClickRemoveCallback?.onRemoveButtonlicked(produc, position)}
            tvName.setOnClickListener{onItemClickCallback?.onItemClicked(produc)}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_menu_pesanan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listProduc.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listProduc[position], position)
    }
}