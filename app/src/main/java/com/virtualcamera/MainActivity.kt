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
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var statusText: TextView
    private lateinit var loopButton: Button
    private lateinit var progress: ProgressBar

    private var selectedVideo: Uri? = null
    private var looping = true
    private var prepared = false

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) loadVideo(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 23, 42)
        window.navigationBarColor = Color.rgb(15, 23, 42)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 20, 20, 16)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }

        val title = TextView(this).apply {
            text = "ABSOLUTE VIRTUAL CAMERA"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 10)
        }

        statusText = TextView(this).apply {
            text = "Select a video to begin"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 10)
        }

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            setMediaController(MediaController(this@MainActivity))
        }

        progress = ProgressBar(this).apply {
            visibility = android.view.View.GONE
        }

        val previewFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                videoView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                progress,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(
            previewFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { setMargins(0, 6, 0, 10) }
        )

        root.addView(makeButton("SELECT VIDEO") {
            videoPicker.launch(arrayOf("video/*"))
        })

        loopButton = makeButton("LOOP: ON") {
            looping = !looping
            loopButton.text = if (looping) "LOOP: ON" else "LOOP: OFF"
            if (prepared) {
                statusText.text =
                    if (looping) "Loop enabled" else "Loop disabled"
                // Apply the new setting without reloading the video.
                videoView.setOnCompletionListener {
                    if (looping) {
                        videoView.seekTo(0)
                        videoView.start()
                    }
                }
            }
        }
        root.addView(loopButton)

        root.addView(makeButton("PLAY / RESUME") {
            if (selectedVideo == null) {
                statusText.text = "Select a video first"
            } else if (prepared) {
                videoView.start()
                statusText.text = "Playing preview"
            } else {
                selectedVideo?.let { loadVideo(it) }
            }
        })

        root.addView(makeButton("STOP PREVIEW") {
            videoView.pause()
            statusText.text = "Preview paused"
        })

        root.addView(makeButton("START SERVICE") {
            statusText.text =
                "Preview only: virtual camera feed is not implemented yet."
            Toast.makeText(
                this,
                "Camera feed integration is a separate step",
                Toast.LENGTH_LONG
            ).show()
        })

        setContentView(root)
    }

    private fun loadVideo(uri: Uri) {
        selectedVideo = uri
        prepared = false
        progress.visibility = android.view.View.VISIBLE
        statusText.text = "Loading video..."

        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        videoView.setOnPreparedListener { player ->
            progress.visibility = android.view.View.GONE
            player.isLooping = looping
            prepared = true
            videoView.start()
            statusText.text = "Video ready"
        }

        videoView.setOnErrorListener { _, what, extra ->
            progress.visibility = android.view.View.GONE
            prepared = false
            statusText.text = "Playback error: $what / $extra"
            true
        }

        videoView.setOnCompletionListener {
            if (looping) {
                videoView.seekTo(0)
                videoView.start()
            } else {
                statusText.text = "Video finished"
            }
        }

        videoView.setVideoURI(uri)
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 2, 0, 2) }
        }
    }

    override fun onDestroy() {
        if (::videoView.isInitialized) videoView.stopPlayback()
        super.onDestroy()
    }
}
