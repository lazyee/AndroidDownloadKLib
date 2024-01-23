package com.lazyee.download.klib

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:下载管理类
 * Date: 2023/9/21 13:59
 */
private const val TAG = "[DownloadManager]"
class DownloadManager private constructor(mContext: Context,private val mDownloadThreadCoreSize:Int){
    private val mDownloadingTaskList = CopyOnWriteArrayList<DownloadTask>()
    private val mDownloadTaskList = mutableListOf<DownloadTask>()
    private val mSuccessDownloadUrlList = mutableListOf<String>()
    private val mFailDownloadUrlList = mutableListOf<String>()
    private var mCallbackDownloadingTaskList = mutableListOf<DownloadTask>()
    private var mDownloadCallbackHashMap = HashMap<Any,DownloadCallback>()
    private var mExecutorService: ExecutorService
    private var mDownloadDBHelper:DownloadDBHelper
    private var mLastCallbackDownloadProgressTime = 0L
    private var mDownloadHandler = DownloadHandler()
    private var isCancel = false

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

    fun start(){
        isCancel = false
        internalDownload()
    }
    fun download(downloadTask: DownloadTask):DownloadManager{
        return download(listOf(downloadTask))
    }

    fun download(downloadTaskList:List<DownloadTask>):DownloadManager{
        downloadTaskList.forEach  {task->
            if(mDownloadTaskList.find { it.downloadUrl == task.downloadUrl } == null){
                task.setDownloadTaskCallback(mDownloadTaskCallback)
                mDownloadTaskList.add(task)
            }
        }
        return this
    }

    fun download(downloadUrl:String, savePath:String):DownloadManager{
        return download(listOf(downloadUrl),savePath)
    }

    fun download(downloadUrlList: List<String>,savePath: String):DownloadManager{
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
        return this
    }

    private fun internalDownload(){
        if(isCancel)return
        synchronized(mDownloadTaskList){
            while (mDownloadTaskList.isNotEmpty() && mDownloadingTaskList.size < mDownloadThreadCoreSize){
                val task = mDownloadTaskList.removeFirst()
                mDownloadingTaskList.add(task)
                mExecutorService.execute { task.execute() }
            }
        }
    }


    private val mDownloadTaskCallback = object :InternalDownloadTaskCallback{
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

        override fun onDownloadComplete(downloadUrl: String) {
            synchronized(mCallbackDownloadingTaskList){
                mCallbackDownloadingTaskList.removeAll { it.downloadUrl == downloadUrl }
                mDownloadingTaskList.removeAll { it.downloadUrl == downloadUrl }
                if(!mSuccessDownloadUrlList.contains(downloadUrl)){
                    mSuccessDownloadUrlList.add(downloadUrl)
                }
                mDownloadDBHelper.deleteByKey(getKeyByUrl(downloadUrl))

                callbackByHandler{
                    mDownloadCallbackHashMap.values.forEach { it.onDownloadComplete(downloadUrl) }
                }
                internalDownload()
                callbackAllDownloadEnd()
            }
        }

        override fun onDownloadFail(exception: DownloadException) {
            synchronized(mCallbackDownloadingTaskList) {
                val downloadUrl = exception.downloadUrl
                handleDownloadException(exception)

                mCallbackDownloadingTaskList.removeAll { it.downloadUrl == downloadUrl }
                mDownloadingTaskList.removeAll { it.downloadUrl == downloadUrl }
                if(!mFailDownloadUrlList.contains(downloadUrl)){
                    mFailDownloadUrlList.add(downloadUrl)
                }


                callbackByHandler {
                    mDownloadCallbackHashMap.values.forEach { it.onDownloadFail(exception) }
                }
                internalDownload()
                callbackAllDownloadEnd()
            }
        }
    }

    private fun handleDownloadException(exception: DownloadException){
        val downloadUrl = exception.downloadUrl
        if(exception is DownloadFileNotFoundException){
            mDownloadDBHelper.deleteByKey(getKeyByUrl(downloadUrl))
        }
    }

    private fun callbackByHandler(callback:()->Unit){
        mDownloadHandler.sendMessage(mDownloadHandler.obtainDownloadMessage{
            callback()
        })
    }

    private fun callbackAllDownloadEnd(){
        if(isCancel)return
        if(mDownloadTaskList.isNotEmpty() || mDownloadingTaskList.isNotEmpty())return

        mDownloadHandler.sendMessage(mDownloadHandler.obtainDownloadMessage {
            mDownloadCallbackHashMap.values.forEach { it.onAllDownloadEnd(mSuccessDownloadUrlList,mFailDownloadUrlList) }
        })
        mSuccessDownloadUrlList.clear()
        mFailDownloadUrlList.clear()
    }

    fun addDownloadCallback(key:Any,callback: DownloadCallback): DownloadManager {
        if(!mDownloadCallbackHashMap.containsKey(key)){
            mDownloadCallbackHashMap[key] = callback
        }
        return this
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
            isCancel = true
            mDownloadingTaskList.forEach { it.cancel() }
            mDownloadingTaskList.clear()
            mDownloadTaskList.clear()
            mSuccessDownloadUrlList.clear()
            mFailDownloadUrlList.clear()
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