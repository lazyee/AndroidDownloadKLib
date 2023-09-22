package com.lazyee.download.klib

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Author: leeorz
 * Email: 378229364@qq.com
 * Description:
 * Date: 2023/9/22 16:07
 */
private const val TAG = "DownloadDBHelper"
private const val T_DOWNLOAD = "t_download"
private const val DB_NAME = "download.db"
private const val VERSION = 1
private const val COLUMN_ID = "_id"
private const val COLUMN_FILE_KEY = "file_key"
private const val COLUMN_DOWNLOAD_URL = "download_url"
private const val COLUMN_SAVE_FILE_PATH = "save_file_path"
private const val COLUMN_DOWNLOAD_SIZE = "download_size"
private const val COLUMN_TOTAL_SIZE = "total_size"
private const val COLUMN_SUPPORT_SPLIT_DOWNLOAD = "support_split_download"
class DownloadDBHelper(context: Context) :SQLiteOpenHelper(context,DB_NAME,null,VERSION){
    override fun onCreate(db: SQLiteDatabase?) {
        val downloadTableSql = """
            CREATE TABLE IF NOT EXISTS $T_DOWNLOAD (
            '$COLUMN_ID' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            '$COLUMN_FILE_KEY' LONG,
            '$COLUMN_DOWNLOAD_URL' VARCHAR,
            '$COLUMN_SAVE_FILE_PATH' VARCHAR,
            '$COLUMN_DOWNLOAD_SIZE' LONG,
            '$COLUMN_TOTAL_SIZE' LONG,
            '$COLUMN_SUPPORT_SPLIT_DOWNLOAD' INTEGER
            );
        """.trimIndent()
        Log.e(TAG,"downloadSql:$downloadTableSql")
        db?.execSQL(downloadTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun getDownloadTaskByKey(key:String) : MutableList<InternalDownloadTask> {
        val taskList = mutableListOf<InternalDownloadTask>()
        val cursor = readableDatabase.query(T_DOWNLOAD,null,"$COLUMN_FILE_KEY=?", arrayOf(key),null,null,null)
        while (cursor.moveToNext()){
            taskList.add( InternalDownloadTask(cursor.getString(cursor.getColumnIndex(COLUMN_DOWNLOAD_URL)),
                cursor.getString(cursor.getColumnIndex(COLUMN_FILE_KEY)),
                cursor.getString(cursor.getColumnIndex(COLUMN_SAVE_FILE_PATH)),).also {
                    it.contentLength = cursor.getLong(cursor.getColumnIndex(COLUMN_TOTAL_SIZE))
                    it.downloadSize = cursor.getLong(cursor.getColumnIndex(COLUMN_DOWNLOAD_SIZE))
                    it.isSupportSplitDownload = cursor.getInt(cursor.getColumnIndex(COLUMN_SUPPORT_SPLIT_DOWNLOAD)) == 1
            })
        }
        cursor.close()
        return taskList
    }

    fun exist(key: String):Boolean{
        val cursor = readableDatabase.query(T_DOWNLOAD,
            arrayOf(COLUMN_FILE_KEY),"$COLUMN_FILE_KEY=?", arrayOf(key),null,null,null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun deleteByKey(key:String){
        writableDatabase.delete(T_DOWNLOAD,"$COLUMN_FILE_KEY=?", arrayOf(key))
    }

    fun updateDownloadTask(task:InternalDownloadTask){
        writableDatabase.beginTransaction()
        val values = ContentValues()
        values.put(COLUMN_FILE_KEY,task.getKey())
        values.put(COLUMN_DOWNLOAD_URL,task.getDownloadUrl())
        values.put(COLUMN_SAVE_FILE_PATH,task.getDownloadFilePath())
        values.put(COLUMN_DOWNLOAD_SIZE,task.downloadSize)
        values.put(COLUMN_TOTAL_SIZE,task.contentLength)
        values.put(COLUMN_SUPPORT_SPLIT_DOWNLOAD,if(task.isSupportSplitDownload) 1 else 0)
        if(exist(task.getKey())){
            writableDatabase.update(T_DOWNLOAD,values,"$COLUMN_FILE_KEY=?", arrayOf(task.getKey()))
        }else{
            writableDatabase.insert(T_DOWNLOAD,null,values)
        }

        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    fun closeDB(){
        if(readableDatabase.isOpen){
            readableDatabase.close()
        }

        if(writableDatabase.isOpen){
            writableDatabase.close()
        }
    }
}