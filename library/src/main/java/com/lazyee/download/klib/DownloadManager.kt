package com.lazyee.download.klib

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.util.Vector
import java.util.WeakHashMap
import java.util.concurrent.CopyOnWriteArrayList
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
    private val mDownloadingTaskList = CopyOnWriteArrayList<DownloadTask>()
    private val mDownloadTaskList = mutableListOf<DownloadTask>()
    private val mSuccessDownloadTaskList = mutableListOf<DownloadTask>()
    private val mFailDownloadTaskList = mutableListOf<DownloadTask>()
    private var mCallbackDownloadingTaskList = mutableListOf<DownloadTask>()
    private var mDownloadCallbackHashMap = HashMap<Any,DownloadCallback>()
    private var mExecutorService: ExecutorService
    private var mDownloadDBHelper:DownloadDBHelper
    private var mLastCallbackDownloadProgressTime = 0L
    private var mDownloadHandler = DownloadHandler()

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
        internalDownload()
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
            synchronized(mDownloadTaskList){
                if(mDownloadTaskList.find { it.downloadUrl == downloadUrl } == null){
                    val task = DownloadTask(downloadUrl,getKeyByUrl(downloadUrl),savePath)
                    task.setDownloadTaskCallback(mDownloadTaskCallback)
                    mDownloadTaskList.add(task)
                }
            }
        }
        internalDownload()
    }

    private fun internalDownload(){
        synchronized(mDownloadTaskList){
            while (mDownloadTaskList.isNotEmpty() && mDownloadingTaskList.size < mDownloadThreadCoreSize){
                val task = mDownloadTaskList.removeFirst()
                mDownloadingTaskList.add(task)
                mExecutorService.execute { task.execute() }
            }
        }
    }


    private val mDownloadTaskCallback = object :DownloadTaskCallback{
        override fun provideDownloadTaskHistory(task: DownloadTask):DownloadTask? {
            return mDownloadDBHelper.getDownloadTaskByKey(task.key).firstOrNull()
        }

        override fun onDownloadStart(task: DownloadTask) {
            mDownloadDBHelper.updateDownloadTask(task)
            callbackByHandler{
                mDownloadCallbackHashMap.values.forEach { it.onDownloadStart(task) }
            }
        }


        override fun onDownloading(task: DownloadTask) {
            synchronized(mCallbackDownloadingTaskList){
                if(!mCallbackDownloadingTaskList.contains(task)){
                    mCallbackDownloadingTaskList.add(task)
                }

                //回调时间最少500ms
                val currentTimeMillis = System.currentTimeMillis()
                if(currentTimeMillis - mLastCallbackDownloadProgressTime > 500){
                    val callbackDownloadingTaskList = mutableListOf<DownloadTask?>()
                    mCallbackDownloadingTaskList.forEach { callbackDownloadingTaskList.add(it) }
                    if(callbackDownloadingTaskList.isNotEmpty()){
                        callbackByHandler{
                            mDownloadCallbackHashMap.values.forEach { it.onDownloading(callbackDownloadingTaskList) }
                        }
                    }

                    mCallbackDownloadingTaskList.clear()
                    mLastCallbackDownloadProgressTime = currentTimeMillis
                }
            }
        }

        override fun onDownloadComplete(task: DownloadTask) {
            synchronized(mCallbackDownloadingTaskList){
                mCallbackDownloadingTaskList.removeAll { it.downloadUrl == task.downloadUrl }
                mDownloadingTaskList.remove(task)
                mSuccessDownloadTaskList.add(task)
                mDownloadDBHelper.deleteByKey(task.key)

                callbackByHandler{
                    mDownloadCallbackHashMap.values.forEach { it.onDownloadComplete(task) }
                }
                internalDownload()
                callbackAllDownloadEnd()
            }
        }

        override fun onDownloadFail(exception: DownloadException) {
            synchronized(mCallbackDownloadingTaskList) {
                val task = exception.task
                handleDownloadException(exception)

                mCallbackDownloadingTaskList.removeAll { it.downloadUrl == task.downloadUrl }
                mDownloadingTaskList.remove(task)
                mFailDownloadTaskList.add(task)

                callbackByHandler {
                    mDownloadCallbackHashMap.values.forEach { it.onDownloadFail(exception) }
                }
                internalDownload()
                callbackAllDownloadEnd()
            }
        }
    }

    private fun handleDownloadException(exception: DownloadException){
        val task = exception.task
        if(exception is DownloadFileNotFoundException){
            mDownloadDBHelper.deleteByKey(task.key)
        }
    }

    private fun callbackByHandler(callback:()->Unit){
        mDownloadHandler.sendMessage(mDownloadHandler.obtainDownloadMessage{
//            LogUtils.e(TAG,"thread:${Thread.currentThread().name }")
            callback()
        })
    }

    private fun callbackAllDownloadEnd(){
        if(mDownloadTaskList.isNotEmpty() || mDownloadingTaskList.isNotEmpty())return
        val successDownloadUrlList = mutableListOf<String>()
        val failDownloadUrlList = mutableListOf<String>()
        mSuccessDownloadTaskList.forEach{
            if(!successDownloadUrlList.contains(it.downloadUrl)){
                successDownloadUrlList.add(it.downloadUrl)
            }
        }

        mFailDownloadTaskList.forEach{
            if(!failDownloadUrlList.contains(it.downloadUrl)){
                failDownloadUrlList.add(it.downloadUrl)
            }
        }

        mDownloadHandler.sendMessage(mDownloadHandler.obtainDownloadMessage {
            mDownloadCallbackHashMap.values.forEach { it.onAllDownloadEnd(successDownloadUrlList,failDownloadUrlList) }
        })
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
        mCallbackDownloadingTaskList.clear()
    }

    /**
     * 取消下载
     */
    fun cancelAll(){
        try{
            mDownloadingTaskList.forEach { it.cancel() }
            mDownloadingTaskList.clear()
            mDownloadTaskList.clear()
            mSuccessDownloadTaskList.clear()
            mFailDownloadTaskList.clear()
            mCallbackDownloadingTaskList.clear()
        }catch (e:Exception){
            e.printStackTrace()
        }
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