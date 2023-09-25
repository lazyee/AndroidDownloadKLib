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
    private val downloadInfoList = mutableListOf<CallbackInfo>()
    private val downloadInfoAdapter = DownloadInfoAdapter()
    private val rvDownloadInfo by lazy { findViewById<RecyclerView>(R.id.rvDownloadInfo) }
    private val btnStartDownload by lazy { findViewById<Button>(R.id.btnStartDownload) }
    private val mDownloadManager by lazy { DownloadManager.with(this).debug(true) }
    private val mTestDownloadUrlList = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_download)
        rvDownloadInfo.adapter = downloadInfoAdapter
        mDownloadManager.addDownloadCallback(this,this)
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/21922bc5aa31be92da886d315da73a40/21922bc5aa31be92da886d315da73a40.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/dd7e1636bf314b42027b0e25d215e872/dd7e1636bf314b42027b0e25d215e872.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/be8cc0ca0b81d4903be968a35fb7d07f/be8cc0ca0b81d4903be968a35fb7d07f.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/11ca7357050998d44b9075de701bc5cb/11ca7357050998d44b9075de701bc5cb.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/b57f5b425d854bdd28eb1789965fc62d/b57f5b425d854bdd28eb1789965fc62d.png")

        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/logo-ac25a7fb.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/nec-b982b606.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/model-bg-a643653d.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/ec-5281d1a7.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/home-bg-9f5eb5f3.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/04/c4cef88aaa57c9d2232c2c45aa7291f1/c4cef88aaa57c9d2232c2c45aa7291f1.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/a25c34aec71079ae0a867b92e95be584/a25c34aec71079ae0a867b92e95be584.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/7c9a8ab653328c44f01b09ecce256a5d/7c9a8ab653328c44f01b09ecce256a5d.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/b29ab7b56dae5a6b9cc5e1114ad6dcef/b29ab7b56dae5a6b9cc5e1114ad6dcef.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/b96f3563d472f83a39cc7b5f1254908b/b96f3563d472f83a39cc7b5f1254908b.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/f633718db2ecaa12b962e53acef07f2d/f633718db2ecaa12b962e53acef07f2d.png")
        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/655061a9be39632526a2c33977b5864f/655061a9be39632526a2c33977b5864f.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/04/27/51ec22780d6d48525750ebd049a51f1f/51ec22780d6d48525750ebd049a51f1f.jpg")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/04/27/d14e8b8c266d409bfd9bf52e9b4e74a1/d14e8b8c266d409bfd9bf52e9b4e74a1.jpg")


        btnStartDownload.setOnClickListener {
            val savePath = filesDir.absolutePath + File.separator + "cache"
            mDownloadManager.download(mTestDownloadUrlList,savePath)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager.removeDownloadCallback(this)
    }

    override fun onDownloadStart(downloadUrl: String) {
        runOnUiThread {
            downloadInfoList.add(CallbackInfo(info = "开始下载:$downloadUrl") )
            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,50)
        }
    }

    override fun onDownloading(downloadUrl: String, currentDownloadSize: Long, totalSize: Long) {
        runOnUiThread {
            val info = "下载完成:$downloadUrl;已下载:$currentDownloadSize;文件大小:$totalSize"
            var callbackInfo = downloadInfoList.find { it.key == downloadUrl }
            if(callbackInfo == null){
                callbackInfo = CallbackInfo( key = downloadUrl,info = info)
                downloadInfoList.add(callbackInfo)
            }else{
                callbackInfo.info = info
            }

            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,50)
        }
    }

    override fun onDownloadComplete(downloadUrl: String, savePath: String) {
        runOnUiThread {
            downloadInfoList.add(CallbackInfo(info ="已下载:$downloadUrl;保存到:$savePath"))
            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,50)
        }
    }

    override fun onDownloadFail(errorMsg: String) {
        runOnUiThread {
            downloadInfoList.add(CallbackInfo(info = errorMsg))
            downloadInfoAdapter.notifyDataSetChanged()
            rvDownloadInfo.scrollBy(0,50)
        }
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
        fun bind(info:CallbackInfo){
            tvDownloadInfo.text = info.info
        }
    }

    private data class CallbackInfo(val key:String? = null,var info:String)
}
