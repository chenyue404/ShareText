package com.cy.shareText

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ToastUtils


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
        val iv_copy: ImageView = itemView.findViewById(R.id.iv_copy)

        init {
            iv_copy.setOnClickListener {
                val text = tv_01.text
                //获取剪贴板管理器：
                val cm: ClipboardManager? =
                    it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", text)
                // 将ClipData内容放到系统剪贴板里。
                cm?.setPrimaryClip(mClipData)

                ToastUtils.showShort(R.string.copyed)
            }
        }
    }
}