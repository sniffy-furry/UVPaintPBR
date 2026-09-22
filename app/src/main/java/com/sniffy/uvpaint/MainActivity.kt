package com.sniffy.uvpaint

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.sniffy.uvpaint.gl.PaintGLSurfaceView
import com.sniffy.uvpaint.gl.loadMeshFromFile
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var glView: PaintGLSurfaceView

    private val pickModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importModel(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = PaintGLSurfaceView(this)
        val root = FrameLayout(this).apply { addView(glView) }

        val importButton = Button(this).apply {
            text = "Import Model"
            setOnClickListener { pickModel.launch(arrayOf("*/*")) }
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

    private fun importModel(uri: Uri) {
        thread {
            try {
                val name = queryDisplayName(uri) ?: "model"
                val ext = name.substringAfterLast('.', "").ifEmpty { "obj" }
                val dest = File(cacheDir, "import.$ext")
                contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("could not open picked file")

                val mesh = loadMeshFromFile(dest.absolutePath)
                    ?: throw IllegalStateException("assimp failed to parse $name")
                glView.submitMesh(mesh)
                runOnUiThread { Toast.makeText(this, "Loaded $name", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return uri.lastPathSegment
    }

    override fun onPause() { super.onPause(); glView.onPause() }
    override fun onResume() { super.onResume(); glView.onResume() }
}
