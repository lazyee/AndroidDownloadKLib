package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:07
 */
interface DownloadCallback{
    fun onDownloadStart(downloadUrl:String)
    fun onDownloading(downloadProgressInfoList:List<DownloadProgressInfo>)
    fun onDownloadComplete(downloadUrl:String,savePath: String)
    fun onDownloadFail(downloadUrl:String,errorMsg:String)
    fun onAllDownloadEnd(successUrlList:MutableList<String>,failUrlList:MutableList<String>)
}

data class DownloadProgressInfo(val downloadUrl:String, var currentDownloadSize:Long,var totalSize:Long)