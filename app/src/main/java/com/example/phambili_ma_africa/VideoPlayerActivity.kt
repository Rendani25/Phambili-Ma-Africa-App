package com.example.phambili_ma_africa

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val videoView = findViewById<VideoView>(R.id.video_view)
        val videoFile = intent.getStringExtra("video_file") ?: ""

        if (videoFile.isNotEmpty()) {
            val uri = Uri.parse("android.resource://$packageName/raw/$videoFile")
            videoView.setVideoURI(uri)
            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.start()
        }
    }
}
