package com.cy.shareText

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListAdapter(private val dataList: ArrayList<String>) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tv_01.text = dataList[position]
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv_01: TextView = itemView.findViewById(R.id.tv_01)
    }
}