package com.example.drinkcourt.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.conn.Connect
import com.example.drinkcourt.model.SubTransaksi
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class ListBottomAdapter (private val listItem: MutableList<SubTransaksi>, private val context: Context, private val cheker: Boolean): RecyclerView.Adapter<ListBottomAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null
    private var connect: Connect = Connect()
    var kondition = false

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(subTransaksi: SubTransaksi, position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMenu = itemView.findViewById<TextView>(R.id.tvMenuBottom)
        private val tvQty = itemView.findViewById<TextView>(R.id.tvQuantyBottom)
        private val btnRemove = itemView.findViewById<ImageView>(R.id.btnRemoveBottom)
        private val btnAdd = itemView.findViewById<ImageView>(R.id.btnAddBottom)
        private val btnClear = itemView.findViewById<ImageView>(R.id.btnClearBottom)
        private val btnSend = itemView.findViewById<ImageView>(R.id.btnSendBottom)
        private val catatan = itemView.findViewById<TextView>(R.id.tvCatatanBottom)
        private val llCatatan = itemView.findViewById<LinearLayout>(R.id.llCatatanListItemBottom)

        fun bind(subTransaksi: SubTransaksi, position: Int) {
            tvMenu.text = subTransaksi.namaMenu
            tvQty.text = subTransaksi.qty.toString()

            if (!cheker){
                btnRemove.visibility = View.GONE
                btnAdd.visibility = View.GONE
                btnSend.visibility = View.GONE
                btnClear.visibility = View.GONE
            }

            if (subTransaksi.pref != ""){
                catatan.text = subTransaksi.pref
                catatan.visibility = View.VISIBLE
                llCatatan.visibility = View.VISIBLE
            }else{
                catatan.visibility = View.GONE
                llCatatan.visibility = View.GONE
            }

            btnRemove.setOnClickListener{
                if (subTransaksi.qty!! > 1){
                    subTransaksi.qty = subTransaksi.qty!! - 1
                    notifyDataSetChanged()
                }
            }

            btnAdd.setOnClickListener {
                if (subTransaksi.qty!! < subTransaksi.maxQty!!){
                    subTransaksi.qty = subTransaksi.qty!! + 1
                    notifyDataSetChanged()
                }
            }

            btnClear.setOnClickListener {
                subTransaksi.qty = 1
                notifyDataSetChanged()
            }

            btnSend.setOnClickListener{onItemClickCallback?.onItemClicked(subTransaksi, position)}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_items_bottom, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listItem[position], position)
    }
}