package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:07
 */
interface DownloadCallback{
    fun onDownloadStart(task: DownloadTask)
    fun onDownloading(taskList:List<DownloadTask?>)
    fun onDownloadComplete(task: DownloadTask)
    fun onDownloadFail(exception: DownloadException)
    fun onAllDownloadEnd(successUrlList:MutableList<String>,failUrlList:MutableList<String>)
}
