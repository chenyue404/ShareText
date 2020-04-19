package com.cy.shareText.list

import androidx.recyclerview.widget.DiffUtil

/**
 * Created by Eddie on 2020/4/19 0019.
 */
class ListAdapterDiffCallback(
    private val oldList: ArrayList<String>,
    private val newList: ArrayList<String>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}