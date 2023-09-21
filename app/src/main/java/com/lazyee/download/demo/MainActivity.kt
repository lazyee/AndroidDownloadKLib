package com.lazyee.download.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val btnResumeDownload by lazy { findViewById<Button>(R.id.btnResumeDownload) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnResumeDownload.setOnClickListener {
            startActivity(Intent(this@MainActivity,ResumeDownloadActivity::class.java))
        }
    }
}
