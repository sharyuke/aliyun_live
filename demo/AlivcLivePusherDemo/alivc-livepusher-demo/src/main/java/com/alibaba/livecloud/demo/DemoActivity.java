package com.alibaba.livecloud.demo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.alibaba.livecloud.live.AlivcMediaFormat;
import com.alibaba.livecloud.utils.ToastUtils;
import com.alivc.live.pusher.demo.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DemoActivity extends Activity implements View.OnClickListener,RadioGroup.OnCheckedChangeListener{
    private EditText urlET;
    private Button connectBT;
    private RadioGroup resolutionCB;
    private RadioButton resolution240button;
    private RadioButton resolution360button;
    private RadioButton resolution480button;
    private RadioButton resolution540button;
    private RadioButton resolution720button;
    private RadioGroup rotationGroup;
    private RadioButton screenOrientation1;
    private RadioButton screenOrientation2;
    private CheckBox frontCameraMirror;
    private EditText mEtMaxBitrate;
    private EditText mEtMinBitrate;
    private EditText mEtBestBitrate;
    private EditText mEtInitialBitrate;
    private EditText mEtFrameRate;

    private EditText watermarkET;
    private EditText dxET;
    private EditText dyET;
    private EditText siteET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        connectBT = (Button) findViewById(R.id.connectBT);
        connectBT.setOnClickListener(this);
        urlET = (EditText) findViewById(R.id.rtmpUrl);
        resolutionCB = (RadioGroup) findViewById(R.id.resolution_group);
        resolution240button = (RadioButton) findViewById(R.id.radiobutton0);
        resolution360button = (RadioButton) findViewById(R.id.radiobutton1);
        resolution480button = (RadioButton) findViewById(R.id.radiobutton2);
        resolution540button = (RadioButton) findViewById(R.id.radiobutton3);
        resolution720button = (RadioButton) findViewById(R.id.radiobutton4);
        rotationGroup =(RadioGroup)findViewById(R.id.rotation_group);
        screenOrientation1 = (RadioButton) findViewById(R.id.screenOrientation1);
        screenOrientation2 = (RadioButton) findViewById(R.id.screenOrientation2);
        frontCameraMirror = (CheckBox) findViewById(R.id.front_camera_mirror);
        resolutionCB.setOnCheckedChangeListener(this);
        rotationGroup.setOnCheckedChangeListener(this);
        mEtBestBitrate = (EditText) findViewById(R.id.et_best_bitrate);
        mEtMaxBitrate = (EditText) findViewById(R.id.et_max_bitrate);
        mEtMinBitrate = (EditText) findViewById(R.id.et_min_bitrate);
        mEtInitialBitrate = (EditText) findViewById(R.id.et_init_bitrate);
        mEtFrameRate = (EditText) findViewById(R.id.et_frame_rate);


        watermarkET = (EditText) findViewById(R.id.watermark_path);
        watermarkET.setText("/sdcard/watermark.png");
        dxET = (EditText) findViewById(R.id.dx);
        dyET = (EditText) findViewById(R.id.dy);
        siteET = (EditText) findViewById(R.id.site);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }
    private BitmapFactory.Options loadWatermarkInfo(String watermark) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        if (watermark.startsWith("/")) {
            BitmapFactory.decodeFile(watermark, options);
        } else {
            try {
                InputStream watermarkStrream;
                AssetManager asset = getAssets();
                if (watermark.startsWith("assets://")) {
                    watermarkStrream = asset.open(watermark.replace("assets://", ""));
                } else {
                    watermarkStrream = asset.open(watermark);
                }
                BitmapFactory.decodeStream(watermarkStrream, null, options);
            } catch (FileNotFoundException e) {
                ToastUtils.showToast(DemoActivity.this,
                        "can not find watermark file with path '" + watermark + "'");
                return null;
            } catch (IOException e) {
                ToastUtils.showToast(DemoActivity.this,
                        "can not find watermark file with path '" + watermark + "'");
                return null;            }
        }
        return options;
    }

    @Override
    public void onClick(View v) {
        if(R.id.connectBT == v.getId()) {
            int videoResolution = 0;
            int cameraFrontFacing = 0;
            boolean screenOrientation = false;
            if(resolution240button.isChecked()){
                videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_240P;
            }else if (resolution360button.isChecked()) {
                videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_360P;
            } else if (resolution480button.isChecked()) {
                videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_480P;
            } else if (resolution540button.isChecked()) {
                videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_540P;
            } else if (resolution720button.isChecked()) {
                videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_720P;
            }

            if(frontCameraMirror.isChecked()){
                cameraFrontFacing = AlivcMediaFormat.CAMERA_FACING_FRONT;
            }else {
                cameraFrontFacing = AlivcMediaFormat.CAMERA_FACING_BACK;
            }

            if (screenOrientation1.isChecked()){
                screenOrientation = true;
            }else {
                screenOrientation = false;
            }

            if(TextUtils.isEmpty(urlET.getText())){
                ToastUtils.showToast(v.getContext(),"Push url is null");
                return;
            }

            String watermark = watermarkET.getText().toString();
            int dx = dxET.getText().toString().isEmpty() ? 14 : Integer.parseInt(dxET.getText().toString());
            int dy = dyET.getText().toString().isEmpty() ? 14 : Integer.parseInt(dyET.getText().toString());
            int site = siteET.getText().toString().isEmpty() ? 1 : Integer.parseInt(siteET.getText().toString());
            int minBitrate = 500;
            int maxBitrate = 800;
            int bestBitrate = 600;
            int initBitrate = 600;
            int watermarkWidth = 50;
            int watermarkHeight;
            BitmapFactory.Options options = loadWatermarkInfo(watermark);
            if(options == null) {
                return;
            }
            watermarkHeight = (int)(watermarkWidth * 1.0f * options.outHeight / options.outWidth);
            int frameRate = TextUtils.isEmpty(mEtFrameRate.getText().toString())?
                    30:Integer.parseInt(mEtFrameRate.getText().toString());
            try{
                minBitrate = Integer.parseInt(mEtMinBitrate.getText().toString());
            }catch (NumberFormatException e) {
            }

            try{
                maxBitrate = Integer.parseInt(mEtMaxBitrate.getText().toString());
            }catch(NumberFormatException e){}

            try {
                bestBitrate = Integer.parseInt(mEtBestBitrate.getText().toString());
            }catch (NumberFormatException e){}

            try {
                initBitrate = Integer.parseInt(mEtInitialBitrate.getText().toString());
            }catch(NumberFormatException e){}

            LiveCameraActivity.RequestBuilder builder = new LiveCameraActivity.RequestBuilder()
                    .bestBitrate(bestBitrate)
                    .cameraFacing(cameraFrontFacing)
                    .dx(dx)
                    .dy(dy)
                    .site(site)
                    .rtmpUrl(urlET.getText().toString())
                    .videoResolution(videoResolution)
                    .portrait(screenOrientation)
                    .watermarkUrl(watermark)
                    .minBitrate(minBitrate)
                    .maxBitrate(maxBitrate)
                    .frameRate(frameRate)
                    .watermarkWidth(watermarkWidth)
                    .watermarkHeight(watermarkHeight)
                    .initBitrate(initBitrate);

            LiveCameraActivity.startActivity(v.getContext(), builder);
        }
    }
}
