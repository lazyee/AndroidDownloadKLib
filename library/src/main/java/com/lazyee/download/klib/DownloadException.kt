package com.lazyee.download.klib

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:定义下载流程中发现的异常
 * Date: 2023/10/13 23:38
 */
open class DownloadException(val task: DownloadTask, msg:String) : Exception(msg)

/**
 * 404 exception
 */
class DownloadFileNotFoundException(task: DownloadTask,msg: String = "下载失败,状态码:[404],文件不存在") : DownloadException(task,msg)

/**
 * 在测试过程中发现，有时候可以正常资讯读取操作，但是读到的内容是空的，从而导致下载了无效的文件的清空，针对这种清空，创建了读取空值的异常
 */
class DownloadFileReadEmptyValueException(task:DownloadTask,msg: String):DownloadException(task,msg)