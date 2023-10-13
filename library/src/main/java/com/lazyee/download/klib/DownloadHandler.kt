package com.lazyee.download.klib

import android.os.Handler
import android.os.Message
import java.util.WeakHashMap

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/10/14 00:27
 */
internal const val MSG_DOWNLOAD_TASK = 1
private const val TAG = "[DownloadHandler]"
internal class DownloadHandler(private val mDownloadCallbackHashMap:WeakHashMap<Any,DownloadCallback>) : Handler(){
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        when(msg.what){
            MSG_DOWNLOAD_TASK->{
                (msg.obj as ()->Unit).invoke()
            }
        }
    }

    fun obtainDownloadMessage(callback: ()->Unit): Message {
        return Message.obtain().also {
            it.what = MSG_DOWNLOAD_TASK
            it.obj = callback
        }
    }
}

