package com.lazyee.download.klib

import java.net.HttpURLConnection

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2024/4/18 14:20
 */

data class DownloadRequest(val url:String, val method:String = GET) {

    fun updateHttpUrlConnection(connection:HttpURLConnection):HttpURLConnection{
        return connection
    }
    companion object Method{
        val GET = "GET"
        val POST = "POST"
        val HEAD = "HEAD"
    }

}