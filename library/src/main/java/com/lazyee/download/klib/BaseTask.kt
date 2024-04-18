package com.lazyee.download.klib

import java.net.URLEncoder
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:下载任务基类
 * Date: 2024/1/16 09:54
 */
open class BaseTask(val downloadRequest:DownloadRequest) {
    private val TAG = "[BaseTask]"
    internal val CONNECTION_TIMEOUT = 3_000
    internal val READ_TIMEOUT = 3_000
    internal val MAX_RETRY_COUNT = 3//最大重试次数

    var isCancelTask = false

    open fun cancel(){
        isCancelTask = true
    }

    internal fun urlEncodeChinese(url:String): String {
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

    internal fun isByteArrayEmpty(byteArray: ByteArray): Boolean {
        return byteArray.all { it.toInt() == -1 }
    }
}