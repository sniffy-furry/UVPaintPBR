#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "mesh_loader.h"

#define LOG_TAG "uvpaint-native"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_sniffy_uvpaint_gl_NativeMesh_nativeLoadModel(JNIEnv* env, jobject, jstring jpath) {
    const char* pathChars = env->GetStringUTFChars(jpath, nullptr);
    if (!pathChars) {
        LOGE("GetStringUTFChars failed (bad jstring or OOM)");
        return 0;
    }
    std::string path(pathChars);
    env->ReleaseStringUTFChars(jpath, pathChars);

    std::string error;
    MeshData* mesh = LoadMeshFromFile(path, &error);
    if (!mesh) {
        LOGE("model load failed for %s: %s", path.c_str(), error.c_str());
        return 0;
    }
    LOGI("loaded %s: %zu floats (verts), %zu indices",
         path.c_str(), mesh->vertexData.size(), mesh->indices.size());
    return reinterpret_cast<jlong>(mesh);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_sniffy_uvpaint_gl_NativeMesh_nativeGetVertexData(JNIEnv* env, jobject, jlong handle) {
    auto* mesh = reinterpret_cast<MeshData*>(handle);
    if (!mesh || mesh->vertexData.empty()) return nullptr;

    auto len = static_cast<jsize>(mesh->vertexData.size());
    jfloatArray arr = env->NewFloatArray(len);
    if (!arr) {
        LOGE("NewFloatArray(%d) failed", len);
        return nullptr;
    }
    env->SetFloatArrayRegion(arr, 0, len, mesh->vertexData.data());
    return arr;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_sniffy_uvpaint_gl_NativeMesh_nativeGetIndexData(JNIEnv* env, jobject, jlong handle) {
    auto* mesh = reinterpret_cast<MeshData*>(handle);
    if (!mesh || mesh->indices.empty()) return nullptr;

    std::vector<jint> idx(mesh->indices.begin(), mesh->indices.end());
    auto len = static_cast<jsize>(idx.size());
    jintArray arr = env->NewIntArray(len);
    if (!arr) {
        LOGE("NewIntArray(%d) failed", len);
        return nullptr;
    }
    env->SetIntArrayRegion(arr, 0, len, idx.data());
    return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_sniffy_uvpaint_gl_NativeMesh_nativeFree(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<MeshData*>(handle);
}
