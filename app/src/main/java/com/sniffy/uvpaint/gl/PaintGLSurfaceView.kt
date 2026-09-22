package com.sniffy.uvpaint.gl

import android.content.Context
import android.opengl.GLSurfaceView

class PaintGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: PaintRenderer

    init {
        setEGLContextClientVersion(3)
        renderer = PaintRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
