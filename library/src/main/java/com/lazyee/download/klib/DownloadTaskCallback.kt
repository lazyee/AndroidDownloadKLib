package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:07
 */
internal interface DownloadTaskCallback{
    fun provideDownloadTaskHistory(task: DownloadTask):DownloadTask?
    fun onDownloadStart(task: DownloadTask)
    fun onDownloading(task: DownloadTask)
    fun onDownloadComplete(task: DownloadTask)
    fun onDownloadFail(task: DownloadTask,errorMsg:String)
    fun onDownloadFileNotFound(task: DownloadTask)
}