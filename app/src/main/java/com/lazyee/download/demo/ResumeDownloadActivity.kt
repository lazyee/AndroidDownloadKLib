package com.lazyee.download.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 10:48
 */
class ResumeDownloadActivity :AppCompatActivity() {
    private val downloadInfoList = mutableListOf<String>()
    private val downloadInfoAdapter = DownloadInfoAdapter()
    private val rvDownloadInfo by lazy { findViewById<RecyclerView>(R.id.rvDownloadInfo) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rvDownloadInfo.adapter = downloadInfoAdapter
    }

    private inner class DownloadInfoAdapter : RecyclerView.Adapter<DownloadInfoViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadInfoViewHolder {
            return DownloadInfoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_download_info,null))
        }

        override fun getItemCount(): Int {
            return downloadInfoList.size
        }

        override fun onBindViewHolder(holder: DownloadInfoViewHolder, position: Int) {
            holder.bind(downloadInfoList[position])
        }

    }

    private inner class DownloadInfoViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        private val tvDownloadInfo by lazy { itemView.findViewById<TextView>(R.id.tvDownloadInfo) }
        fun bind(info:String){
            tvDownloadInfo.text = info
        }
    }
}
