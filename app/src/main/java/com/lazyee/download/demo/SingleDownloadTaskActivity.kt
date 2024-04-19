package com.lazyee.download.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lazyee.download.klib.DownloadException
import com.lazyee.download.klib.DownloadTask
import com.lazyee.download.klib.DownloadTaskCallback
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import java.io.File
import java.lang.ref.WeakReference


/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:单任务下载
 * Date: 2024/1/3 15:15
 */
class SingleDownloadTaskActivity :AppCompatActivity(){
//    private val downloadUrl = "https://mallcdn.gacmotor.com/myfiles/common/img/2023/09/25/168aa2eabb3d8acb3753bb4819d3453f/168aa2eabb3d8acb3753bb4819d3453f.png"
    private val downloadUrl = "https://malltest.gacmotor.com/myfiles/common/file/2023/11/15/401f2609da09c9dbffa2d6d9afab3dc1/401f2609da09c9dbffa2d6d9afab3dc1.zip"
//    private val downloadFileName = "aaa.png"
    private val downloadFileName = "aaa.zip"
    private val downloadFilePath by lazy { filesDir.absolutePath + File.separator + downloadFileName }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_download_task)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val file = File(downloadFilePath)
            if(file.exists()){
                file.delete()
            }

            val tempFile = File(filesDir.absolutePath + File.separator + "_" + downloadFileName)
            if(tempFile.exists()){
                tempFile.delete()
            }
        }
        findViewById<Button>(R.id.btnStartDownload).setOnClickListener {
            val downloadTask = DownloadTask(downloadUrl, downloadFilePath)
            downloadTask.justDownload(object : DownloadTaskCallback {
                override fun onDownloadStart(task: DownloadTask) {
                    tvStatus.text = "开始下载"
                }

                override fun onDownloading(task: DownloadTask) {
                    tvStatus.text = "下载中:${task.downloadSize}/${task.contentLength}"
                }

                override fun onDownloadComplete(task: DownloadTask) {
                    tvStatus.text = "下载成功"
                }

                override fun onDownloadFail(exception: DownloadException) {
                    tvStatus.text = exception.message
                }
            })
        }

        findViewById<Button>(R.id.btnFileDownloaderStart).setOnClickListener {
            var time = 0L
            FileDownloader.getImpl()
                .create(downloadUrl)
                .setPath(downloadFilePath)
                .setListener(object :FileDownloadListener(){
                    override fun pending(p0: BaseDownloadTask?, p1: Int, p2: Int) {

                    }

                    override fun progress(p0: BaseDownloadTask?, p1: Int, p2: Int) {
                        tvStatus.text = "下载中:${p1}/${p2}"
                    }

                    override fun completed(p0: BaseDownloadTask?) {
                        Log.e("TAG","costTime:" + (System.currentTimeMillis() - time))
                    }

                    override fun paused(p0: BaseDownloadTask?, p1: Int, p2: Int) {
                    }

                    override fun error(p0: BaseDownloadTask?, p1: Throwable?) {
                    }

                    override fun warn(p0: BaseDownloadTask?) {
                    }

                })
                .start()
            time = System.currentTimeMillis()
        }
    }
}