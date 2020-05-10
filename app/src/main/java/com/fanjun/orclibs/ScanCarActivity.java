package com.fanjun.orclibs;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import com.fanjun.orclibs.Utils.CameraUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import hotcard.doc.reader.NativeOcrPn;

/**
 * 识别车牌demo
 */
public class ScanCarActivity extends AppCompatActivity implements Camera.PreviewCallback, SurfaceHolder.Callback {
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    NativeOcrPn mScanCarApi;
    Camera mCamera;
    ImageView view2;
    boolean success;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_car);
        surfaceView = findViewById(R.id.mSurfaceView);
        view2 = findViewById(R.id.view2);
        CameraUtils.init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//禁止息屏
        this.surfaceHolder = this.surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
        this.surfaceHolder.setType(3);
        this.mScanCarApi = new NativeOcrPn(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 201:
                        success = true;
                        //识别结果
                        byte[] arrayOfByte = new byte[1024];
                        mScanCarApi.GetResult(arrayOfByte, arrayOfByte.length);
                        //保存图片路径
                        String imagePath = newImgPath();
                        try {
                            mScanCarApi.CarImage(imagePath.getBytes("gbk"));
                            JSONObject localJSONObject1 = new JSONObject(new String(arrayOfByte, "gbk"));
                            //车牌号
                            String mEt_carno = localJSONObject1.getString("Num");
                            //行数
                            String mEt_layer = localJSONObject1.getString("Layer");
                            //颜色
                            String mEt_color = localJSONObject1.getString("Color");
                            Intent i = new Intent();
                            String ocrResult = "车牌号：" + mEt_carno + "\n"
                                    + "行数:" + mEt_layer + "\n"
                                    + "颜色:" + mEt_color + "\n"
                                    + "图片:" + imagePath + "\n";
                            i.putExtra("OCRResult", ocrResult);
                            setResult(RESULT_OK, i);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    protected void onResume() {
        super.onResume();
    }

    private byte[] data;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        this.data = data;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            mCamera = Camera.open(0);//0:后置 1：前置
            initCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            mCamera.setPreviewDisplay(holder);
            initAutoFocusTimer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    /**
     * 初始化相机
     */
    void initCamera() {
        try {
            mCamera.setPreviewCallback(this);
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size optionSize = CameraUtils.findBestPreviewResolution(mCamera);
            parameters.setPreviewSize(optionSize.width, optionSize.height);
            parameters.setPictureSize(optionSize.width, optionSize.height);
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setFlashMode("off");
            parameters.setPictureFormat(256);
            parameters.setJpegQuality(100);
            parameters.set("orientation", "portrait");
            parameters.set("rotation", 90);
            mCamera.setDisplayOrientation(90);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放相机
     */
    void closeCamera() {
        try {
            if (autoFocusTimer != null) {
                autoFocusTimer.cancel();
            }
            if (mCamera != null) {
                mCamera.stopPreview();
                //mCamera.release();//加上要挂啊
                mCamera = null;
            }
        } catch (Exception e) {
        }
    }

    Timer autoFocusTimer;

    void initAutoFocusTimer() {
        final Camera.Size size = mCamera.getParameters().getPreviewSize();
        final int[] arrayOfInt = new int[4];
        arrayOfInt[0] = view2.getLeft();
        arrayOfInt[1] = view2.getTop();
        arrayOfInt[2] = view2.getRight();
        arrayOfInt[3] = view2.getBottom();
        if (autoFocusTimer == null) {
            autoFocusTimer = new Timer();
            autoFocusTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mCamera != null) {
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean suc, Camera camera) {
                                if (mCamera != null) {
                                    mCamera.cancelAutoFocus();
                                }
                            }
                        });
                    }
                    if (!success && data != null) {
                        mScanCarApi.ScanCarNo(data, size.width, size.height, arrayOfInt, ScanCarActivity.this);
                    }
                }
            }, 0, 300);
        }

    }

    @Override
    public void finish() {
        super.finish();
        closeCamera();
    }

    public static String newImgPath() {
        File localFile = new File(Environment.getExternalStorageDirectory().getPath() + "/ccymimg/");
        if (!localFile.exists())
            localFile.mkdirs();
        return Environment.getExternalStorageDirectory().getPath() + "/ccymimg/" + new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date()) + ".jpg";
    }
}
