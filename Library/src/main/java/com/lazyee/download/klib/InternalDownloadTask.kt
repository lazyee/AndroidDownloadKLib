package com.lazyee.download.klib

import java.io.File

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:05
 */
class InternalDownloadTask(private val downloadUrl:String,
                                    private val key:String,
                                    private val savePath:String):DownloadTask {
    var downloadSize = 0L
    var isDownloading = false
    var isSupportSplitDownload = false
    var contentLength = 0L
    private var downloadFilePath:String
    init {
        if(savePath.endsWith(File.separator)){
            downloadFilePath = savePath + key
        }else{
            downloadFilePath = savePath + File.separator + key
        }
    }

    fun getDownloadFilePath():String = downloadFilePath

    override fun getDownloadUrl(): String {
        return downloadUrl
    }

    override fun getKey(): String {
        return key
    }


}