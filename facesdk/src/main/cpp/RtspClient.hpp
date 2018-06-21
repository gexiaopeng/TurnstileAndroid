//
// Created by Han,Buhe on 2017/4/27.
//

#ifndef FACEMEMBER_RTSPCLIENT_H
#define FACEMEMBER_RTSPCLIENT_H

#include <string>
#include <thread>
#include <mutex>
#include <atomic>
#include <condition_variable>


extern "C" {
#include <stdio.h>
#include <time.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h>
#include <libavutil/time.h>
#include <libavcodec/jni.h>
#include <libyuv/rotate.h>
#include <libyuv/convert_argb.h>
}

using namespace std;

typedef function<void(int priority, const char *tag, const char *fmt)> LogCallbackFunction;

class RtspClient {
public:

    enum LogPriority {
        LOG_PRIORITY_ERROR = 0,
    };

    enum RtspErrorCode {
        ERROR_STREAM_OPEN_FAIL = 10000,
        ERROR_CODEC = 20000,
        RECEIVER_THREAD_EXIT = 30000,
    };

    enum Status {
        INITIALIZED = 0,
        STARTED = 10,
        STOPPED = 20,
    };

    RtspClient();
    ~RtspClient();
    
    void setJavaVM(void *javaVM);

    void setUserData(void *userdata);

    void* getUserData();

    void start(const char *path);

    void stopReceiver();
    void restart();

    void setLogCallback(LogCallbackFunction logCallback);

    void setErrorCallback(function<void(int, string)> errorCallback);

    void setImageCallback(function<void(AVFrame *frame)> imageCallback);

    void setCallbackThreadExitCallback(function<void()> callback);

    void log(int priority, const char *tag, const char *fmt, ...);

private:
    void *javaVM = nullptr;
    void *userData = nullptr;

    thread *receiverThread = nullptr;
    thread *callbackThread = nullptr;

    std::mutex *mutex = nullptr;
    std::condition_variable *condition = nullptr;
    std::atomic_int state;

    LogCallbackFunction logCallback = nullptr;
    std::function<void(int code, string msg)> errorCallback = nullptr;
    std::function<void(AVFrame *frame)> imageCallback = nullptr;
    std::function<void()> callbackThreadExitCallback = nullptr;

    volatile atomic_bool stop;
    volatile atomic_bool _restart;

    struct AVFrame *decodedFrame = nullptr;

    void notifyError(int code, string msg);

    void handleFrame(AVFrame *frame);

    void startCallbackThread();
};

#endif //FACEMEMBER_RTSPCLIENT_H
