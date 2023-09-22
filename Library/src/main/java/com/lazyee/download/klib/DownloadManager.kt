package com.lazyee.download.klib

import android.content.Context
import android.text.TextUtils
import android.util.Log
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
class DownloadManager(private val mContext: Context) {
    private val mDownloadTaskList = mutableListOf<InternalDownloadTask>()
    private var mDownloadCallbackHashMap = hashMapOf<Any,DownloadCallback>()
    private var mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var mCurrentDownloadHttpURLConnection:HttpURLConnection? = null
    private var mCurrentHeadHttpURLConnection:HttpURLConnection? = null
    private val mDownloadDBHelper = DownloadDBHelper(mContext)

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

    private fun realDownload(){
        val task = mDownloadTaskList.firstOrNull()
        task?:return
        if(task.isDownloading) return
        mExecutorService.execute {
            while (mDownloadTaskList.isNotEmpty()){
                executeDownloadTask(task)
            }
        }
    }

    private fun executeDownloadTask(task:InternalDownloadTask){
        try{

            if(!checkDownloadUrlHead(task))return
            callbackDownloadStart(task)
            var alreadyDownloadSize = mDownloadDBHelper.getDownloadTaskByKey(task.getKey()).firstOrNull()?.downloadSize?:0L

            val httpUrlConnection = URL(task.getDownloadUrl()).openConnection() as HttpURLConnection
            mCurrentDownloadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "GET"

            if (task.isSupportSplitDownload && alreadyDownloadSize > 0){
                createDownloadFile(task.getDownloadFilePath(),false)
                httpUrlConnection.setRequestProperty("Range", "bytes=$alreadyDownloadSize-")
                Log.e(TAG,"分片下载 startIndex:$alreadyDownloadSize")
            }else{
                Log.e(TAG,"全量下载")
                createDownloadFile(task.getDownloadFilePath(),true)
            }

            Log.e(TAG,"response code:${httpUrlConnection.responseCode}")
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

                callbackDownloadComplete(task)
                httpUrlConnection.inputStream.close()
            } else {
                callbackDownloadFail("download fail,stateCode:${httpUrlConnection.responseCode}")
            }
            mDownloadTaskList.remove(task)
            httpUrlConnection.disconnect()
        } catch (e: Exception) {
            callbackDownloadFail("exception:${e.message}")
            e.printStackTrace()
            mDownloadTaskList.remove(task)
        }
    }

    private fun checkDownloadUrlHead(task: InternalDownloadTask): Boolean {
        val httpUrlConnection = URL(task.getDownloadUrl()).openConnection() as HttpURLConnection
        mCurrentHeadHttpURLConnection = httpUrlConnection
        httpUrlConnection.requestMethod = "HEAD"
        httpUrlConnection.connectTimeout = 500
        httpUrlConnection.readTimeout = 500
        val responseCode = httpUrlConnection.responseCode
        Log.e(TAG,"responseCode:$responseCode")
        if(responseCode == HttpURLConnection.HTTP_OK){
            val contentLength:Long = httpUrlConnection.contentLength.toLong()
            task.contentLength = contentLength
            Log.e(TAG,"contentLength:$contentLength")
            val acceptRanges = httpUrlConnection.getHeaderField("Accept-Ranges")
            if(acceptRanges != null && acceptRanges.lowercase() == "bytes"){
                Log.e(TAG,"支持分片下载")
                task.isSupportSplitDownload = true
            }else{
                Log.e(TAG,"不支持分片下载")
                task.isSupportSplitDownload = false
            }
        }
        httpUrlConnection.disconnect()
        return responseCode == HttpURLConnection.HTTP_OK

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
    private fun callbackDownloading(task: InternalDownloadTask){
        mDownloadDBHelper.updateDownloadTask(task)
        mDownloadCallbackHashMap.values.forEach { it.onDownloading(task.getDownloadUrl(),task.downloadSize,task.contentLength) }
    }
    private fun callbackDownloadComplete(task: InternalDownloadTask){
        mDownloadDBHelper.deleteByKey(task.getKey())
        mDownloadCallbackHashMap.values.forEach { it.onDownloadComplete(task.getDownloadUrl(),task.getDownloadFilePath()) }
    }
    private fun callbackDownloadFail(errorMsg:String){
        mDownloadCallbackHashMap.values.forEach { it.onDownloadFail(errorMsg) }
    }

    fun exit(){
        mCurrentHeadHttpURLConnection?.disconnect()
        mCurrentHeadHttpURLConnection?.disconnect()

    }
}