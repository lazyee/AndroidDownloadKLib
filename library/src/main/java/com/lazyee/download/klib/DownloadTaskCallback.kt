package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:07
 */
interface DownloadTaskCallback{
    fun provideDownloadTaskHistory(task: InternalDownloadTask):InternalDownloadTask?
    fun onDownloadStart(task: InternalDownloadTask)
    fun onDownloading(task: InternalDownloadTask)
    fun onDownloadComplete(task: InternalDownloadTask)
    fun onDownloadFail(task: InternalDownloadTask,errorMsg:String)
}