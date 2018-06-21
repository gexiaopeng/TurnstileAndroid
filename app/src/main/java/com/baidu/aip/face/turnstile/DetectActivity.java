/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import com.baidu.aip.FaceDetector;
import com.baidu.aip.ImageFrame;
import com.baidu.aip.face.CameraImageSource;
import com.baidu.aip.face.FaceDetectManager;
import com.baidu.aip.face.FaceFilter;
import com.baidu.aip.face.PreviewView;
import com.baidu.aip.face.camera.CameraView;
import com.baidu.aip.face.camera.ICameraControl;
import com.baidu.aip.face.camera.PermissionCallback;
import com.shinesun.face.R;
import com.baidu.aip.face.turnstile.exception.FaceError;
import com.baidu.aip.face.turnstile.model.FaceModel;
import com.baidu.aip.face.turnstile.model.RegResult;
import com.baidu.aip.face.turnstile.utils.ImageUtil;
import com.baidu.aip.face.turnstile.utils.OnResultListener;
import com.baidu.idl.facesdk.FaceInfo;
import com.shinesun.face.R;

import android.Manifest;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DetectActivity extends AppCompatActivity {

    private TextView nameTextView;
    // 预览View;
    private PreviewView previewView;
    // textureView用于绘制人脸框等。
    private TextureView textureView;
    // 用于检测人脸。
    private FaceDetectManager faceDetectManager;

    // 为了方便调式。
    private ImageView testView;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detected);
        faceDetectManager = new FaceDetectManager(getApplicationContext());
        testView = (ImageView) findViewById(R.id.test_view);
        nameTextView = (TextView) findViewById(R.id.name_text_view);
        previewView = (PreviewView) findViewById(R.id.preview_view);
        textureView = (TextureView) findViewById(R.id.texture_view);

        // 从系统相机获取图片帧。
        final CameraImageSource cameraImageSource = new CameraImageSource(this);
        // 图片越小检测速度越快，闸机场景640 * 480 可以满足需求。实际预览值可能和该值不同。和相机所支持的预览尺寸有关。
        // 可以通过 camera.getParameters().getSupportedPreviewSizes()查看支持列表。
        cameraImageSource.getCameraControl().setPreferredPreviewSize(1280, 720);

        // 设置预览
        cameraImageSource.setPreviewView(previewView);
        // 设置图片源
        faceDetectManager.setImageSource(cameraImageSource);
        // 设置人脸过滤角度，角度越小，人脸越正，比对时分数越高
        faceDetectManager.getFaceFilter().setAngle(20);
        // 设置回调，回调人脸检测结果。
        faceDetectManager.setOnFaceDetectListener(new FaceDetectManager.OnFaceDetectListener() {
            @Override
            public void onDetectFace(int retCode, FaceInfo[] infos, ImageFrame frame) {
                // TODO 显示检测的图片。用于调试，如果人脸sdk检测的人脸需要朝上，可以通过该图片判断
                final Bitmap bitmap =
                        Bitmap.createBitmap(frame.getArgb(), frame.getWidth(), frame.getHeight(), Bitmap.Config
                                .ARGB_8888);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        testView.setImageBitmap(bitmap);
                    }
                });
                if (infos == null) {
                    // null表示，没有人脸。
                    showFrame(null);
                    shouldUpload = true;
                }
            }
        });
        // 人脸追踪回调。没有人脸时不会回调。
        faceDetectManager.setOnTrackListener(new FaceFilter.OnTrackListener() {
            @Override
            public void onTrack(FaceFilter.TrackedModel trackedModel) {
                showFrame(trackedModel);
                if (trackedModel.meetCriteria()) {
                    // 该帧符合过虑标准，人脸质量较高。上传至服务器，进行识别
                    upload(trackedModel);
                }
            }
        });

        // 安卓6.0+ 运行时，权限回调。
        cameraImageSource.getCameraControl().setPermissionCallback(new PermissionCallback() {
            @Override
            public boolean onRequestPermission() {
                ActivityCompat.requestPermissions(DetectActivity.this,
                        new String[]{Manifest.permission.CAMERA}, 100);
                return true;
            }
        });

        textureView.setOpaque(false);

        // 不需要屏幕自动变黑。
        textureView.setKeepScreenOn(true);

        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) {
            // previewView.setScaleType(PreviewView.ScaleType.FIT_WIDTH);
            // 相机坚屏模式
            cameraImageSource.getCameraControl().setDisplayOrientation(CameraView.ORIENTATION_PORTRAIT);
        } else {
            // previewView.setScaleType(PreviewView.ScaleType.FIT_WIDTH);
            // 相机横屏模式
            cameraImageSource.getCameraControl().setDisplayOrientation(CameraView.ORIENTATION_HORIZONTAL);
        }

        setCameraType(cameraImageSource);
    }

    private void setCameraType(CameraImageSource cameraImageSource) {
        // TODO 选择使用前置摄像头
        cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_FACING_FRONT);

        // TODO 选择使用后置摄像头
//         cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_FACING_BACK);
//         previewView.setMirrored(false);

        // TODO 选择使用usb摄像头 如果不设置，人脸框会镜像，显示不准
        //  cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_USB);
        //  previewView.setMirrored(false);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // 开始检测
        faceDetectManager.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 结束检测。
        faceDetectManager.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetectManager.stop();
    }

    // 屏幕上显示用户信息。
    private void showUserInfo(String userInfo, float score) {

        // 把userInfo和分数显示在屏幕上
        String text = String.format(Locale.ENGLISH, "%s  %.2f", userInfo, score);
        nameTextView.setText(text);
    }

    private boolean shouldUpload = true;

    // 上传一帧至服务器进行，人脸识别。
    private void upload(FaceFilter.TrackedModel model) {
        if (model.getEvent() != FaceFilter.Event.OnLeave) {
            if (!shouldUpload) {
                return;
            }
            shouldUpload = false;
            final Bitmap face = model.cropFace();
            try {
                final File file = File.createTempFile(UUID.randomUUID().toString() + "", ".jpg");
                // 人脸识别不需要整张图片。可以对人脸区别进行裁剪。减少流量消耗和，网络传输占用的时间消耗。
                ImageUtil.resize(face, file, 200, 200);
                APIService.getInstance().identify(new OnResultListener<RegResult>() {
                    @Override
                    public void onResult(RegResult result) {
                        if (file != null && file.exists()) {
                            file.delete();
                        }
                        if (result == null) {
                            return;
                        }

                        String res = result.getJsonRes();
                        double maxScore = 0;
                        String userId = "";
                        String userInfo = "";
                        if (TextUtils.isEmpty(res)) {
                            return;
                        }
                        JSONObject obj = null;
                        try {
                            obj = new JSONObject(res);
                            JSONObject resObj = obj.optJSONObject("result");
                            if (resObj != null) {
                                JSONArray resArray = resObj.optJSONArray("user_list");
                                int size = resArray.length();


                                for (int i = 0; i < size; i++) {
                                    JSONObject s = (JSONObject) resArray.get(i);
                                    if (s != null) {
                                        double score = s.getDouble("score");
                                        if (score > maxScore) {
                                            maxScore = score;
                                            userId = s.getString("user_id");
                                            userInfo = s.getString("user_info");
                                        }

                                    }
                                }

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        float score = 0;
                        // 识别分数小于80，也可能是角度不好。可以选择重试。
                        if (score < 80) {
                            shouldUpload = true;
                        }
                        showUserInfo(userInfo, (float) maxScore);
                    }

                    @Override
                    public void onError(FaceError error) {
                        error.printStackTrace();
                        shouldUpload = true;
                        if (file != null && file.exists()) {
                            file.delete();
                        }
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
        paint.setTextSize(30);
    }

    RectF rectF = new RectF();

    /**
     * 绘制人脸框。
     *
     * @param model 追踪到的人脸
     */
    private void showFrame(FaceFilter.TrackedModel model) {
        Canvas canvas = textureView.lockCanvas();
        if (canvas == null) {
            return;
        }
        // 清空canvas
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (model != null) {
            model.getImageFrame().retain();
            rectF.set(model.getFaceRect());

            // 检测图片的坐标和显示的坐标不一样，需要转换。
            previewView.mapFromOriginalRect(rectF);
            if (model.meetCriteria()) {
                // 符合检测要求，绘制绿框
                paint.setColor(Color.GREEN);
            } else {
                // 不符合要求，绘制黄框
                paint.setColor(Color.YELLOW);

                String text = "请正视屏幕";
                float width = paint.measureText(text) + 50;
                float x = rectF.centerX() - width / 2;
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(text, x + 25, rectF.top - 20, paint);
                paint.setColor(Color.YELLOW);
            }
            paint.setStyle(Paint.Style.STROKE);
            // 绘制框
            canvas.drawRect(rectF, paint);
        }
        textureView.unlockCanvasAndPost(canvas);
    }

}
