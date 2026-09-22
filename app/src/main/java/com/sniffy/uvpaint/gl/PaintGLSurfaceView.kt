package com.sniffy.uvpaint.gl

import android.content.Context
import android.opengl.GLSurfaceView

class PaintGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = PaintRenderer()

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /** Safe to call from any thread. */
    fun submitMesh(mesh: MeshData) {
        queueEvent { renderer.setMesh(mesh) }
    }
}
