package com.sniffy.uvpaint.gl

import android.util.Log

private const val LOG_TAG = "uvpaint-gl"

object NativeMesh {
    init { System.loadLibrary("uvpaint") }

    external fun nativeLoadModel(path: String): Long
    external fun nativeGetVertexData(handle: Long): FloatArray?
    external fun nativeGetIndexData(handle: Long): IntArray?
    external fun nativeFree(handle: Long)
}

/** Interleaved position(3) + normal(3) + uv(2) per vertex. */
data class MeshData(val vertexData: FloatArray, val indices: IntArray)

/** Loads OBJ / glTF / GLB / FBX geometry + UVs via assimp, from a real filesystem path. */
fun loadMeshFromFile(path: String): MeshData? {
    val handle = NativeMesh.nativeLoadModel(path)
    if (handle == 0L) return null
    try {
        val verts = NativeMesh.nativeGetVertexData(handle)
        val idx = NativeMesh.nativeGetIndexData(handle)
        if (verts == null || idx == null) {
            Log.e(LOG_TAG, "$path parsed but produced no mesh data (empty scene?)")
            return null
        }
        return MeshData(verts, idx)
    } finally {
        NativeMesh.nativeFree(handle)
    }
}
