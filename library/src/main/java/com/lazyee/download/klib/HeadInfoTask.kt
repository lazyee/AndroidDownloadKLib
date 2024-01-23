package com.lazyee.download.klib

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2024/1/16 09:52
 */
private const val TAG = "[HeadInfoTask]"
class HeadInfoTask(url:String):BaseTask(url) {

    private var mHeadHttpURLConnection:HttpURLConnection? = null
    fun check():DownloadFileProperty?{
        var downloadFileProperty: DownloadFileProperty? = null

        if(isCancelTask)return null
        LogUtils.e(TAG,"开始检查下载文件属性,链接:${downloadUrl}")
        val httpUrlConnection = URL(urlEncodeChinese(downloadUrl)).openConnection() as HttpURLConnection
        mHeadHttpURLConnection = httpUrlConnection
        httpUrlConnection.requestMethod = "HEAD"
        httpUrlConnection.connectTimeout = CONNECTION_TIMEOUT
        httpUrlConnection.readTimeout = READ_TIMEOUT
        httpUrlConnection.connect()

        when(httpUrlConnection.responseCode){
            HttpURLConnection.HTTP_OK->{
                LogUtils.e(TAG,"请求获取文件信息成功,链接:${downloadUrl}")
                val contentLength:Long = httpUrlConnection.contentLength.toLong()
                LogUtils.e(TAG,"文件大小:[${contentLength}]")
                val acceptRanges = httpUrlConnection.getHeaderField("Accept-Ranges")
                val isSupportSplitDownload = acceptRanges != null && acceptRanges.toLowerCase(Locale.ROOT) == "bytes"
                LogUtils.e(TAG,"链接:${downloadUrl}" +  if(isSupportSplitDownload)",支持分片下载" else "不支持分片下载")
                downloadFileProperty = DownloadFileProperty(contentLength,isSupportSplitDownload)
            }
            HttpURLConnection.HTTP_NOT_FOUND->{
                LogUtils.e(TAG,"文件不存在,链接:${downloadUrl}")
                httpUrlConnection.disconnect()
                throw DownloadFileNotFoundException(downloadUrl)
            }
        }

        httpUrlConnection.disconnect()
        return downloadFileProperty
    }

    override fun cancel() {
        super.cancel()
        mHeadHttpURLConnection?.disconnect()
        mHeadHttpURLConnection = null
    }


}