package com.virtualcamera

import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import android.widget.MediaController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var statusText: TextView
    private lateinit var loopButton: Button

    private var selectedVideo: Uri? = null
    private var looping = true
    private var prepared = false

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedVideo = uri
                prepared = false

                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                statusText.text = "Loading video..."

                videoView.setVideoURI(uri)
                videoView.setOnPreparedListener { player ->
                    player.isLooping = looping
                    prepared = true
                    videoView.start()
                    statusText.text = "Video ready"
                }
                videoView.setOnErrorListener { _, _, _ ->
                    statusText.text = "Unable to play this video"
                    true
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 23, 42)
        window.navigationBarColor = Color.rgb(15, 23, 42)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }

        val title = TextView(this).apply {
            text = "ABSOLUTE VIRTUAL CAMERA"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 12)
        }

        statusText = TextView(this).apply {
            text = "Select a video to begin"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 12)
        }

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            setMediaController(MediaController(this@MainActivity))
        }

        root.addView(title)
        root.addView(statusText)

        root.addView(
            videoView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply {
                setMargins(0, 8, 0, 12)
            }
        )

        root.addView(makeButton("SELECT VIDEO") {
            videoPicker.launch(arrayOf("video/*"))
        })

        loopButton = makeButton("LOOP: ON") {
            looping = !looping
            loopButton.text = if (looping) "LOOP: ON" else "LOOP: OFF"
            if (prepared) {
                val position = videoView.currentPosition
                videoView.setVideoURI(selectedVideo)
                videoView.setOnPreparedListener { player ->
                    player.isLooping = looping
                    player.seekTo(position)
                    videoView.start()
                    prepared = true
                }
            }
        }
        root.addView(loopButton)

        root.addView(makeButton("START SERVICE") {
            statusText.text =
                if (selectedVideo == null) {
                    "Select a video first. Camera feed is not yet implemented."
                } else {
                    "Video preview is ready. Service integration still needs fixing."
                }
        })

        root.addView(makeButton("STOP PREVIEW") {
            videoView.pause()
            statusText.text = "Preview paused"
        })

        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 3, 0, 3)
            }
        }
    }

    override fun onDestroy() {
        if (::videoView.isInitialized) videoView.stopPlayback()
        super.onDestroy()
    }
}
