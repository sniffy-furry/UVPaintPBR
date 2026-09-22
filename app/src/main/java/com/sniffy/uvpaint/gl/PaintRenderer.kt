package com.sniffy.uvpaint.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

private const val VERTEX_SHADER = """
#version 300 es
uniform mat4 uMVP;
uniform mat4 uModel;
layout(location=0) in vec3 aPosition;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
out vec3 vNormal;
out vec2 vUV;
void main() {
    vNormal = mat3(uModel) * aNormal;
    vUV = aUV;
    gl_Position = uMVP * vec4(aPosition, 1.0);
}
"""

// Checker pattern = a free UV-correctness test; real paint layers replace this later.
private const val FRAGMENT_SHADER = """
#version 300 es
precision mediump float;
in vec3 vNormal;
in vec2 vUV;
out vec4 fragColor;
void main() {
    vec3 n = normalize(vNormal);
    float ndotl = max(dot(n, normalize(vec3(0.4, 0.8, 0.6))), 0.15);
    float checker = mod(floor(vUV.x * 8.0) + floor(vUV.y * 8.0), 2.0);
    vec3 base = mix(vec3(0.85, 0.85, 0.9), vec3(0.25, 0.25, 0.3), checker);
    fragColor = vec4(base * ndotl, 1.0);
}
"""

private const val LOG_TAG = "uvpaint-gl"

/**
 * Draws the currently loaded mesh with a UV-checker shader, which doubles as
 * a visual smoke test for both the assimp import and the UV coordinates.
 * The brush/paint engine will swap the fragment shader's checker for the
 * composited PBR layers.
 */
class PaintRenderer : GLSurfaceView.Renderer {

    private var program = 0
    private val vao = IntArray(1)
    private val vbo = IntArray(1)
    private val ebo = IntArray(1)
    private var indexCount = 0
    private var aspect = 1f
    private var rotationDeg = 0f

    private val mvpMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    @Volatile private var pendingMesh: MeshData? = null

    /** Safe to call from any thread; the buffer upload happens on the GL thread. */
    fun setMesh(mesh: MeshData) {
        pendingMesh = mesh
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f, 0.12f, 0.14f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        program = ShaderUtil.buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glGenBuffers(1, ebo, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspect = width.toFloat() / max(height, 1)
    }

    override fun onDrawFrame(gl: GL10?) {
        pendingMesh?.let {
            // GLSurfaceView's render thread does not catch exceptions thrown
            // from here — an uncaught one takes the whole app down. Catching
            // it here turns a hard crash into a log line + a blank/unchanged
            // viewport, so a bad mesh degrades instead of killing the app.
            try {
                upload(it)
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "mesh upload failed", e)
                indexCount = 0
            }
            pendingMesh = null
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (indexCount == 0) return

        rotationDeg = (rotationDeg + 0.5f) % 360f

        Matrix.perspectiveM(projMatrix, 0, 45f, aspect, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3.2f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationDeg, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "uMVP"), 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "uModel"), 1, false, modelMatrix, 0)

        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    /** Centers and rescales the mesh so any import (regardless of source scale) frames nicely. */
    private fun normalized(vertexData: FloatArray): FloatArray {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var i = 0
        while (i < vertexData.size) {
            val x = vertexData[i]; val y = vertexData[i + 1]; val z = vertexData[i + 2]
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
            i += 8
        }
        val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f; val cz = (minZ + maxZ) / 2f
        val extent = max(max(maxX - minX, maxY - minY), max(maxZ - minZ, 0.0001f))
        val scale = 1.6f / extent
        val out = vertexData.copyOf()
        i = 0
        while (i < out.size) {
            out[i] = (out[i] - cx) * scale
            out[i + 1] = (out[i + 1] - cy) * scale
            out[i + 2] = (out[i + 2] - cz) * scale
            i += 8
        }
        return out
    }

    private fun upload(mesh: MeshData) {
        val verts = normalized(mesh.vertexData)
        val vBuf: FloatBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts).apply { position(0) }
        val iBuf: IntBuffer = ByteBuffer.allocateDirect(mesh.indices.size * 4)
            .order(ByteOrder.nativeOrder()).asIntBuffer().put(mesh.indices).apply { position(0) }

        GLES30.glBindVertexArray(vao[0])

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, vBuf, GLES30.GL_STATIC_DRAW)

        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 4, iBuf, GLES30.GL_STATIC_DRAW)

        GLES30.glBindVertexArray(0)
        indexCount = mesh.indices.size
        rotationDeg = 0f
    }
}
