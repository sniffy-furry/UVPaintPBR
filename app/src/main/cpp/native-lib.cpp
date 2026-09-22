#include <jni.h>
#include <android/log.h>

#define LOG_TAG "uvpaint-native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_sniffy_uvpaint_gl_PaintRenderer_nativePing(JNIEnv *env, jobject /* this */) {
    LOGI("native pipeline stub alive");
    return env->NewStringUTF("uvpaint-native-ok");
}
