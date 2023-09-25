package com.lazyee.download.klib

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.core.os.BuildCompat
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 13:59
 */
private const val TAG = "[DownloadManager]"
class DownloadManager private constructor(mContext: Context){
    private val mDownloadTaskList = mutableListOf<InternalDownloadTask>()
    private var mDownloadCallbackHashMap = hashMapOf<Any,DownloadCallback>()
    private var mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var mCurrentDownloadHttpURLConnection:HttpURLConnection? = null
    private var mCurrentHeadHttpURLConnection:HttpURLConnection? = null
    private var mDownloadDBHelper:DownloadDBHelper

    init {
        mDownloadDBHelper = DownloadDBHelper(mContext)
    }

    companion object{
        fun with(context: Context): DownloadManager {
            val downloadManager = DownloadManager(context)
            downloadManager.debug(false)
            return downloadManager
        }
    }


    fun debug(isDebug:Boolean):DownloadManager{
        LogUtils.init(isDebug)
        return this
    }

//    fun initInstance(context:Context){
//        val key = "1695373632879"
//        val url = System.currentTimeMillis().toString()
//        DownloadDBHelper(context).updateDownloadTask(InternalDownloadTask(url, md5(key),"44444"))
//
//    }
    fun download(downloadUrl:String, savePath:String){
        mDownloadTaskList.add(InternalDownloadTask(downloadUrl, getKeyByUrl(downloadUrl), savePath))
        realDownload()
    }

    fun download(downloadUrlList: List<String>,savePath: String){
        val realDownloadUrlList = mutableListOf<String>()
        downloadUrlList.forEach {
            if(!realDownloadUrlList.contains(it)){
                realDownloadUrlList.add(it)
            }
        }
        realDownloadUrlList.forEach { downloadUrl->
            if(mDownloadTaskList.find { it.getDownloadUrl() == downloadUrl } == null){
                mDownloadTaskList.add(InternalDownloadTask(downloadUrl,getKeyByUrl(downloadUrl),savePath))
            }
        }
        realDownload()
    }

    private fun realDownload(){
        val task = mDownloadTaskList.firstOrNull()
        task?:return
        if(task.isDownloading) return
        mExecutorService.execute {
            while (mDownloadTaskList.isNotEmpty()){
                executeDownloadTask(mDownloadTaskList.first())
            }
        }
    }

    private fun executeDownloadTask(task:InternalDownloadTask){
        try{
            val downloadFileProperty = checkDownloadUrlHead(task)
            if(downloadFileProperty == null){
                LogUtils.e(TAG,"检查文件属性失败,结束下载任务:${task.getDownloadUrl()}")
                callbackDownloadFail(task,"检查文件属性失败,结束下载任务:${task.getDownloadUrl()}")
            }
            downloadFileProperty?:return
            task.contentLength = downloadFileProperty.contentLength
            task.isSupportSplitDownload = downloadFileProperty.isSupportSplitDownload



            val downloadTaskRecord = mDownloadDBHelper.getDownloadTaskByKey(task.getKey()).firstOrNull()
            var alreadyDownloadSize = downloadTaskRecord?.downloadSize?: 0L

            if(downloadTaskRecord != null && !checkConsistencyFromDB(downloadFileProperty,downloadTaskRecord)){
                LogUtils.e(TAG,"在数据库记录中文件一致性校验不通过，准备重新下载")
                alreadyDownloadSize = 0L
            }

            if(checkConsistencyFromLocalFile(downloadFileProperty,task) && alreadyDownloadSize == 0L){
                LogUtils.e(TAG,"在本地文件检测到文件已经完整下载，跳过本次下载任务")
                callbackDownloadComplete(task)
                return
            }

            callbackDownloadStart(task)
            val httpUrlConnection = URL(task.getDownloadUrl()).openConnection() as HttpURLConnection
            mCurrentDownloadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "GET"

            if (task.isSupportSplitDownload && alreadyDownloadSize > 0){
                createDownloadFile(task.getDownloadFilePath(),false)
                httpUrlConnection.setRequestProperty("Range", "bytes=$alreadyDownloadSize-")
                LogUtils.e(TAG,"开始分片下载，从位置:${alreadyDownloadSize}位置开始")
            }else{
                LogUtils.e(TAG,"开始全量下载")
                createDownloadFile(task.getDownloadFilePath(),true)
            }

            if (httpUrlConnection.responseCode == HttpURLConnection.HTTP_PARTIAL
                || httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK ) {

                val buffer = ByteArray(1024)
                var readCount = 0
                val randomAccessFile = RandomAccessFile(task.getDownloadFilePath(), "rwd")
                randomAccessFile.seek(alreadyDownloadSize)
                while (httpUrlConnection.inputStream.read(buffer, 0, buffer.count()).also { readCount = it } != -1) {
                    randomAccessFile.write(buffer, 0, readCount)
                    alreadyDownloadSize += readCount
                    task.downloadSize = alreadyDownloadSize
                    callbackDownloading(task)
                }

                LogUtils.e(TAG,"文件下载完成")
                callbackDownloadComplete(task)
                httpUrlConnection.inputStream.close()
            } else {
                callbackDownloadFail(task,"download fail,stateCode:${httpUrlConnection.responseCode}")
            }
            httpUrlConnection.disconnect()
        } catch (e: Exception) {
            callbackDownloadFail(task,"exception:${e.message}")
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

    private fun md5(content:String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(content.toByteArray())
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun getKeyByUrl(url:String): String {
        return md5(url)
    }

    fun addDownloadCallback(key:Any,callback: DownloadCallback){
        if(!mDownloadCallbackHashMap.containsKey(key)){
            mDownloadCallbackHashMap[key] = callback
        }
    }

    fun removeDownloadCallback(key: Any){
        mDownloadCallbackHashMap.remove(key)
    }

    fun clearDownloadCallback(){
        mDownloadCallbackHashMap.clear()
    }

    private fun callbackDownloadStart(task: InternalDownloadTask){
        mDownloadDBHelper.updateDownloadTask(task)
        mDownloadCallbackHashMap.values.forEach { it.onDownloadStart(task.getDownloadUrl()) }
    }

    private var lastCallbackDownloadProgressTime = 0L
    private fun callbackDownloading(task: InternalDownloadTask){
        mDownloadDBHelper.updateDownloadTask(task)
        //回调时间最少1s
        val currentTimeMillis = System.currentTimeMillis()
        if(currentTimeMillis - lastCallbackDownloadProgressTime > 1_000){
            mDownloadCallbackHashMap.values.forEach { it.onDownloading(task.getDownloadUrl(),task.downloadSize,task.contentLength) }
            lastCallbackDownloadProgressTime = System.currentTimeMillis()
        }
    }

    private fun callbackDownloadComplete(task: InternalDownloadTask){
        mDownloadTaskList.remove(task)
        mDownloadDBHelper.deleteByKey(task.getKey())
        mDownloadCallbackHashMap.values.forEach { it.onDownloadComplete(task.getDownloadUrl(),task.getDownloadFilePath()) }
    }
    private fun callbackDownloadFail(task: InternalDownloadTask,errorMsg:String){
        mDownloadTaskList.remove(task)
        mDownloadCallbackHashMap.values.forEach { it.onDownloadFail(errorMsg) }
    }

    fun exit(){
        mCurrentHeadHttpURLConnection?.disconnect()
        mCurrentHeadHttpURLConnection?.disconnect()

    }
}