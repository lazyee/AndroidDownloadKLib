package com.lazyee.download.klib

import android.text.TextUtils
import android.util.Log

/**
 * @Author leeorz
 * @Date 2020/11/2-6:54 PM
 * @Description:日志工具类
 */
internal object LogUtils {

    private var isDebug = true
    fun init(isDebug:Boolean){
        this.isDebug = isDebug
    }

    fun d(tag: String?, any: Any?) {
        if (!isDebug) return
        Log.d(getTag(tag), getMsg(any))
    }

    fun e(tag: String?, any: Any?) {
        Log.e(getTag(tag), getMsg(any))
    }

    fun i(tag: String?, any: Any?) {
        if (!isDebug) return
        Log.i(getTag(tag), getMsg(any))
    }

    fun w(tag: String?,any: Any?){
        if (!isDebug)return
        Log.w(getTag(tag), getMsg(any))
    }

    fun v(tag: String?,any: Any?){
        if (!isDebug)return
        Log.v(getTag(tag), getMsg(any))
    }

    private fun getTag(tag: String?):String{
        if (TextUtils.isEmpty(tag))return "[TAG]"
        return tag!!
    }

    private fun getMsg(any:Any?):String{
        if (any == null)return "[null]"
        if (any is String) return any
        return any.toString()
    }
}
