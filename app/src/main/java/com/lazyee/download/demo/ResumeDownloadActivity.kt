package com.lazyee.download.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lazyee.download.klib.DownloadCallback
import com.lazyee.download.klib.DownloadManager
import com.lazyee.download.klib.DownloadProgressInfo
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
    private var downloadSuccessSize = 0
    private var downloadFailSize = 0
    private var isTouch = false
    private var lastTouchTime = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_download)
        rvDownloadInfo.adapter = downloadInfoAdapter
        mDownloadManager.addDownloadCallback(this,this)
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/21922bc5aa31be92da886d315da73a40/21922bc5aa31be92da886d315da73a40.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/dd7e1636bf314b42027b0e25d215e872/dd7e1636bf314b42027b0e25d215e872.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/be8cc0ca0b81d4903be968a35fb7d07f/be8cc0ca0b81d4903be968a35fb7d07f.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/11ca7357050998d44b9075de701bc5cb/11ca7357050998d44b9075de701bc5cb.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/b57f5b425d854bdd28eb1789965fc62d/b57f5b425d854bdd28eb1789965fc62d.png")
//
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/logo-ac25a7fb.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/nec-b982b606.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/model-bg-a643653d.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/ec-5281d1a7.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/uhd2/assets/home-bg-9f5eb5f3.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/04/c4cef88aaa57c9d2232c2c45aa7291f1/c4cef88aaa57c9d2232c2c45aa7291f1.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/18/b29ab7b56dae5a6b9cc5e1114ad6dcef/b29ab7b56dae5a6b9cc5e1114ad6dcef.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/09/a25c34aec71079ae0a867b92e95be584/a25c34aec71079ae0a867b92e95be584.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/7c9a8ab653328c44f01b09ecce256a5d/7c9a8ab653328c44f01b09ecce256a5d.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/b29ab7b56dae5a6b9cc5e1114ad6dcef/b29ab7b56dae5a6b9cc5e1114ad6dcef.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/b96f3563d472f83a39cc7b5f1254908b/b96f3563d472f83a39cc7b5f1254908b.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/f633718db2ecaa12b962e53acef07f2d/f633718db2ecaa12b962e53acef07f2d.png")
//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/img/2023/05/17/655061a9be39632526a2c33977b5864f/655061a9be39632526a2c33977b5864f.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/04/27/51ec22780d6d48525750ebd049a51f1f/51ec22780d6d48525750ebd049a51f1f.jpg")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/04/27/d14e8b8c266d409bfd9bf52e9b4e74a1/d14e8b8c266d409bfd9bf52e9b4e74a1.jpg")




//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/home-bg-9f5eb5f3.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/nec-b982b606.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/ec-5281d1a7.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/home-bg-19ef180c.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/logo-ac25a7fb.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/model-bg-a643653d.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/07/19/c4cef88aaa57c9d2232c2c45aa7291f1/c4cef88aaa57c9d2232c2c45aa7291f1.png")
//
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/14/59b6d08ccab9c6dd3ad98fc806ef84f6/59b6d08ccab9c6dd3ad98fc806ef84f6.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/14/c4cef88aaa57c9d2232c2c45aa7291f1/c4cef88aaa57c9d2232c2c45aa7291f1.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/14/9a9a3400c4af0290ca62f6cd8baf2110/9a9a3400c4af0290ca62f6cd8baf2110.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/07/19/a25c34aec71079ae0a867b92e95be584/a25c34aec71079ae0a867b92e95be584.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/21abd8e781fae286f4992bfc7cbd5fd5/21abd8e781fae286f4992bfc7cbd5fd5.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/654ad09b26a271b56cdf288c7612a69f/654ad09b26a271b56cdf288c7612a69f.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/3295e04b86245aeeec6e0cdada9bdba8/3295e04b86245aeeec6e0cdada9bdba8.png")
        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/168aa2eabb3d8acb3753bb4819d3453f/168aa2eabb3d8acb3753bb4819d3453f.png")

//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/other-bg-0dc1c997.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/7b8fc79b1c91664614f116a889daaf71/7b8fc79b1c91664614f116a889daaf71.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/8966bb1cc554d11c3867569c1b487b47/8966bb1cc554d11c3867569c1b487b47.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/14/9a9a3400c4af0290ca62f6cd8baf2110/9a9a3400c4af0290ca62f6cd8baf2110.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/14/c4cef88aaa57c9d2232c2c45aa7291f1/c4cef88aaa57c9d2232c2c45aa7291f1.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/14/59b6d08ccab9c6dd3ad98fc806ef84f6/59b6d08ccab9c6dd3ad98fc806ef84f6.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/choose-bg-9fc70ad8.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/choose-bg-73ed50b1.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/25/10a52668b6b2872f5853cc1f282d9be5/10a52668b6b2872f5853cc1f282d9be5.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/25/a07ced77fe2f04fbba7f3b09d84632ca/a07ced77fe2f04fbba7f3b09d84632ca.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/22/8858321ed380551962a92f840da756bd/8858321ed380551962a92f840da756bd.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/07/8640ccfeee57eb66131c7cd66343f5ed/8640ccfeee57eb66131c7cd66343f5ed.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/08/8c07d46281941268354e2da4e6badfcd/8c07d46281941268354e2da4e6badfcd.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/08/7ab0befd3fa8cc7c95ceae1a2a765950/7ab0befd3fa8cc7c95ceae1a2a765950.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/29/81d41b1f671330972d73c0260658583b/81d41b1f671330972d73c0260658583b.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/08/1fdc8c1f3b91cb6f37d4db425a21a4a6/1fdc8c1f3b91cb6f37d4db425a21a4a6.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/06/d293410f19bb472fe4b93d368c89d88f/d293410f19bb472fe4b93d368c89d88f.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/06/738d8419fbf30d7dae799daa312c8b08/738d8419fbf30d7dae799daa312c8b08.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/07/19/c4cef88aaa57c9d2232c2c45aa7291f1/c4cef88aaa57c9d2232c2c45aa7291f1.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/23/032504b54a77b8d792c8280ad2603599/032504b54a77b8d792c8280ad2603599.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/dabcc7653d14f18d6bf2723904bb3b4d/dabcc7653d14f18d6bf2723904bb3b4d.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/810bbe15df1319f9b337e609d03f58f0/810bbe15df1319f9b337e609d03f58f0.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/1ee09c6577107b96a71501e194c20cc8/1ee09c6577107b96a71501e194c20cc8.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/6b23f7079df5a946ed1ef56a78ac7c27/6b23f7079df5a946ed1ef56a78ac7c27.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/7983a93af113041129c051da183023a8/7983a93af113041129c051da183023a8.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/924001b01b9bb8136e34678ef568cad3/924001b01b9bb8136e34678ef568cad3.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/24/7c8a4130028c918f2fd41d35afaf67da/7c8a4130028c918f2fd41d35afaf67da.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/24/8c1772625e33877d4e17de126db069e3/8c1772625e33877d4e17de126db069e3.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/24/bf53cbeaeef79cf76fd470712cbbc9d6/bf53cbeaeef79cf76fd470712cbbc9d6.png")
//        mTestDownloadUrlList.add("https://mall.gacmotor.com/uhd2/assets/other-bg-0dc1c997.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2021/12/30/54ace39a0e3ea1ef43f70cbc0977dc09/54ace39a0e3ea1ef43f70cbc0977dc09.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/e6e9271e4e5df46c10f696b26aa8031a/e6e9271e4e5df46c10f696b26aa8031a.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/e5a5feec953e0c21cd5c860aa7e6ac78/e5a5feec953e0c21cd5c860aa7e6ac78.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/80e909a03a0f4c5a7a576787cb84eedc/80e909a03a0f4c5a7a576787cb84eedc.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/c7b8df9deaea20dbb0c5b0e7ee70f045/c7b8df9deaea20dbb0c5b0e7ee70f045.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/0862b5eb90df6f7b14bf85383466427b/0862b5eb90df6f7b14bf85383466427b.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/618f7362323544b662deaa16fd32ad42/618f7362323544b662deaa16fd32ad42.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/48ea7d611a5b14e225421218a828cf57/48ea7d611a5b14e225421218a828cf57.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/9b744f1a95e13c00bdafe1868be54d57/9b744f1a95e13c00bdafe1868be54d57.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/05/6a93cb08524337c32c1b7a070dd9ed5d/6a93cb08524337c32c1b7a070dd9ed5d.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/24/7c8a4130028c918f2fd41d35afaf67da/7c8a4130028c918f2fd41d35afaf67da.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/24/8c1772625e33877d4e17de126db069e3/8c1772625e33877d4e17de126db069e3.png")
//        mTestDownloadUrlList.add("https://mallcdn.gacmotor.com/myfiles/common/img/2023/08/24/bf53cbeaeef79cf76fd470712cbbc9d6/bf53cbeaeef79cf76fd470712cbbc9d6.png")


//        mTestDownloadUrlList.add("https://malltest.gacmotor.com/myfiles/common/video/2023/02/14/5f4ddb98b67b5ad8be631b9196b2db32/5f4ddb98b67b5ad8be631b9196b2db32.mp4")
        addNewCallbackInfoToList("downloadInfo","下载成功:${downloadSuccessSize};下载失败:${downloadFailSize}")
        btnStartDownload.setOnClickListener {
            val savePath = filesDir.absolutePath + File.separator + "cache"
            mDownloadManager.download(mTestDownloadUrlList,savePath)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager.removeDownloadCallback(this)
    }

    override fun onAllDownloadEnd(successUrlList: MutableList<String>, failUrlList: MutableList<String>) {
        runOnUiThread {
            addNewCallbackInfoToList("downloadInfo","下载成功:${successUrlList.size};下载失败:${failUrlList.size}")
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onDownloadComplete(downloadUrl: String, savePath: String) {
        runOnUiThread {
            downloadSuccessSize++
            addNewCallbackInfoToList("downloadInfo","下载成功${downloadSuccessSize};下载失败${downloadFailSize}")
            val target = downloadInfoList.find { it.key == downloadUrl }
            downloadInfoList.remove(target)
            downloadInfoAdapter.notifyDataSetChanged()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun addNewCallbackInfoToList(key: String,info: String ){
        val target = downloadInfoList.find { it.key == key && it.key.isNotEmpty() }
        if(target == null){
            downloadInfoList.add(CallbackInfo(key,info))
        }else{
            target.info = info
        }
        downloadInfoAdapter.notifyDataSetChanged()
        scrollToBottom()
    }

    private fun scrollToBottom(){
        if(isTouch || System.currentTimeMillis() - lastTouchTime < 1_000)return
        val position = if(downloadInfoAdapter.itemCount - 1 > 0) downloadInfoAdapter.itemCount - 1 else 0
        (rvDownloadInfo.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,0)

    }

    override fun onDownloadFail(downloadUrl: String,errorMsg: String) {
        runOnUiThread {
            downloadFailSize++
            addNewCallbackInfoToList("downloadInfo","下载成功${downloadSuccessSize};下载失败${downloadFailSize}")
            val target = downloadInfoList.find { it.key == downloadUrl }
            downloadInfoList.remove(target)
            downloadInfoAdapter.notifyDataSetChanged()
        }
    }

    override fun onDownloadStart(downloadUrl: String) {
        runOnUiThread { addNewCallbackInfoToList(downloadUrl,"资源[${getFileNameFromUrl(downloadUrl)}]开始下载")}
    }

    override fun onDownloading(downloadProgressInfoList: List<DownloadProgressInfo>) {
        runOnUiThread {
            downloadProgressInfoList.forEach {info ->
                addNewCallbackInfoToList(info.downloadUrl,"资源[${getFileNameFromUrl(info.downloadUrl)}]正在下载[${(info.currentDownloadSize * 100f / info.totalSize).toInt()}%]")
            }
        }
    }

    private fun getFileNameFromUrl(url:String): String {
        return url.split("/").last()
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
