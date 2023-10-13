package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/10/13 23:38
 */
open class DownloadException(val task: DownloadTask, msg:String) : Exception(msg){
}

class DownloadFileNotFoundException(task: DownloadTask,msg: String) : DownloadException(task,msg){
}