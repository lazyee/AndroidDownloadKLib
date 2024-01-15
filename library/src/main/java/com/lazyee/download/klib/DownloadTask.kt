package com.lazyee.download.klib

import android.text.TextUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:下载任务
 * Date: 2023/9/21 15:05
 */
private const val TAG = "[DownloadTask]"
private const val MAX_RETRY_COUNT = 3//最大重试次数
private const val CONNECTION_TIMEOUT = 3_000
private const val READ_TIMEOUT = 3_000
class DownloadTask(val downloadUrl:String, val key:String, private val savePath:String) {
    var downloadSize = 0L
    var isSupportSplitDownload = false
    var contentLength = 0L
    val downloadFilePath:String
    private val tempDownloadFilePath:String
    private val bufferSize = 32 * 1024//一次性读取32k数据
    private var retryCount = 0//重试次数

    private var isCancelTask = false
    private var mCurrentDownloadHttpURLConnection: HttpURLConnection? = null
    private var mCurrentHeadHttpURLConnection: HttpURLConnection? = null
    private var mDownloadTaskCallback:DownloadTaskCallback? = null
    private var mDownloadHandler :DownloadHandler? = null

    constructor(downloadUrl:String,downloadFilePath:String):this(downloadUrl,File(downloadFilePath))
    constructor(downloadUrl:String,downloadFile:File):this(downloadUrl,downloadFile.name,downloadFile.parentFile?.absolutePath?:"")

    init {
        var realSavePath:String = savePath
        if(!savePath.endsWith(File.separator)){
            realSavePath = savePath +  File.separator
        }
        downloadFilePath = realSavePath + key
        tempDownloadFilePath = realSavePath + "_" + key
    }

    internal fun setDownloadTaskCallback(callback: InternalDownloadTaskCallback){
        mDownloadTaskCallback = callback
    }

    fun justDownload(callback:DownloadTaskCallback){
        mDownloadTaskCallback = callback
        mDownloadHandler = DownloadHandler()
        Thread{ execute() }.start()
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

    internal fun execute(){
        try{
            if(isCancelTask){
                LogUtils.e(TAG,"下载任务已经取消!!!")
                return
            }
            val downloadFileProperty = checkDownloadUrlHead(this)
            if(downloadFileProperty == null){
                if(!retry()){
                    LogUtils.e(TAG,"检查文件属性失败,结束下载任务:${downloadUrl}")
                    callbackDownloadFail(DownloadException(this,"检查文件属性失败,结束下载任务:${downloadUrl}"))
                }
                return
            }

            this.contentLength = downloadFileProperty.contentLength
            this.isSupportSplitDownload = downloadFileProperty.isSupportSplitDownload

            var downloadTaskRecord:DownloadTask? = provideDownloadTaskHistory()

            //获取本地临时下载文件的文件大小，此大小作为目前已经下载的文件大小
            val tempDownloadFile = File(tempDownloadFilePath)
            var alreadyDownloadSize = 0L
            if(tempDownloadFile.exists()){
                if(isSupportSplitDownload){
                    alreadyDownloadSize = tempDownloadFile.length()
                    LogUtils.e(TAG,"本地存在临时下载文件,临时文件大小为:[${alreadyDownloadSize}]")
                }else{
                    tempDownloadFile.delete()
                }
            }

            if(downloadTaskRecord != null && !checkConsistencyFromDB(downloadFileProperty,downloadTaskRecord)){
                LogUtils.e(TAG,"未在数据库中找到相关下载记录或者数据库记录中文件一致性校验不通过，准备重新下载")
                alreadyDownloadSize = 0L
            }

            if(checkConsistencyFromLocalFile(downloadFileProperty) && alreadyDownloadSize == 0L){
                LogUtils.e(TAG,"在本地文件检测到文件已经完整下载，跳过本次下载任务")
                callbackDownloadComplete()
                return
            }

            if(alreadyDownloadSize == downloadFileProperty.contentLength){
                LogUtils.e(TAG,"在本地文件检测到临时文件和目标大小一致，跳过本次下载")
                File(tempDownloadFilePath).renameTo(File(downloadFilePath))
                callbackDownloadComplete()
                return
            }

            callbackDownloadStart()
            val httpUrlConnection = URL(urlEncodeChinese(downloadUrl)).openConnection() as HttpURLConnection
            mCurrentDownloadHttpURLConnection = httpUrlConnection
            httpUrlConnection.requestMethod = "GET"
            httpUrlConnection.readTimeout = READ_TIMEOUT
            httpUrlConnection.connectTimeout = CONNECTION_TIMEOUT
            if (isSupportSplitDownload && alreadyDownloadSize > 0){
                createDownloadFile(tempDownloadFilePath,false)
                httpUrlConnection.setRequestProperty("Range", "bytes=$alreadyDownloadSize-")
                LogUtils.e(TAG,"开始分片下载，从位置:${alreadyDownloadSize}位置开始")
            }else{
                LogUtils.e(TAG,"开始全量下载")
                createDownloadFile(tempDownloadFilePath,true)
            }

            httpUrlConnection.connect()
            when(httpUrlConnection.responseCode){
                HttpURLConnection.HTTP_PARTIAL,
                HttpURLConnection.HTTP_OK->{
                    val buffer = ByteArray(bufferSize,){-1}
                    var readSize = 0
                    val randomAccessFile = RandomAccessFile(tempDownloadFilePath, "rwd")
                    randomAccessFile.seek(alreadyDownloadSize)
                    val bufferedInputStream = BufferedInputStream(httpUrlConnection.inputStream)

                    while (bufferedInputStream.read(buffer).also { readSize = it } != -1 && !isCancelTask) {
                        if(isByteArrayEmpty(buffer)) throw DownloadFileReadEmptyValueException(this,"读取在线资源错误")
                        randomAccessFile.write(buffer, 0, readSize)
                        alreadyDownloadSize += readSize
                        downloadSize = alreadyDownloadSize
                        callbackDownloading()
                    }

                    if(alreadyDownloadSize == downloadFileProperty.contentLength
                        || downloadFileProperty.contentLength <= 0){//文件已经完整下载
                        File(tempDownloadFilePath).renameTo(File(downloadFilePath))
                        callbackDownloadComplete()
                    }

                    httpUrlConnection.inputStream.close()
                    httpUrlConnection.disconnect()
                }
                HttpURLConnection.HTTP_NOT_FOUND->{
                    httpUrlConnection.disconnect()
                    callbackDownloadFail(DownloadFileNotFoundException(this))
                }
                else->{
                    httpUrlConnection.disconnect()
                    if(!retry()){
                        callbackDownloadFail(DownloadException(this,"下载失败,状态码:${httpUrlConnection.responseCode}"))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if(e is DownloadFileNotFoundException){
                callbackDownloadFail(e)
                return
            }
            if(!retry()){
                if(e is DownloadException){
                    callbackDownloadFail(e)
                }else{
                    callbackDownloadFail(DownloadException(this,"${e.message}"))
                }
                retryCount = 0
            }
        }
    }



    private fun provideDownloadTaskHistory(): DownloadTask? {
        if(mDownloadTaskCallback is InternalDownloadTaskCallback){
            return (mDownloadTaskCallback as InternalDownloadTaskCallback).provideDownloadTaskHistory(this)
        }

        return null
    }

    private fun callbackDownloadStart(){
        mDownloadTaskCallback?:return
        if(mDownloadTaskCallback is InternalDownloadTaskCallback){
            mDownloadTaskCallback?.onDownloadStart(this)
            return
        }

        mDownloadHandler?.run {
            sendMessage(obtainDownloadMessage {
                mDownloadTaskCallback?.onDownloadStart(this@DownloadTask)
            })
        }

    }

    private fun callbackDownloading(){
        mDownloadTaskCallback?:return
        if(mDownloadTaskCallback is InternalDownloadTaskCallback){
            mDownloadTaskCallback?.onDownloading(this)
            return
        }

        mDownloadHandler?.run {
            sendMessage(obtainDownloadMessage {
                mDownloadTaskCallback?.onDownloading(this@DownloadTask)
            })
        }
    }

    private fun callbackDownloadComplete(){
        LogUtils.e(TAG,"下载完成")
        mDownloadTaskCallback?:return
        if(mDownloadTaskCallback is InternalDownloadTaskCallback){
            mDownloadTaskCallback?.onDownloadComplete(this)
            return
        }

        mDownloadHandler?.run {
            sendMessage(obtainDownloadMessage {
                mDownloadTaskCallback?.onDownloadComplete(this@DownloadTask)
            })
        }
    }

    private fun callbackDownloadFail(exception :DownloadException){
        if(isCancelTask)return
        LogUtils.e(TAG,"下载失败")
        mDownloadTaskCallback?:return
        if(mDownloadTaskCallback is InternalDownloadTaskCallback){
            mDownloadTaskCallback?.onDownloadFail(exception)
            return
        }

        mDownloadHandler?.run {
            sendMessage(obtainDownloadMessage {
                mDownloadTaskCallback?.onDownloadFail(exception)
            })
        }
    }

    private fun isByteArrayEmpty(byteArray: ByteArray): Boolean {
        return byteArray.all { it.toInt() == -1 }
    }

    private fun retry(): Boolean {
        if(isCancelTask) return false
        if(retryCount < MAX_RETRY_COUNT){
            retryCount++
            LogUtils.e(TAG,"正在第${retryCount}次重试下载任务[${downloadUrl}]")
            execute()
            return true
        }

        return false
    }

    private fun checkDownloadUrlHead(task: DownloadTask): DownloadFileProperty? {
        var downloadFIleProperty: DownloadFileProperty? = null

        if(isCancelTask)return null
        LogUtils.e(TAG,"[HEAD]开始检查下载文件属性,链接:${task.downloadUrl}")
        val httpUrlConnection = URL(urlEncodeChinese(task.downloadUrl)).openConnection() as HttpURLConnection
        mCurrentHeadHttpURLConnection = httpUrlConnection
        httpUrlConnection.requestMethod = "HEAD"
        httpUrlConnection.connectTimeout = CONNECTION_TIMEOUT
        httpUrlConnection.readTimeout = READ_TIMEOUT
        httpUrlConnection.connect()

        when(httpUrlConnection.responseCode){
            HttpURLConnection.HTTP_OK->{
                LogUtils.e(TAG,"[HEAD]请求获取文件信息成功,链接:${task.downloadUrl}")
                val contentLength:Long = httpUrlConnection.contentLength.toLong()
                LogUtils.e(TAG,"[HEAD]文件大小:[${contentLength}]")
                val acceptRanges = httpUrlConnection.getHeaderField("Accept-Ranges")
                val isSupportSplitDownload = acceptRanges != null && acceptRanges.toLowerCase(Locale.ROOT) == "bytes"
                LogUtils.e(TAG,"[HEAD]链接:${task.downloadUrl}" +  if(isSupportSplitDownload)",支持分片下载" else "不支持分片下载")
                downloadFIleProperty = DownloadFileProperty(contentLength,isSupportSplitDownload)
            }
            HttpURLConnection.HTTP_NOT_FOUND->{
                LogUtils.e(TAG,"[HEAD]文件不存在,链接:${task.downloadUrl}")
                httpUrlConnection.disconnect()
                throw DownloadFileNotFoundException(this)
            }
        }

        httpUrlConnection.disconnect()
        return downloadFIleProperty
    }



    /**
     * 检查文件一致性
     */
    private fun checkConsistencyFromDB(property: DownloadFileProperty,downloadTaskRecord:DownloadTask): Boolean {
        return property.contentLength == downloadTaskRecord.contentLength
    }

    private fun checkConsistencyFromLocalFile(property: DownloadFileProperty): Boolean {
        val file = File(downloadFilePath)
        val fileLength = if(file.exists()) file.length() else 0L
        return property.contentLength == fileLength
    }


    /**
     * 取消下载
     */
    fun cancel(){
        isCancelTask = true
        mCurrentHeadHttpURLConnection?.disconnect()
        mCurrentDownloadHttpURLConnection?.disconnect()
        mCurrentHeadHttpURLConnection = null
        mCurrentDownloadHttpURLConnection = null
    }

    private fun urlEncodeChinese(url:String): String {
        var finalUrl = url
        val pattern: Pattern = Pattern.compile("([\\u4e00-\\u9fa5]+)")
        val matcher: Matcher = pattern.matcher(url)

        if(matcher.find()){
            val groupCount = matcher.groupCount()
            repeat(groupCount){
                val matchChinese = matcher.group(it + 1)
                val urlEncodeChinese = URLEncoder.encode(matchChinese,"UTF-8")
                finalUrl = finalUrl.replace(matchChinese,urlEncodeChinese)
            }
            LogUtils.e(TAG,"链接中发现中文,对中文进行URLEncode编码:$finalUrl")
        }

        return finalUrl
    }
}