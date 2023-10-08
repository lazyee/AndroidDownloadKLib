package com.lazyee.download.klib

import android.text.TextUtils
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 15:05
 */
private const val TAG = "[InternalDownloadTask]"
class InternalDownloadTask(private val downloadUrl:String,
                                    private val key:String,
                                    private val savePath:String):DownloadTask {
    var downloadSize = 0L
    var isDownloading = false
    var isReadyDownload = false//是否准备下载
    var isSupportSplitDownload = false
    var contentLength = 0L
    private var downloadFilePath:String

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

    fun setDownloadTaskCallback(callback: DownloadTaskCallback){
        mDownloadTaskCallback = callback
    }

    fun getDownloadFilePath():String = downloadFilePath

    override fun getDownloadUrl(): String {
        return downloadUrl
    }

    override fun getKey(): String {
        return key
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
                LogUtils.e(TAG,"检查文件属性失败,结束下载任务:${getDownloadUrl()}")
                mDownloadTaskCallback?.onDownloadFail(this,"检查文件属性失败,结束下载任务:${getDownloadUrl()}")
            }
            downloadFileProperty?:return
            this.contentLength = downloadFileProperty.contentLength
            this.isSupportSplitDownload = downloadFileProperty.isSupportSplitDownload

            val downloadTaskRecord = mDownloadTaskCallback?.onGetDownloadTaskHistory(this)
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
            val httpUrlConnection = URL(getDownloadUrl()).openConnection() as HttpURLConnection
            mCurrentDownloadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "GET"

            if (isSupportSplitDownload && alreadyDownloadSize > 0){
                createDownloadFile(getDownloadFilePath(),false)
                httpUrlConnection.setRequestProperty("Range", "bytes=$alreadyDownloadSize-")
                LogUtils.e(TAG,"开始分片下载，从位置:${alreadyDownloadSize}位置开始")
            }else{
                LogUtils.e(TAG,"开始全量下载")
                createDownloadFile(getDownloadFilePath(),true)
            }

            if (httpUrlConnection.responseCode == HttpURLConnection.HTTP_PARTIAL
                || httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK ) {

                val buffer = ByteArray(1024)
                var readCount = 0
                val randomAccessFile = RandomAccessFile(getDownloadFilePath(), "rwd")
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

    private fun checkDownloadUrlHead(task: InternalDownloadTask): DownloadFileProperty? {
        var downloadFIleProperty: DownloadFileProperty? = null
        try {
            val httpUrlConnection = URL(task.getDownloadUrl()).openConnection() as HttpURLConnection
            mCurrentHeadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "HEAD"
            httpUrlConnection.connectTimeout = 500
            httpUrlConnection.readTimeout = 500
            val responseCode = httpUrlConnection.responseCode
            LogUtils.e(TAG,"[HEAD]请求获取文件信息成功,链接:${task.getDownloadUrl()}")

            if(responseCode == HttpURLConnection.HTTP_OK){
                val contentLength:Long = httpUrlConnection.contentLength.toLong()
                LogUtils.e(TAG,"文件大小:[${contentLength}]")
                val acceptRanges = httpUrlConnection.getHeaderField("Accept-Ranges")
                val isSupportSplitDownload = acceptRanges != null && acceptRanges.lowercase() == "bytes"
                LogUtils.e(TAG,"链接:${task.getDownloadUrl()}" +  if(isSupportSplitDownload)",支持分片下载" else "不支持分片下载")
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
    private fun checkConsistencyFromDB(property: DownloadFileProperty,downloadTaskRecord:InternalDownloadTask): Boolean {
        return property.contentLength == downloadTaskRecord.contentLength
    }

    private fun checkConsistencyFromLocalFile(property: DownloadFileProperty,task: InternalDownloadTask): Boolean {
        val file = File(task.getDownloadFilePath())
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