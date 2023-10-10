package com.lazyee.download.klib

import android.text.TextUtils
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:05
 */
private const val TAG = "[InternalDownloadTask]"
private const val MAX_RETRY_COUNT = 3//最大重试次数
class DownloadTask(val downloadUrl:String,
                   val key:String,
                   private val savePath:String) {
    var downloadSize = 0L
    var isDownloading = false
    var isReadyDownload = false//是否准备下载
    var isSupportSplitDownload = false
    var contentLength = 0L
    val downloadFilePath:String
    private val tempDownloadFilePath:String
    private val bufferSize = 32 * 1024//一次性读取32k数据
    private var retryCount = 0//重试次数

    private var mCurrentDownloadHttpURLConnection: HttpURLConnection? = null
    private var mCurrentHeadHttpURLConnection: HttpURLConnection? = null
    private var mDownloadTaskCallback:DownloadTaskCallback? = null

    init {
        var realSavePath:String = savePath
        if(!savePath.endsWith(File.separator)){
            realSavePath = savePath +  File.separator
        }
        downloadFilePath = realSavePath + key
        tempDownloadFilePath = realSavePath + "_" + key
    }

    internal fun setDownloadTaskCallback(callback: DownloadTaskCallback){
        mDownloadTaskCallback = callback
    }

    private fun createDownloadFile(path:String,deleteIfExist:Boolean){
        val saveFile = File(path)
        val parentDirPath = saveFile.parent
        if(parentDirPath != null && !TextUtils.isEmpty(parentDirPath)){
            val parentDir = File(parentDirPath)
            if(!parentDir.exists()){
                parentDir.mkdirs()
            }
        }

        if(deleteIfExist && saveFile.exists()){
            saveFile.delete()
            saveFile.createNewFile()
        }else if(!saveFile.exists()){
            saveFile.createNewFile()
        }
    }

    fun execute(){
        try{
            isReadyDownload = true
            mDownloadTaskCallback?.onDownloadStart(this)
            val httpUrlConnection = URL(downloadUrl).openConnection() as HttpURLConnection
            mCurrentDownloadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "GET"

            contentLength = httpUrlConnection.contentLength.toLong()
            val acceptRanges = httpUrlConnection.getHeaderField("Accept-Ranges")
            val isSupportSplitDownload = acceptRanges != null && acceptRanges.toLowerCase(Locale.ROOT) == "bytes"

            val downloadTaskRecord = mDownloadTaskCallback?.provideDownloadTaskHistory(this)
            var alreadyDownloadSize = downloadTaskRecord?.downloadSize?: 0L

            if(downloadTaskRecord != null && !checkConsistencyFromDB(contentLength,downloadTaskRecord)){
                LogUtils.e(TAG,"未在数据库中找到相关下载记录或者数据库记录中文件一致性校验不通过，准备重新下载")
                alreadyDownloadSize = 0L
            }

            if(checkConsistencyFromLocalFile(contentLength) && alreadyDownloadSize == 0L){
                LogUtils.e(TAG,"在本地文件检测到文件已经完整下载，跳过本次下载任务")
                mDownloadTaskCallback?.onDownloadComplete(this)
                httpUrlConnection.disconnect()
                return
            }

            if (isSupportSplitDownload && alreadyDownloadSize > 0){
                createDownloadFile(tempDownloadFilePath,false)
                httpUrlConnection.setRequestProperty("Range", "bytes=$alreadyDownloadSize-")
                LogUtils.e(TAG,"开始分片下载，从位置:${alreadyDownloadSize}位置开始")
            }else{
                LogUtils.e(TAG,"开始全量下载")
                createDownloadFile(tempDownloadFilePath,true)
            }

            if (httpUrlConnection.responseCode == HttpURLConnection.HTTP_PARTIAL
                || httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK ) {

                val buffer = ByteArray(bufferSize)
                var readCount = 0
                val randomAccessFile = RandomAccessFile(tempDownloadFilePath, "rwd")
                randomAccessFile.seek(alreadyDownloadSize)
                while (httpUrlConnection.inputStream.read(buffer, 0, buffer.count()).also { readCount = it } != -1) {
                    randomAccessFile.write(buffer, 0, readCount)
                    alreadyDownloadSize += readCount
                    downloadSize = alreadyDownloadSize
                    mDownloadTaskCallback?.onDownloading(this)
                }

                LogUtils.e(TAG,"文件下载完成")
                //将临时文件改名为正式文件
                File(tempDownloadFilePath).renameTo(File(downloadFilePath))
                mDownloadTaskCallback?.onDownloadComplete(this)
                httpUrlConnection.inputStream.close()
                httpUrlConnection.disconnect()
            } else {
                httpUrlConnection.disconnect()
                if(!retry()){
                    mDownloadTaskCallback?.onDownloadFail(this,"下载失败,状态码:${httpUrlConnection.responseCode}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if(!retry()){
                mDownloadTaskCallback?.onDownloadFail(this,"exception:${e.message}")
                retryCount = 0
            }
        }
    }

    private fun retry(): Boolean {
        if(retryCount < MAX_RETRY_COUNT){
            retryCount++
            LogUtils.e(TAG,"正在重试${retryCount}次下载任务[${downloadUrl}]")
            execute()
            return true
        }

        return false
    }

    /**
     * 检查文件一致性
     */
    private fun checkConsistencyFromDB(contentLength:Long ,downloadTaskRecord:DownloadTask): Boolean {
        return contentLength == downloadTaskRecord.contentLength
    }

    private fun checkConsistencyFromLocalFile(contentLength:Long): Boolean {
        val file = File(downloadFilePath)
        val fileLength = if(file.exists()) file.length() else 0L
        return contentLength == fileLength
    }


    /**
     * 取消下载
     */
    fun cancel(){
        mCurrentHeadHttpURLConnection?.disconnect()
        mCurrentHeadHttpURLConnection?.disconnect()
    }
}