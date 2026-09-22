package com.sniffy.uvpaint

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sniffy.uvpaint.gl.PaintGLSurfaceView
import com.sniffy.uvpaint.gl.loadMeshFromFile
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var glView: PaintGLSurfaceView

    // In-app file browser (FilePickerActivity) — replaces the system
    // ACTION_OPEN_DOCUMENT picker. It hands back a real filesystem path
    // rather than a single opaque content:// Uri, so a model's companion
    // files (loose glTF .bin/textures, OBJ .mtl) sitting next to it stay
    // resolvable by assimp instead of being left behind.
    private val pickModel = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(FilePickerActivity.EXTRA_PATH)?.let { importModel(it) }
        }
    }

    // Android 11+ (API 30+): "All files access" is a special permission
    // granted from a Settings screen, not a runtime dialog.
    private val requestAllFilesAccess = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasStorageAccess()) launchPicker() else toastNoAccess()
    }

    // Android 10 and below: classic runtime permission.
    private val requestLegacyStorage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchPicker() else toastNoAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = PaintGLSurfaceView(this)
        val root = FrameLayout(this).apply { addView(glView) }

        val importButton = Button(this).apply {
            text = "Import Model"
            setOnClickListener { onImportClicked() }
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 48
        }
        root.addView(importButton, params)

        setContentView(root)
    }

    private fun onImportClicked() {
        if (hasStorageAccess()) launchPicker() else requestStorageAccess()
    }

    private fun hasStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Toast.makeText(
                this,
                "Grant \"Allow access to manage all files\" so companion textures next to a model can be found",
                Toast.LENGTH_LONG
            ).show()
            try {
                requestAllFilesAccess.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                // Some OEM builds don't resolve the per-app variant; fall back
                // to the general "All files access" management screen.
                requestAllFilesAccess.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            requestLegacyStorage.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun toastNoAccess() {
        Toast.makeText(this, "Storage access is required to import models", Toast.LENGTH_LONG).show()
    }

    private fun launchPicker() {
        pickModel.launch(Intent(this, FilePickerActivity::class.java))
    }

    private fun importModel(path: String) {
        thread {
            try {
                val mesh = loadMeshFromFile(path)
                    ?: throw IllegalStateException("assimp failed to parse $path")
                glView.submitMesh(mesh)
                runOnUiThread { Toast.makeText(this, "Loaded ${File(path).name}", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onPause() { super.onPause(); glView.onPause() }
    override fun onResume() { super.onResume(); glView.onResume() }
}
