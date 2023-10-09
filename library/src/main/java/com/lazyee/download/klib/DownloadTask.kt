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
class DownloadTask(val downloadUrl:String,
                   val key:String,
                   private val savePath:String) {
    var downloadSize = 0L
    var isDownloading = false
    var isReadyDownload = false//是否准备下载
    var isSupportSplitDownload = false
    var contentLength = 0L
    val downloadFilePath:String
    private val bufferSize = 32 * 1024//一次性读取32k数据

    private var mCurrentDownloadHttpURLConnection: HttpURLConnection? = null
    private var mCurrentHeadHttpURLConnection: HttpURLConnection? = null
    private var mDownloadTaskCallback:DownloadTaskCallback? = null

    init {
        if(savePath.endsWith(File.separator)){
            downloadFilePath = savePath + key
        }else{
            downloadFilePath = savePath + File.separator + key
        }
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
            val downloadFileProperty = checkDownloadUrlHead(this)
            if(downloadFileProperty == null){
                LogUtils.e(TAG,"检查文件属性失败,结束下载任务:${downloadUrl}")
                mDownloadTaskCallback?.onDownloadFail(this,"检查文件属性失败,结束下载任务:${downloadUrl}")
            }
            downloadFileProperty?:return
            this.contentLength = downloadFileProperty.contentLength
            this.isSupportSplitDownload = downloadFileProperty.isSupportSplitDownload

            val downloadTaskRecord = mDownloadTaskCallback?.provideDownloadTaskHistory(this)
            var alreadyDownloadSize = downloadTaskRecord?.downloadSize?: 0L

            if(downloadTaskRecord != null && !checkConsistencyFromDB(downloadFileProperty,downloadTaskRecord)){
                LogUtils.e(TAG,"在数据库记录中文件一致性校验不通过，准备重新下载")
                alreadyDownloadSize = 0L
            }

            if(checkConsistencyFromLocalFile(downloadFileProperty,this) && alreadyDownloadSize == 0L){
                LogUtils.e(TAG,"在本地文件检测到文件已经完整下载，跳过本次下载任务")
                mDownloadTaskCallback?.onDownloadComplete(this)
                return
            }

            mDownloadTaskCallback?.onDownloadStart(this)
            val httpUrlConnection = URL(downloadUrl).openConnection() as HttpURLConnection
            mCurrentDownloadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "GET"

            if (isSupportSplitDownload && alreadyDownloadSize > 0){
                createDownloadFile(downloadFilePath,false)
                httpUrlConnection.setRequestProperty("Range", "bytes=$alreadyDownloadSize-")
                LogUtils.e(TAG,"开始分片下载，从位置:${alreadyDownloadSize}位置开始")
            }else{
                LogUtils.e(TAG,"开始全量下载")
                createDownloadFile(downloadFilePath,true)
            }

            if (httpUrlConnection.responseCode == HttpURLConnection.HTTP_PARTIAL
                || httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK ) {

                val buffer = ByteArray(bufferSize)
                var readCount = 0
                val randomAccessFile = RandomAccessFile(downloadFilePath, "rwd")
                randomAccessFile.seek(alreadyDownloadSize)
                while (httpUrlConnection.inputStream.read(buffer, 0, buffer.count()).also { readCount = it } != -1) {
                    randomAccessFile.write(buffer, 0, readCount)
                    alreadyDownloadSize += readCount
                    downloadSize = alreadyDownloadSize
                    mDownloadTaskCallback?.onDownloading(this)
                }

                LogUtils.e(TAG,"文件下载完成")
                mDownloadTaskCallback?.onDownloadComplete(this)
                httpUrlConnection.inputStream.close()
            } else {
                mDownloadTaskCallback?.onDownloadFail(this,"download fail,stateCode:${httpUrlConnection.responseCode}")
            }
            httpUrlConnection.disconnect()
        } catch (e: Exception) {
            mDownloadTaskCallback?.onDownloadFail(this,"exception:${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkDownloadUrlHead(task: DownloadTask): DownloadFileProperty? {
        var downloadFIleProperty: DownloadFileProperty? = null
        try {
            val httpUrlConnection = URL(task.downloadUrl).openConnection() as HttpURLConnection
            mCurrentHeadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "HEAD"
            httpUrlConnection.connectTimeout = 500
            httpUrlConnection.readTimeout = 500
            val responseCode = httpUrlConnection.responseCode
            LogUtils.e(TAG,"[HEAD]请求获取文件信息成功,链接:${task.downloadUrl}")

            if(responseCode == HttpURLConnection.HTTP_OK){
                val contentLength:Long = httpUrlConnection.contentLength.toLong()
                LogUtils.e(TAG,"文件大小:[${contentLength}]")
                val acceptRanges = httpUrlConnection.getHeaderField("Accept-Ranges")
                val isSupportSplitDownload = acceptRanges != null && acceptRanges.toLowerCase(Locale.ROOT) == "bytes"
                LogUtils.e(TAG,"链接:${task.downloadUrl}" +  if(isSupportSplitDownload)",支持分片下载" else "不支持分片下载")
                downloadFIleProperty = DownloadFileProperty(contentLength,isSupportSplitDownload)
            }
            httpUrlConnection.disconnect()
        }catch (e:Exception){
            e.printStackTrace()
        }

        return downloadFIleProperty
    }

    /**
     * 检查文件一致性
     */
    private fun checkConsistencyFromDB(property: DownloadFileProperty,downloadTaskRecord:DownloadTask): Boolean {
        return property.contentLength == downloadTaskRecord.contentLength
    }

    private fun checkConsistencyFromLocalFile(property: DownloadFileProperty,task: DownloadTask): Boolean {
        val file = File(task.downloadFilePath)
        val fileLength = if(file.exists()) file.length() else 0L
        return property.contentLength == fileLength


    }


    /**
     * 取消下载
     */
    fun cancel(){
        mCurrentHeadHttpURLConnection?.disconnect()
        mCurrentHeadHttpURLConnection?.disconnect()
    }
}