package com.lazyee.download.klib

import android.content.Context
import java.io.File
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.WeakHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/21 13:59
 */
private const val TAG = "[DownloadManager]"
class DownloadManager private constructor(mContext: Context,private val mDownloadThreadCoreSize:Int){
    private val mDownloadTaskList = mutableListOf<DownloadTask>()
    private var mDownloadCallbackHashMap = WeakHashMap<Any,DownloadCallback>()
//    private var mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var mExecutorService: ExecutorService
    private var mDownloadDBHelper:DownloadDBHelper
    private val mSuccessDownloadTaskList = mutableListOf<DownloadTask>()
    private val mFailDownloadTaskList = mutableListOf<DownloadTask>()
    private var mLastCallbackDownloadProgressTime = 0L
    private var mDownloadProgressInfoList = mutableListOf<DownloadProgressInfo>()

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

        private fun md5(content:String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(content.toByteArray())
            return bytes.joinToString("") { "%02X".format(it) }
        }

        fun getKeyByUrl(url:String): String {
            return md5(url)
        }
    }


    fun debug(isDebug:Boolean):DownloadManager{
        LogUtils.init(isDebug)
        return this
    }

    fun getDownloadThreadCoreSize():Int = mDownloadThreadCoreSize

    fun download(downloadTask: DownloadTask){
        download(listOf(downloadTask))
    }

    fun download(downloadTaskList:List<DownloadTask>){
        downloadTaskList.forEach  {task->
            if(mDownloadTaskList.find { it.downloadUrl == task.downloadUrl } == null){
                task.setDownloadTaskCallback(mDownloadTaskCallback)
                mDownloadTaskList.add(task)
            }
        }
        realDownload()
    }

    fun download(downloadUrl:String, savePath:String){
        download(listOf(downloadUrl),savePath)
    }

    fun download(downloadUrlList: List<String>,savePath: String){
        val realDownloadUrlList = mutableListOf<String>()
        downloadUrlList.forEach {
            if(!realDownloadUrlList.contains(it)){
                realDownloadUrlList.add(it)
            }
        }
        realDownloadUrlList.forEach { downloadUrl->
            if(mDownloadTaskList.find { it.downloadUrl == downloadUrl } == null){
                val task = DownloadTask(downloadUrl,getKeyByUrl(downloadUrl),savePath)
                task.setDownloadTaskCallback(mDownloadTaskCallback)
                mDownloadTaskList.add(task)
            }
        }
        realDownload()
    }

    private fun realDownload(){
        mDownloadTaskList.forEach {task->
            mExecutorService.execute { task.execute() }
        }

    }


    private val mDownloadTaskCallback = object :DownloadTaskCallback{
        override fun provideDownloadTaskHistory(task: DownloadTask):DownloadTask? {
            return mDownloadDBHelper.getDownloadTaskByKey(task.key).firstOrNull()
        }

        override fun onDownloadStart(task: DownloadTask) {
            mDownloadDBHelper.updateDownloadTask(task)
            mDownloadCallbackHashMap.values.forEach { it.onDownloadStart(task.downloadUrl) }
        }


        override fun onDownloading(task: DownloadTask) {
            mDownloadDBHelper.updateDownloadTask(task)
            var target = mDownloadProgressInfoList.find { it.downloadUrl == task.downloadUrl }
            if(target == null){
                target = DownloadProgressInfo(task.downloadUrl,task.downloadSize,task.contentLength)
                mDownloadProgressInfoList.add(target)
            }else{
                target.currentDownloadSize = task.downloadSize
                target.totalSize = task.contentLength
            }

            //回调时间最少500ms
            val currentTimeMillis = System.currentTimeMillis()
            if(currentTimeMillis - mLastCallbackDownloadProgressTime > 500){
                val callbackDownloadProgressInfoList = mutableListOf<DownloadProgressInfo>()
                mDownloadProgressInfoList.forEach { callbackDownloadProgressInfoList.add(it) }
                if(callbackDownloadProgressInfoList.isNotEmpty()){
                    mDownloadCallbackHashMap.values.forEach { it.onDownloading(callbackDownloadProgressInfoList) }
                }
                mDownloadProgressInfoList.clear()
                mLastCallbackDownloadProgressTime = currentTimeMillis
            }
        }

        override fun onDownloadComplete(task: DownloadTask) {
            mDownloadProgressInfoList.removeAll { it.downloadUrl == task.downloadUrl }
            mDownloadTaskList.remove(task)
            mSuccessDownloadTaskList.add(task)
            mDownloadDBHelper.deleteByKey(task.key)
            mDownloadCallbackHashMap.values.forEach { it.onDownloadComplete(task.downloadUrl,task.downloadFilePath) }
            if(mDownloadTaskList.isEmpty()){
                callbackAllDownloadEnd()
            }
        }

        override fun onDownloadFail(task: DownloadTask, errorMsg: String) {
            mDownloadProgressInfoList.removeAll { it.downloadUrl == task.downloadUrl }
            mDownloadTaskList.remove(task)
            mFailDownloadTaskList.add(task)
            mDownloadCallbackHashMap.values.forEach { it.onDownloadFail(task.downloadUrl,errorMsg) }

            if(mDownloadTaskList.isEmpty()){
                callbackAllDownloadEnd()
            }
        }

        override fun onDownloadFileNotFound(task: DownloadTask) {
            mDownloadDBHelper.deleteByKey(task.key)
        }
    }

    private fun callbackAllDownloadEnd(){
        val successDownloadUrlList = mutableListOf<String>()
        val failDownloadUrlList = mutableListOf<String>()
        mSuccessDownloadTaskList.forEach{
            successDownloadUrlList.add(it.downloadUrl)
        }

        mFailDownloadTaskList.forEach{
            failDownloadUrlList.add(it.downloadUrl)
        }

        mDownloadCallbackHashMap.values.forEach { it.onAllDownloadEnd(successDownloadUrlList,failDownloadUrlList) }
        mSuccessDownloadTaskList.clear()
        mFailDownloadTaskList.clear()
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
        mDownloadProgressInfoList.clear()
    }

    /**
     * 取消下载
     */
    fun cancelAll(){
        mDownloadTaskList.clear()
        mDownloadTaskList.forEach { it.cancel() }
        mExecutorService.shutdownNow()
    }

    /**
     * 从数据库中获取所有下载任务
     */
    fun getAllDownloadTaskFromDB(): MutableList<DownloadTask> {
        return mDownloadDBHelper.getAllDownloadTask()
    }

    /**
     * 删除下载临时文件
     */
    fun deleteDownloadTempFileByKey(key:String,savePath:String){
        var realSavePath:String = savePath
        if(!savePath.endsWith(File.separator)){
            realSavePath = savePath +  File.separator
        }
        val tempDownloadFilePath = realSavePath + "_" + key
        val tempFile = File(tempDownloadFilePath)
        if(!tempFile.exists())return
        if(tempFile.isFile){
            tempFile.delete()
        }
    }

    /**
     * 清空下载临时文件
     */
    fun clearDownloadTempFile(savePath: String){
        val tempFileDir = File(savePath)
        if(!tempFileDir.exists())return
        if(!tempFileDir.isDirectory)return
        Thread{
            tempFileDir.listFiles()?.forEach {file->
                if(file.name.startsWith("_")){
                    file.delete()
                }
            }
        }.start()
    }

    /**
     * 获取下载文件
     */
    fun getDownloadFile(url:String, savePath: String): File {
        val task = DownloadTask(url, md5(url),savePath)
        return File(task.downloadFilePath)
    }
}