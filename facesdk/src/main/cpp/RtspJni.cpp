//
// Created by Han,Buhe on 2017/4/28.
//

#include "RtspClient.hpp"

extern "C" {
#include <android/log.h>
#include <jni.h>
}

static const char *TAG = "rtsp_native";
static const char *RTSP_CLIENT_PATH = "com/baidu/aip/RtspClient";

JavaVM *gJavaVM = nullptr;
jmethodID gOnDataMethodID = nullptr;
jmethodID gOnErrorMethodID = nullptr;

thread_local JNIEnv *localEnv = nullptr;

static void doCallback(jobject ref, AVFrame *pFrame) {
    if (!pFrame) {
        return;
    }
    int width = pFrame->width;
    int height = pFrame->height;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "width:%d,height:%d", width, height);
    if (width <= 0 || height <= 0) {
        return;
    }

    if (!localEnv) {
        gJavaVM->AttachCurrentThread(&localEnv, nullptr);
    }

    //TODO cache;
    jintArray data = localEnv->NewIntArray(width * height);
    localEnv->SetIntArrayRegion(data, 0, width * height, (const jint *) pFrame->data[0]);
    localEnv->CallVoidMethod(ref, gOnDataMethodID, data, width, height);
    localEnv->DeleteLocalRef(data);
}

static jlong init(JNIEnv *env, jobject self) {
    jobject globalRef = env->NewGlobalRef(self);
    RtspClient *rtspClient = new RtspClient();
    rtspClient->setJavaVM(gJavaVM);
    rtspClient->setUserData(globalRef);
    rtspClient->setErrorCallback([globalRef](int code, string msg) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "ERROR:%s", msg.c_str());
        JNIEnv *jniEnv = nullptr;
        gJavaVM->AttachCurrentThread(&jniEnv, nullptr);
        const char *c_str = msg.c_str();
        jstring message = jniEnv->NewStringUTF(c_str);
        jniEnv->CallVoidMethod(globalRef, gOnErrorMethodID, code, message);
        jniEnv->DeleteLocalRef(message);
        gJavaVM->DetachCurrentThread();
    });
    rtspClient->setCallbackThreadExitCallback([](){
        gJavaVM->DetachCurrentThread();
    });
    rtspClient->setImageCallback([globalRef](AVFrame *frame) {
        doCallback(globalRef, frame);
    });
    rtspClient->setLogCallback([](int priority, const char *tag, const char *fmt) {
//        __android_log_print(ANDROID_LOG_WARN, tag, fmt, "");
    });
    return (jlong) rtspClient;
}

static void start(JNIEnv *env, jobject, jlong pointer, jstring url) {
    RtspClient *rtspClient = (RtspClient *) pointer;
    const char *str = env->GetStringUTFChars(url, nullptr);
    const char *path = strdup(str);
    env->ReleaseStringUTFChars(url, str);
    rtspClient->start(path);
}

static void restart(JNIEnv *env, jobject, jlong pointer) {
    RtspClient *rtspClient = (RtspClient *) pointer;
    rtspClient->restart();
}

static void stop(JNIEnv *env, jobject, jlong pointer) {
    RtspClient *rtspClient = (RtspClient *) pointer;
    rtspClient->stopReceiver();
    __android_log_print(ANDROID_LOG_ERROR, TAG, "stopReceiver");
}


static void release(JNIEnv *env, jobject, jlong pointer) {
    RtspClient *rtspClient = (RtspClient *) pointer;
//    jobject globalRef = (jobject) rtspClient->getUserData();
//    delete rtspClient;
//    env->DeleteGlobalRef(globalRef);
//    rtspClient->stopReceiver();
}

static jclass buildJNICache(JNIEnv *env) {
    jclass cls = env->FindClass(RTSP_CLIENT_PATH);
    if (!cls) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Cannot find RtspClient Class!\n");
    }
    gOnDataMethodID = env->GetMethodID(cls, "onData", "([III)V");
    gOnErrorMethodID = env->GetMethodID(cls, "onError", "(ILjava/lang/String;)V");
    return cls;
}

static JNINativeMethod methods[] = {
        {"nativeInit",    "()J",                    (void *) init},
        {"nativeStart",   "(JLjava/lang/String;)V", (void *) start},
        {"nativeRelease", "(J)V",                   (void *) release},
        {"nativeRestart", "(J)V",                   (void *) restart},
        {"nativeStop", "(J)V",                   (void *) stop},
};

static jboolean registerNatives(JNIEnv *env, jclass rtspClass) {
    if (env->RegisterNatives(rtspClass, methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_FALSE;
    }
    __android_log_print(ANDROID_LOG_INFO, TAG, "JNI_OnLoad returned.\n");
    return JNI_TRUE;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    gJavaVM = vm;
    jclass rtspClass = buildJNICache(env);

    jboolean registerResult = registerNatives(env, rtspClass);
    env->DeleteLocalRef(rtspClass);
    if (!registerResult) {
        return -1;
    }
    return JNI_VERSION_1_6;
}

