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
class DownloadManager private constructor(mContext: Context,mDownloadThreadCoreSize:Int){
    private val mDownloadTaskList = mutableListOf<InternalDownloadTask>()
    private var mDownloadCallbackHashMap = hashMapOf<Any,DownloadCallback>()
//    private var mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var mExecutorService: ExecutorService
    private var mDownloadDBHelper:DownloadDBHelper

    init {
        mDownloadDBHelper = DownloadDBHelper(mContext)
        mExecutorService = Executors.newFixedThreadPool(mDownloadThreadCoreSize)
    }

    companion object{
        fun with(context: Context,downloadThreadCoreSize:Int = 3): DownloadManager {
            val downloadManager = DownloadManager(context,downloadThreadCoreSize)
            downloadManager.debug(false)
            return downloadManager
        }
    }


    fun debug(isDebug:Boolean):DownloadManager{
        LogUtils.init(isDebug)
        return this
    }


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
                val task = InternalDownloadTask(downloadUrl,getKeyByUrl(downloadUrl),savePath)
                task.setDownloadTaskCallback(mDownloadTaskCallback)
                mDownloadTaskList.add(task)
            }
        }
        realDownload()
    }

    private fun realDownload(){
        mDownloadTaskList.forEach {task ->
            if(task.isReadyDownload) return@forEach
            if(task.isDownloading) return@forEach
            mExecutorService.execute { task.execute() }
        }
    }

    private val mDownloadTaskCallback = object :DownloadTaskCallback{

        override fun provideDownloadTaskHistory(task: InternalDownloadTask):InternalDownloadTask? {
            return mDownloadDBHelper.getDownloadTaskByKey(task.getKey()).firstOrNull()
        }

        override fun onDownloadStart(task: InternalDownloadTask) {
            mDownloadDBHelper.updateDownloadTask(task)
            mDownloadCallbackHashMap.values.forEach { it.onDownloadStart(task.getDownloadUrl()) }
        }

        private var lastCallbackDownloadProgressTime = 0L
        override fun onDownloading(task: InternalDownloadTask) {
            mDownloadDBHelper.updateDownloadTask(task)
            //回调时间最少500ms
            val currentTimeMillis = System.currentTimeMillis()
            if(currentTimeMillis - lastCallbackDownloadProgressTime > 500){
                mDownloadCallbackHashMap.values.forEach { it.onDownloading(task.getDownloadUrl(),task.downloadSize,task.contentLength) }
                lastCallbackDownloadProgressTime = System.currentTimeMillis()
            }
        }

        override fun onDownloadComplete(task: InternalDownloadTask) {
            mDownloadTaskList.remove(task)
            mDownloadDBHelper.deleteByKey(task.getKey())
            mDownloadCallbackHashMap.values.forEach { it.onDownloadComplete(task.getDownloadUrl(),task.getDownloadFilePath()) }
        }

        override fun onDownloadFail(task: InternalDownloadTask, errorMsg: String) {
            mDownloadTaskList.remove(task)
            mDownloadCallbackHashMap.values.forEach { it.onDownloadFail(errorMsg) }
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

    /**
     * 取消下载
     */
    fun cancelAll(){
        mDownloadTaskList.forEach { it.cancel() }
    }
}