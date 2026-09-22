package com.sniffy.uvpaint.gl

object NativeMesh {
    init { System.loadLibrary("uvpaint") }

    external fun nativeLoadModel(path: String): Long
    external fun nativeGetVertexData(handle: Long): FloatArray
    external fun nativeGetIndexData(handle: Long): IntArray
    external fun nativeFree(handle: Long)
}

/** Interleaved position(3) + normal(3) + uv(2) per vertex. */
data class MeshData(val vertexData: FloatArray, val indices: IntArray)

/**
 * Loads OBJ / glTF / GLB / FBX geometry + UVs via assimp.
 * Note: only the single picked file is read — external companions (OBJ .mtl,
 * loose glTF .bin/texture files) aren't resolved yet. Self-contained formats
 * (.glb, .fbx, .obj without materials) work fully; multi-file glTF needs the
 * whole folder imported, which is a later step.
 */
fun loadMeshFromFile(path: String): MeshData? {
    val handle = NativeMesh.nativeLoadModel(path)
    if (handle == 0L) return null
    val verts = NativeMesh.nativeGetVertexData(handle)
    val idx = NativeMesh.nativeGetIndexData(handle)
    NativeMesh.nativeFree(handle)
    return MeshData(verts, idx)
}
