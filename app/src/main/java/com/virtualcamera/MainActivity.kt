package com.virtualcamera

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var selectedAppText: TextView
    private lateinit var photoPreview: ImageView

    private var selectedAppPackage: String? = null
    private var selectedPhotoUri: Uri? = null

    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                photoPreview.setImageURI(uri)
                photoPreview.visibility = ImageView.VISIBLE
                statusText.text = "Photo selected successfully"
            }
        }

    private val appPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val packageName = data?.getStringExtra("selected_package")
                val appName = data?.getStringExtra("selected_app_name")

                if (packageName != null) {
                    selectedAppPackage = packageName
                    selectedAppText.text = "Selected app: ${appName ?: packageName}"
                    statusText.text = "Target app selected"
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
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }

        val scrollView = ScrollView(this)
        scrollView.addView(root)

        val title = TextView(this).apply {
            text = "ABSOLUTE VIRTUAL CAMERA"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 20)
        }

        statusText = TextView(this).apply {
            text = "Select an app and a photo"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 12)
        }

        selectedAppText = TextView(this).apply {
            text = "No app selected"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 12)
        }

        photoPreview = ImageView(this).apply {
            visibility = ImageView.GONE
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            adjustViewBounds = true
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(selectedAppText)

        root.addView(makeButton("SELECT APP") {
            showAppPicker()
        })

        root.addView(makeButton("SELECT PHOTO") {
            photoPicker.launch("image/*")
        })

        root.addView(
            photoPreview,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300
            ).apply {
                setMargins(0, 12, 0, 12)
            }
        )

        root.addView(makeButton("OPEN APP") {
            openSelectedApp()
        })

        root.addView(makeButton("START SERVICE") {
            statusText.text =
                "Photo preview is available. Camera feed integration is separate."
            Toast.makeText(
                this,
                "This button does not start a camera feed yet.",
                Toast.LENGTH_LONG
            ).show()
        })

        root.addView(makeButton("STOP PREVIEW") {
            photoPreview.setImageDrawable(null)
            photoPreview.visibility = ImageView.GONE
            statusText.text = "Preview cleared"
        })

        setContentView(scrollView)
    }

    private fun showAppPicker() {
        val intent = Intent(this, TargetAppPickerActivity::class.java)
        appPicker.launch(intent)
    }

    private fun openSelectedApp() {
        val packageName = selectedAppPackage

        if (packageName == null) {
            statusText.text = "Please select an app first"
            return
        }

        val launchIntent = packageManager
            .getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            statusText.text = "Unable to open selected app"
        }
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 5)
            }
        }
    }
}
