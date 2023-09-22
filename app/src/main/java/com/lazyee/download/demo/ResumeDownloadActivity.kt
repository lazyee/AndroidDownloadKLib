package com.lazyee.download.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.lazyee.download.klib.DownloadCallback
import com.lazyee.download.klib.DownloadManager
import java.io.File

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 10:48
 */
class ResumeDownloadActivity :AppCompatActivity(),DownloadCallback {
    private val downloadInfoList = mutableListOf<String>()
    private val downloadInfoAdapter = DownloadInfoAdapter()
    private val rvDownloadInfo by lazy { findViewById<RecyclerView>(R.id.rvDownloadInfo) }
    private val btnStartDownload by lazy { findViewById<Button>(R.id.btnStartDownload) }
    private val mDownloadManager by lazy { DownloadManager(this@ResumeDownloadActivity) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_download)
        rvDownloadInfo.adapter = downloadInfoAdapter

        btnStartDownload.setOnClickListener {

            val url = "https://i0.hdslb.com/bfs/manga-static/7d2718962b3847afa013aea0d9978873cecfae70.jpg"
            val savePath = filesDir.absolutePath + File.separator + "cache"
            mDownloadManager.download(url,savePath)
            mDownloadManager.addDownloadCallback(this,this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager.removeDownloadCallback(this)
    }

    override fun onDownloadStart(downloadUrl: String) {
        runOnUiThread {
            downloadInfoList.add("开始下载:$downloadUrl")
            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,-10)
        }
    }

    override fun onDownloading(downloadUrl: String, currentDownloadSize: Long, totalSize: Long) {
        runOnUiThread {
            downloadInfoList.add("下载完成:$downloadUrl;已下载:$currentDownloadSize;文件大小:$totalSize")
            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,-10)
        }
    }

    override fun onDownloadComplete(downloadUrl: String, savePath: String) {
        runOnUiThread {
            downloadInfoList.add("下载完成:$downloadUrl;保存到:$savePath")
            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,-10)
        }
    }

    override fun onDownloadFail(errorMsg: String) {

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
