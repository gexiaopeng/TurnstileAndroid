/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.baidu.aip.ImageFrame;
import com.baidu.aip.face.FaceCropper;
import com.baidu.aip.face.FaceDetectManager;
import com.baidu.aip.face.FileImageSource;
import com.shinesun.face.R;
import com.baidu.aip.face.turnstile.exception.FaceError;
import com.baidu.aip.face.turnstile.model.FaceVerifyResult;
import com.baidu.aip.face.turnstile.model.RegResult;
import com.baidu.aip.face.turnstile.utils.ImageUtil;
import com.baidu.aip.face.turnstile.utils.OnResultListener;
import com.baidu.idl.facesdk.FaceInfo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 该类提供人脸注册功能，注册的人脸可以通个自动检测和选自相册两种方式获取。
 */

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_PICK_IMAGE = 1000;
    private static final int REQUEST_CODE_AUTO_DETECT = 100;
    private EditText usernameEditText;
    private ImageView avatarImageView;
    private Button autoDetectButton;
    private Button fromAlbumButton;
    private Button submitButton;

    // 注册时使用人脸图片路径。
    private String faceImagePath;

    // 从相机识别时使用。
    private FaceDetectManager detectManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detectManager = new FaceDetectManager(getApplicationContext());
        setContentView(R.layout.activity_reg);

        usernameEditText = (EditText) findViewById(R.id.username_et);
        avatarImageView = (ImageView) findViewById(R.id.avatar_iv);
        autoDetectButton = (Button) findViewById(R.id.auto_detect_btn);
        fromAlbumButton = (Button) findViewById(R.id.pick_from_album_btn);
        submitButton = (Button) findViewById(R.id.submit_btn);

        autoDetectButton.setOnClickListener(this);
        fromAlbumButton.setOnClickListener(this);
        submitButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == autoDetectButton) {
            Intent intent = new Intent(RegisterActivity.this, RegDetectActivity.class);
            startActivityForResult(intent, REQUEST_CODE_AUTO_DETECT);
        } else if (v == fromAlbumButton) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);

        } else if (v == submitButton && !TextUtils.isEmpty(faceImagePath)) {
            // 人脸注册和1：n属于在线接口，需要通过ak，sk获得token后进行调用，此方法为获取token，为了防止你得ak，sk泄露，
            // 建议把此调用放在您的服务端
            register(faceImagePath);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AUTO_DETECT && data != null) {
            faceImagePath = data.getStringExtra("file_path");

            Bitmap bitmap = BitmapFactory.decodeFile(faceImagePath);
            avatarImageView.setImageBitmap(bitmap);

        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = getRealPathFromURI(uri);
            Bitmap btimap = BitmapFactory.decodeFile(path);
            avatarImageView.setImageBitmap(btimap);
            try {
                File file = File.createTempFile(UUID.randomUUID().toString() + "", ".jpg");
                // 压缩人脸图片至300 * 300，减少网络传输时间
                ImageUtil.resize(btimap, file, 300, 300);
                RegisterActivity.this.faceImagePath = file.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void register(String filePath) {

        final String username = usernameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(RegisterActivity.this, "姓名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        /*
         * 用户id（由数字、字母、下划线组成），长度限制128B
         * uid为用户的id,百度对uid不做限制和处理，应该与您的帐号系统中的用户id对应。
         *
         */
        final String uid = UUID.randomUUID().toString().substring(0, 8) + "_123";
        //        String uid = 修改为自己用户系统中用户的id;

        final File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(RegisterActivity.this, "文件不存在", Toast.LENGTH_LONG).show();
            return;
        }

//        APIService.getInstance().verify(new OnResultListener<FaceVerifyResult>() {
//            @Override
//            public void onResult(FaceVerifyResult result) {
//                final double livenss = result.getLivenss();
//                if (livenss > 0.834963) {
        APIService.getInstance().reg(new OnResultListener<RegResult>() {
            @Override
            public void onResult(RegResult result) {
                Log.i("wtf", "orientation->" + result.getJsonRes());
                toast("注册成功！");
                startResultActivity(1, username);
            }

            @Override
            public void onError(FaceError error) {
                toast("注册失败");
                startResultActivity(1, null);
            }
        }, file, uid, username);
//                } else {
//                    toast("注册失败,活体未通过");
//                    startResultActivity(livenss, null);
//                }
//
//            }
//
//            @Override
//            public void onError(FaceError error) {
//                toast("注册失败");
//            }
//        }, file);
    }

    private void startResultActivity(double livenessValue, String username) {
        Intent intent = new Intent(RegisterActivity.this, RegisterResultActivity.class);
        intent.putExtra(RegisterResultActivity.KEY_USER_NAME, username);
//        intent.putExtra(RegisterResultActivity.KEY_LIVENESS_VALUE, livenessValue);
        startActivity(intent);
    }

    private void toast(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RegisterActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Handler handler = new Handler(Looper.getMainLooper());
}
