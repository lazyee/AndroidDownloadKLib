package com.lazyee.download.demo

import android.app.Application
import com.liulishuo.filedownloader.FileDownloader

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2024/4/19 15:36
 */
class MyApplication :Application(){

    override fun onCreate() {
        super.onCreate()
        FileDownloader.setupOnApplicationOnCreate(this)
    }
}