package com.sniffy.uvpaint.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Placeholder renderer proving the OpenGL ES 3.0 pipeline builds and runs.
 *
 * Replaced step by step by:
 *  - mesh + UV import (native-lib.cpp, via assimp: OBJ/glTF/FBX)
 *  - UV editor overlay (seam marking, LSCM-based Smart UV unwrap)
 *  - layered PBR brush painting (base color / normal / roughness / metallic / AO / height)
 */
class PaintRenderer : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f, 0.12f, 0.14f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        // TODO: draw imported mesh with current UV layout + active paint layer
    }

    companion object {
        init {
            System.loadLibrary("uvpaint")
        }
    }

    external fun nativePing(): String
}
