package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:05
 */
interface DownloadTask {
    fun getDownloadUrl():String
    fun getKey():String
}