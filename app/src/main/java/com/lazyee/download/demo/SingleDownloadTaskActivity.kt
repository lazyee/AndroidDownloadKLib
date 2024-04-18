package com.lazyee.download.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lazyee.download.klib.DownloadException
import com.lazyee.download.klib.DownloadTask
import com.lazyee.download.klib.DownloadTaskCallback
import java.io.File

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:单任务下载
 * Date: 2024/1/3 15:15
 */
class SingleDownloadTaskActivity :AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_download_task)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        findViewById<Button>(R.id.btnStartDownload).setOnClickListener {
            val downloadTask = DownloadTask("https://malltest.gacmotor.com/myfiles/common/file/2023/11/15/401f2609da09c9dbffa2d6d9afab3dc1/401f2609da09c9dbffa2d6d9afab3dc1.zip",
                filesDir.absolutePath + File.separator + "aaa.zip")
            downloadTask.justDownload(object :DownloadTaskCallback{
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
    }
}