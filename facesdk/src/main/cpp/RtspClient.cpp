//
// Created by Han,Buhe on 2017/4/27.
//

#include "RtspClient.hpp"

const char *TAG = "RTSP";

void RtspClient::start(const char *path) {

    if (this->receiverThread) {
        this->_restart = true;
        this->receiverThread->join();
        delete receiverThread;
    }
    this->_restart = false;
    this->stop = false;
    bool hardwareDecode = true;
    if (this->javaVM) {
        av_jni_set_java_vm(this->javaVM, nullptr);
        log(LOG_PRIORITY_ERROR, TAG, "setting jni");
    }

    startCallbackThread();

    this->receiverThread = new std::thread([this, path, hardwareDecode]() {
        AVFormatContext *pFormatCtx = avformat_alloc_context();
        AVDictionary *opts = nullptr;
        log(LOG_PRIORITY_ERROR, TAG, "trying to open: %s.\n", path);
        av_dict_set_int(&opts, "stimeout", 3000000, 0);
        log(LOG_PRIORITY_ERROR, "%", "dict %p",opts);
        if ((avformat_open_input(&pFormatCtx, path, nullptr, &opts) < 0)) {
            av_dict_free(&opts);
            log(LOG_PRIORITY_ERROR, "%", "could not open stream");
            notifyError(ERROR_STREAM_OPEN_FAIL, "could not open stream");
            return;
        }
        if (opts) {
            av_dict_free(&opts);
        }

        if (avformat_find_stream_info(pFormatCtx, nullptr) < 0) {
            string msg = "Couldn't find stream information.\n";
            log(LOG_PRIORITY_ERROR, TAG, "%s", msg.c_str());
            notifyError(ERROR_STREAM_OPEN_FAIL, msg);
            avformat_close_input(&pFormatCtx);
            return;
        }

        int videoIndex = -1;
        for (int i = 0; i < pFormatCtx->nb_streams; i++) {
            if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                videoIndex = i;
                break;
            }
        }
        if (videoIndex == -1) {
            const char msg[] = "Didn't find a video stream.\n";
            log(LOG_PRIORITY_ERROR, TAG, msg);
            notifyError(ERROR_STREAM_OPEN_FAIL, "Didn't find a video stream.\n");
            avformat_close_input(&pFormatCtx);
            return;
        }

        AVCodecParameters *pCodecPar = pFormatCtx->streams[videoIndex]->codecpar;
        AVCodec *pCodec = nullptr;


        AVCodec *cdc = av_codec_next(nullptr);
        while (cdc != nullptr) {
            log(LOG_PRIORITY_ERROR, TAG, "cdc:%s", cdc->long_name);
            cdc = av_codec_next(cdc);

        }
        if (hardwareDecode && pCodecPar->codec_id == AV_CODEC_ID_H264) {//
            pCodec = avcodec_find_decoder_by_name("h264_mediacodec");//hevc_mediacodec
            log(LOG_PRIORITY_ERROR, TAG, "trying use mediacodec decoder for h264.\n");
        } else if (hardwareDecode && pCodecPar->codec_id == AV_CODEC_ID_HEVC) {
            pCodec = avcodec_find_decoder_by_name("hevc_mediacodec");
            log(LOG_PRIORITY_ERROR, TAG, "trying use mediacodec decoder for hevc.\n");
        }

        log(LOG_PRIORITY_ERROR, TAG, "found decode:%p", pCodec);
        if (pCodec == nullptr) {
            log(LOG_PRIORITY_ERROR, TAG, "using default decoder.\n");
            pCodec = avcodec_find_decoder(pCodecPar->codec_id);
        }

        if (pCodec == nullptr) {
            const char msg[] = "Codec not found.\n";
            log(LOG_PRIORITY_ERROR, TAG, msg);
            notifyError(ERROR_CODEC, msg);
            avformat_close_input(&pFormatCtx);
            return;
        }

        log(LOG_PRIORITY_ERROR, TAG, "using decoder:%s.\n", pCodec->long_name);
        log(LOG_PRIORITY_ERROR, TAG, "using decoder:%s.\n", pCodec->name);
        AVCodecContext *pCodecContext = avcodec_alloc_context3(pCodec);
        /* Copy codec parameters from input stream to output codec context */
        if ((avcodec_parameters_to_context(pCodecContext, pCodecPar)) < 0) {
            const char msg[] = "Failed to copy codec parameters to decoder context\n";
            log(LOG_PRIORITY_ERROR, TAG, msg);
            notifyError(ERROR_CODEC, msg);
            avformat_close_input(&pFormatCtx);
            avcodec_free_context(&pCodecContext);
            return;
        }

        if ((avcodec_open2(pCodecContext, pCodec, nullptr)) < 0) {
            const char msg[] = "Could not open codec(%d).\n";
            notifyError(ERROR_CODEC, msg);
            avformat_close_input(&pFormatCtx);
            avcodec_free_context(&pCodecContext);
            return;
        }

        int ret = 0;

        struct AVFrame *pFrame = av_frame_alloc();
        av_dump_format(pFormatCtx, 0, path, 0);
        AVPacket *pPacket = (AVPacket *) av_malloc(sizeof(AVPacket));

        while (!_restart && !stop) {
            log(LOG_PRIORITY_ERROR, TAG, "in loop");
            if (av_read_frame(pFormatCtx, pPacket) >= 0) {
                if (pPacket->stream_index == videoIndex) {
                    ret = avcodec_send_packet(pCodecContext, pPacket);
                    if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                        log(LOG_PRIORITY_ERROR, TAG, "send packet Error.\n");
                        continue;
                    }

                    ret = avcodec_receive_frame(pCodecContext, pFrame);
                    if (ret < 0 && ret != AVERROR_EOF && ret != AVERROR(EAGAIN)) {
                        log(LOG_PRIORITY_ERROR, TAG, "Decode Error.\n");
                        continue;
                    }
                    log(LOG_PRIORITY_ERROR, TAG, "ret:%d", ret);
                    if (ret == 0) {
                        this->state = STARTED;
                        handleFrame(pFrame);
                    } else {
                        log(LOG_PRIORITY_ERROR, TAG, "didn't decode one frame (%s)",
                            av_err2str(ret));
                    }
                }
                av_packet_unref(pPacket);
            } else {
                log(LOG_PRIORITY_ERROR, TAG, "invalid stream index");
                av_usleep(1000);
                av_packet_unref(pPacket);
                continue;
            }
        }

        log(LOG_PRIORITY_ERROR, TAG, "clean up");
        this->mutex->lock();
        if (this->decodedFrame) {
            av_frame_free(&this->decodedFrame);
            this->decodedFrame = nullptr;
        }
        this->mutex->unlock();
        this->condition->notify_all();

        // clean up;
        av_free(pPacket);
        av_frame_free(&pFrame);
        avcodec_close(pCodecContext);
        avcodec_free_context(&pCodecContext);
        avformat_close_input(&pFormatCtx);

        this->state = STOPPED;
        const char msg[] = "receiver thread exit.\n";
        log(LOG_PRIORITY_ERROR, TAG, msg);
        if (!this->_restart) {
            notifyError(RECEIVER_THREAD_EXIT, msg);
        }

        bool s = stop;
        bool r = _restart;
        log(LOG_PRIORITY_ERROR, TAG, "stop:%d,restart:%d", s, r);
//        delete receiverThread;
//        this->receiverThread = nullptr;
    });
}

RtspClient::RtspClient() {
    this->stop = false;
    this->state = INITIALIZED;
    this->mutex = new std::mutex();
    this->condition = new std::condition_variable();
    static bool ffmpegLibInitialized = false;
    if (!ffmpegLibInitialized) {
        av_register_all();
        avformat_network_init();
        ffmpegLibInitialized = true;
    }

}

void RtspClient::startCallbackThread() {
    this->callbackThread = new std::thread([this] {
        auto milliseconds = std::chrono::milliseconds(1);
        log(LOG_PRIORITY_ERROR, TAG, "callback thread start");
        while (!this->stop) {
            this->mutex->lock();
            if (!this->decodedFrame) {
                this->condition->wait_for((unique_lock<std::mutex> &) this->mutex, 10 * milliseconds);
            }
            AVFrame *frame = nullptr;
            if (this->decodedFrame) {
                log(LOG_PRIORITY_ERROR, TAG, "decodedFrame:%p", this->decodedFrame);
                frame = this->decodedFrame;
                this->decodedFrame = nullptr;
            }
            this->mutex->unlock();
            if (frame) {
                if (this->imageCallback) {
                    this->imageCallback(frame);
                }
                av_frame_free(&frame);
            }
        }
        this->callbackThreadExitCallback();
    });
}

void RtspClient::notifyError(int code, string msg) {
    if (this->errorCallback) {
        errorCallback(code, msg);
    }
}

void RtspClient::handleFrame(AVFrame *pFrame) {
    log(LOG_PRIORITY_ERROR, TAG, "handle Frame format:%i", pFrame->format);

    AVFrame *tempFrame = av_frame_alloc();
    tempFrame->width = pFrame->width;
    tempFrame->height = pFrame->height;
    tempFrame->format = AV_PIX_FMT_ARGB;
    if (av_frame_get_buffer(tempFrame, 32) < 0) {
        av_frame_free(&tempFrame);
        return;
    }

    switch (pFrame->format) {
        case AV_PIX_FMT_NV12:
            libyuv::NV12ToARGB(
                    (const uint8 *) pFrame->data[0], pFrame->linesize[0],
                    (const uint8 *) pFrame->data[1], pFrame->linesize[1],
                    tempFrame->data[0], tempFrame->linesize[0],
                    tempFrame->width, tempFrame->height
            );
            break;
        case AV_PIX_FMT_NV21:
            libyuv::NV21ToARGB(
                    (const uint8 *) pFrame->data[0], pFrame->linesize[0],
                    (const uint8 *) pFrame->data[1], pFrame->linesize[1],
                    tempFrame->data[0], tempFrame->linesize[0],
                    tempFrame->width, tempFrame->height
            );
            break;
        case AV_PIX_FMT_YUV420P:
            libyuv::I420ToARGB(
                    (const uint8 *) pFrame->data[0], pFrame->linesize[0],
                    (const uint8 *) pFrame->data[1], pFrame->linesize[1],
                    (const uint8 *) pFrame->data[2], pFrame->linesize[2],
                    tempFrame->data[0], tempFrame->linesize[0],
                    tempFrame->width, tempFrame->height
            );
        case AV_PIX_FMT_YUVJ420P:
            libyuv::J420ToARGB(
                    (const uint8 *) pFrame->data[0], pFrame->linesize[0],
                    (const uint8 *) pFrame->data[1], pFrame->linesize[1],
                    (const uint8 *) pFrame->data[2], pFrame->linesize[2],
                    tempFrame->data[0], tempFrame->linesize[0],
                    tempFrame->width, tempFrame->height
            );
            break;
        case AV_PIX_FMT_RGBA:
            libyuv::RGBAToARGB(
                    pFrame->data[0], pFrame->linesize[0],
                    tempFrame->data[0], tempFrame->linesize[0],
                    tempFrame->width, tempFrame->height
            );
        default:
            // TODO
            break;
    }
    this->mutex->lock();
    if (this->decodedFrame) {
        AVFrame *frame = this->decodedFrame;
        av_frame_free(&frame);
        this->decodedFrame = nullptr;
    }

    log(1, "", "tempFrame:%p", tempFrame);
    this->decodedFrame = tempFrame;
    this->mutex->unlock();
    this->condition->notify_all();
}

void RtspClient::setErrorCallback(function<void(int, string)> errorCallback) {
    this->errorCallback = errorCallback;
}

void RtspClient::setJavaVM(void *javaVM) {
    this->javaVM = javaVM;
}

void *RtspClient::getUserData() {
    return this->userData;
}

void RtspClient::setUserData(void *userdata) {
    this->userData = userdata;
}

void RtspClient::setImageCallback(function<void(AVFrame *frame)> imageCallback) {
    this->imageCallback = imageCallback;
}

void RtspClient::setLogCallback(LogCallbackFunction logCallback) {
    this->logCallback = logCallback;
}

void RtspClient::log(int priority, const char *tag, const char *fmt, ...) {
    if (this->logCallback) {
        __va_list args;
        char buffer[256];
        va_start (args, fmt);
        vsnprintf(buffer, 255, fmt, args);
        va_end(args);
        this->logCallback(priority, tag, buffer);
    }
}

RtspClient::~RtspClient() {
    stop = true;
//    callbackThread->join();
    if (receiverThread) {
//        receiverThread->join();
    }
    delete mutex;
    delete condition;
    delete receiverThread;
    delete callbackThread;
}

void RtspClient::setCallbackThreadExitCallback(function<void()> callback) {
    this->callbackThreadExitCallback = callback;
}

void RtspClient::stopReceiver() {
    log(LOG_PRIORITY_ERROR, TAG, " RtspClient::stopReceiver");
//    if (this->receiverThread) {
//        this->receiverThread->join();
//        delete this->receiverThread;
//    }
    this->receiverThread = nullptr;
    this->stop = true;
    this->_restart = false;
}

void RtspClient::restart() {
    this->_restart = true;
}

