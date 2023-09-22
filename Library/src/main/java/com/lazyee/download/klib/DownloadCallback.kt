package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:07
 */
interface DownloadCallback{
    fun onDownloadStart(downloadUrl:String)
    fun onDownloading(downloadUrl:String, currentDownloadSize:Long,totalSize:Long)
    fun onDownloadComplete(downloadUrl:String,savePath: String)
    fun onDownloadFail(errorMsg:String)
}