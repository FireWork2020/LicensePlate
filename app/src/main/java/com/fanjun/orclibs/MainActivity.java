package com.fanjun.orclibs;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.fanjun.orclibs.Utils.Base64Util;
import com.fanjun.orclibs.Utils.FileUtil;
import com.fanjun.orclibs.Utils.HttpUtil;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button imgCard, videoCard, currentCard;
    TextView mSuccessText;
    public static final int SCAN_IMAGE_REQUEST = 1;
    public static final int SCAN_VIDEO_REQUEST = 2;
    public static final int SCAN_CURRENT_REQUEST = 3;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public void verifyPermission(Context context) {
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgCard = findViewById(R.id.imageRecognition);
        videoCard = findViewById(R.id.videoRecognition);
        currentCard = findViewById(R.id.currentRecognition);
        mSuccessText = findViewById(R.id.mSuccessText);
        imgCard.setOnClickListener(this);
        videoCard.setOnClickListener(this);
        currentCard.setOnClickListener(this);
        AndPermission.with(this).runtime().permission(Permission.Group.CAMERA, Permission.Group.STORAGE).start();
    }

    public void getBitmapsFromVideo() {
        //获取权限
        verifyPermission(getApplicationContext());

        //选取视频
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SCAN_VIDEO_REQUEST);

    }
    public void getBitmapsFromImage(){
        //获取权限
        verifyPermission(getApplicationContext());

        //选取视频
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SCAN_IMAGE_REQUEST);
    }


    public void splitVideoToImage(Intent data){
        VideoView videoView = (VideoView)findViewById(R.id.videoView);
        Uri uri = data.getData();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String number= cursor.getString(0); // 视频编号
        final String path = cursor.getString(1); // 视频文件路径
        videoView.setVideoPath(path);
        videoView.start();
        String size = cursor.getString(2); // 视频大小
        String name = cursor.getString(3); // 视频文件名
        Log.e("-----","number="+number);
        Log.e("-----","v_path="+path);
        Log.e("-----","v_size="+size);
        Log.e("-----","v_name="+name);
        new Thread(new Runnable() {
            @Override
            public void run() {
                splitToImage(path);
            }
        }).start();
    }
    public String splitToImage(String videoPath){
        TreeSet<String> codes = new TreeSet<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoPath);


        // 取得视频的长度(单位为毫秒)
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        // 取得视频的长度(单位为秒)
        int seconds = Integer.valueOf(time) / 1000;

        // 得到每一秒时刻的bitmap比如第一秒,第二秒
        Long currentTimeMillis = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= 26 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        File dir = new File(Environment.getExternalStorageDirectory()+File.separator+currentTimeMillis);
        if(!dir.exists()){
            dir.mkdirs();
            Log.e("----","创建文件夹成功"+dir.getPath());
        }
        Log.e("----","共："+seconds);
        for (int i = 1; i <= seconds; i++) {

            Bitmap bitmap = retriever.getFrameAtTime(i*1000*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            //String path = Environment.getExternalStorageDirectory()+ File.separator + currentTimeMillis + File.separator + i + ".jpg";
            String path = "/storage/emulated/0/DCIM/Camera/"+i + ".jpg";
            Log.e("-----","图片路径:"+path);

            FileOutputStream fos = null;
            File img = new File(path);
            if(!img.exists()){
                try {
                    img.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                fos = new FileOutputStream(img);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            codes.add(licensePlate(path));
        }
        StringBuilder newText = new StringBuilder();
        for(String temp:codes){
            newText.append(temp+"\n");
        }
        mSuccessText.setText(newText.toString());
        return null;
    }


    public String getCode(String oldResult){
        if(!oldResult.contains("error_code")){
            String[] ss = oldResult.split(":");
            for(int i = 0;i < ss.length;i++){
                Log.e("-------",i+"个"+ss[i]);
            }
            String temp = ss[4];
            String[] sss = temp.split("\"");
            for(int i = 0;i <sss.length;i++){
                Log.e("-----",i+"个"+sss[i]);
            }
            Log.e("车牌号",sss[1]);
            return sss[1];
        }
        return "";
    }
    public String licensePlate(String imgPath){
        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate";
        try{
            byte[] imgData = FileUtil.readFileByBytes(imgPath);
            String imgStr = Base64Util.encode(imgData);
            String imgParam = URLEncoder.encode(imgStr,"UTF-8");
            String param = "image="+imgParam;

            String accessToken = "24.e183d8deaf8e15db390c1976e6a12a80.2592000.1590635385.282335-19639905";

            String result = HttpUtil.post(url,accessToken,param);
            Log.e("----",result);

            return getCode(result);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public void recognizeImage(Intent data){
        Uri uri = data.getData();
        final String path = "/storage/emulated/0/DCIM/Camera/" + System.currentTimeMillis() + ".jpg";
        FileOutputStream fos = null;
        File img = null;
        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        try {
            img = new File(path);
            if(!img.exists()){
                img.createNewFile();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("-----","URI:"+uri.toString());
        ContentResolver cr = this.getContentResolver();
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
            bitmap = zipBitmap(bitmap);
            fos = new FileOutputStream(img);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
            imageView.setImageBitmap(bitmap);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mSuccessText.setText(licensePlate(path));
            }
        }).start();
    }

    public Bitmap zipBitmap(Bitmap oldBitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 10;
        oldBitmap.compress(Bitmap.CompressFormat.JPEG,quality,baos);
//        while(baos.toByteArray().length / 1024 > 100){
//            baos.reset();
//            quality -= 10;
//            if(quality <= 0){
//                break;
//            }
//            oldBitmap.compress(Bitmap.CompressFormat.JPEG,quality,baos);
//        }
        return BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()), null, null);
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = null;
        switch (v.getId()) {
            //照片识别
            case R.id.imageRecognition:
                getBitmapsFromImage();
                break;
            //视频识别
            case R.id.videoRecognition:
                getBitmapsFromVideo();
                break;
            //识别车牌号
            case R.id.currentRecognition:
                Intent intent2 = new Intent(MainActivity.this, ScanCarActivity.class);
                startActivityForResult(intent2, SCAN_CURRENT_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("----","request code:" + requestCode);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SCAN_IMAGE_REQUEST:
                    recognizeImage(data);
                    break;
                case SCAN_VIDEO_REQUEST:
                    splitVideoToImage(data);
                    break;
                case SCAN_CURRENT_REQUEST:
                    mSuccessText.setText(data.getStringExtra("OCRResult"));
                    break;
            }
        }
    }
}
