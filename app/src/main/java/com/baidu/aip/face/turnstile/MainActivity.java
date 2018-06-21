/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile;

import com.shinesun.face.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button registerButton;
    private Button detectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerButton = (Button) findViewById(R.id.register_button);
        detectedButton = (Button) findViewById(R.id.detect_button);
        addListener();
    }

    private void addListener() {
        registerButton.setOnClickListener(this);
        detectedButton.setOnClickListener(this);
        findViewById(R.id.rtsp_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (registerButton == v) {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        } else if (detectedButton == v) {
            Intent intent = new Intent(MainActivity.this, DetectActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "如果需要使用网络摄像头，请开启facesdk->build.gradle->externalNativeBuild编译，"
                            + "会生成rstp.so,使用rtsp接受网络视频流，使用ffmpge解码视频帧", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(MainActivity.this, RtspTestActivity.class);
            startActivity(intent);
        }
    }
}
