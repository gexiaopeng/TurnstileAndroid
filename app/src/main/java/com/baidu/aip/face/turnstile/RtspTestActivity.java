/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import com.baidu.aip.ImageFrame;
import com.baidu.aip.face.FaceCropper;
import com.baidu.aip.face.FaceDetectManager;

import com.baidu.aip.face.RtspImageSource;
import com.baidu.aip.face.TexturePreviewView;
import com.shinesun.face.R;
import com.baidu.aip.face.turnstile.exception.FaceError;
import com.baidu.aip.face.turnstile.model.FaceModel;
import com.baidu.aip.face.turnstile.utils.ImageUtil;
import com.baidu.aip.face.turnstile.utils.OnResultListener;
import com.baidu.idl.facesdk.FaceInfo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.widget.TextView;

/**
 * 该Activity 展示了，如果使用网络摄像头(RTSP协议)进行人脸识别。
 * 需要有网络摄像头支持。以及rtsp 的本地so库。
 */
public class RtspTestActivity extends AppCompatActivity {

    private RtspImageSource rtspImageSource = new RtspImageSource();

    private TextView nameTextView;
    private TexturePreviewView previewView;
    private TextureView textureView;

    private FaceDetectManager faceDetectManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        faceDetectManager = new FaceDetectManager(getApplicationContext());
        setContentView(R.layout.activity_detected);
        previewView = (TexturePreviewView) findViewById(R.id.preview_view);
        textureView = (TextureView) findViewById(R.id.texture_view);

        // rtsp 的图像不是镜面的
        previewView.setMirrored(false);
        // 设置预览View用于预览
        rtspImageSource.setPreviewView(previewView);

        // rtsp网络摄像头地址。具体格式见，摄像头说明书。
        String url = String.format(Locale.ENGLISH,
                "rtsp://admin:Aa123456@%s:554/h264/ch1/main/av_stream", "192.168.1.65");
        rtspImageSource.setUrl(url);

        faceDetectManager.setImageSource(rtspImageSource);
        faceDetectManager.setOnFaceDetectListener(new FaceDetectManager.OnFaceDetectListener() {
            @Override
            public void onDetectFace(int retCode, FaceInfo[] infos, ImageFrame frame) {
                showFrame(frame.getArgb(),infos, frame.getWidth(), frame.getHeight());
                if (retCode == 0) {
                    upload(infos,frame.getArgb(),frame.getWidth());
                }
                if (infos == null) {
                    shouldUpload = true;
                }
            }
        });

        nameTextView = (TextView) findViewById(R.id.name_text_view);
        textureView.setOpaque(false);
        textureView.setKeepScreenOn(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        faceDetectManager.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        faceDetectManager.stop();
    }

    private void showUserInfo(FaceModel model) {
        if (model != null) {
            String text = String.format(Locale.ENGLISH,"%s%.2f",
                    model.getUserInfo(),model.getScore());
            nameTextView.setText(text);
        }
    }

    private boolean shouldUpload = true;

    private void upload(FaceInfo[] infos, int[] argb, int width) {
        if (infos != null) {
            if (!shouldUpload){
                return;
            }
            shouldUpload = false;
            // 截取人脸那部分图片。
            Bitmap face = FaceCropper.getFace(argb, infos[0], width);

            try {
                File file = File.createTempFile(UUID.randomUUID().toString() + "", ".jpg");
                // 压缩到200 * 200
                ImageUtil.resize(face, file, 200, 200);
                APIService.getInstance().identify(new OnResultListener<FaceModel>() {
                    @Override
                    public void onResult(FaceModel result) {
                        if (result == null) {
                            return;
                        }
                        if (result.getScore() < 80) {
                            shouldUpload = true;
                        }
                        showUserInfo(result);
                    }

                    @Override
                    public void onError(FaceError error) {
                        error.printStackTrace();
                        shouldUpload = true;
                    }
                }, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            shouldUpload = true;
        }
    }

    private Paint paint = new Paint();

    {
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
    }

    RectF rectF = new RectF();

    private void showFrame(int[] argbc, FaceInfo[] infos, int iwidth, int iheight) {
        Canvas canvas = textureView.lockCanvas();
        if (canvas == null) {
            return;
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (infos != null) {

            int[] points = new int[8];
            for (FaceInfo faceInfo : infos) {

                faceInfo.getRectPoints(points);

                int left = points[2];
                int top = points[3];
                int right = points[6];
                int bottom = points[7];
                //
                int width = right - left;
                int height = bottom - top;

                left = faceInfo.mCenter_x - width / 2;
                top = faceInfo.mCenter_y - height / 2;

                rectF.top = top;
                rectF.left = left;
                rectF.right = left + width;
                rectF.bottom = top + height;

                // 从原始图片坐标映射到显示坐标。
                previewView.mapFromOriginalRect(rectF);
                canvas.drawRect(rectF, paint);
            }
        }
        textureView.unlockCanvasAndPost(canvas);
    }
}
