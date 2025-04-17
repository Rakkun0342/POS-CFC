package com.example.drinkcourt.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkcourt.R
import com.example.drinkcourt.model.Items
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ListItemAdapter(private val listItem: MutableList<Items>): RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: Items)
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvNameMenu)
        private val tvStock = itemView.findViewById<TextView>(R.id.tvStock)
        private val llBgItems = itemView.findViewById<ConstraintLayout>(R.id.llBgItemList)
        fun bind(items: Items){
            tvName.text = items.nama
            tvStock.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(items.harga)

            llBgItems.background = itemView.resources.getDrawable(R.drawable.bg_list)

            if (items.todayDisable == 1){
                llBgItems.background = itemView.resources.getDrawable(R.drawable.bg_list_disable)
            }else{
                itemView.setOnClickListener{onItemClickCallback?.onItemClicked(items)}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_items, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listItem[position])
    }
}