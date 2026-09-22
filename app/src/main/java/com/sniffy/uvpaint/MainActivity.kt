package com.sniffy.uvpaint

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sniffy.uvpaint.gl.PaintGLSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var glView: PaintGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = PaintGLSurfaceView(this)
        setContentView(glView)
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }
}
