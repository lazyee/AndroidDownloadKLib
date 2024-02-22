package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:07
 */

interface DownloadTaskCallback{
    fun onDownloadStart(task: DownloadTask)
    fun onDownloading(task: DownloadTask)
    fun onDownloadComplete(task:DownloadTask)
    fun onDownloadFail(exception: DownloadException)
}
internal interface InternalDownloadTaskCallback :DownloadTaskCallback{
    fun provideDownloadTaskHistory(task: DownloadTask):DownloadTask?
}