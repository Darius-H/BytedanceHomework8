package com.example.cameratest;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.PathUtils;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.transform.Result;



public class MainActivity extends AppCompatActivity {
    private String[] mPermissionsArrays = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
//    private String[] mPermissionsArrays = new String[]{Manifest.permission.CAMERA};
    private int CAMERA_REQUEST_CODE= 1;
    private int REQUEST_CODE_TAKE_PHOTO =666;
    private int REQUEST_CODE_RECORD_VIDEO =667;
    private int result_code = 999;
    private ImageView imageView;
    private VideoView videoview;
    private Button take_photo_button,record_button;
    String takeImagePath;
    Uri uri;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView=findViewById(R.id.imageView);
        imageView.bringToFront();
        videoview=findViewById(R.id.videoView);
        PermissionCheck();
        InitButton();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void PermissionCheck()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"have the right to use camera",Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions(mPermissionsArrays, CAMERA_REQUEST_CODE);
        }
    }

    void InitButton()
    {
        take_photo_button=findViewById(R.id.button);
        record_button=findViewById(R.id.button2);
        take_photo_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File MediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);//获得系统存放照片的路径
                String TimeStamp= new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//通过时间戳确保照片名不同
                File mediaFile = new File(MediaStorageDir,"IMG"+TimeStamp+".jpg");
                if(!mediaFile.exists())
                {
                    mediaFile.getParentFile().mkdirs();
                }
                takeImagePath=mediaFile.getAbsolutePath();
                uri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".fileprovider",mediaFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
                /*
                这里如果我使用真的手机运行，会成功startActivityForResult(cameraIntent, REQUEST_CODE_TAKE_PHOTO);
                但是如果用虚拟机做实验，会跳转到else语句。原因未知
                 */
                if(cameraIntent.resolveActivity(getPackageManager())!=null) {
                    startActivityForResult(cameraIntent, REQUEST_CODE_TAKE_PHOTO);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"failed",Toast.LENGTH_SHORT).show();
                }
            }
        });
        record_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent videointent=new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if(videointent.resolveActivity(getPackageManager())!=null)
                {
                    startActivityForResult(videointent,REQUEST_CODE_RECORD_VIDEO);
                }
            }
        });
    }

    void ScalePhoto(String takeImagePath)
    {
        //获取ImageView控件的宽高
        int targetWidth = imageView.getWidth();
        int targetHeight = imageView.getHeight();
        //创建Options,设置inJustDecodeBounds为true，只解码图片宽高信息
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds =true;
        BitmapFactory.decodeFile(takeImagePath,options);
        int photoWidth=options.outWidth;
        int photoHeight = options.outHeight;
        //计算图片和控件的缩放比例，设置给Options,然后设置inJustDecodeBounds为false,解码真正的图片信息
        int scaleFactor=Math.min(photoWidth/targetWidth,photoHeight/targetHeight);
        options.inJustDecodeBounds=false;
        options.inSampleSize=scaleFactor;
        Bitmap bitmap= BitmapFactory.decodeFile(takeImagePath,options);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(bitmap);
    }
    /*使用荣耀10拍照，发现返回的Intent是null,根据：
    https://blog.csdn.net/dong_1024/article/details/50935358
    这个网站说部分机型的照相机有自己的存储照片的位置，如果向Intent中传入uri，返回的Intent会变成null
    于是我另外查找了如何直接通过uri获取bitmap对象，解决当Intent为null时的情况
     */
    @Override
    protected void onActivityResult(int requestedCode,int resultCode,@Nullable Intent data) {
        super.onActivityResult(requestedCode,resultCode,data);
        if(requestedCode==REQUEST_CODE_TAKE_PHOTO&&resultCode==RESULT_OK)
        {
            if(data!=null)
            {
                Bundle extras= data.getExtras();
                Bitmap bitmap= null;
                if (extras != null) {
                    bitmap = (Bitmap)extras.get("data");
                }
                //imageView.setImageBitmap(bitmap);
                ScalePhoto(takeImagePath);
            }
            else
            {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //imageView.setImageBitmap(bitmap);
                ScalePhoto(takeImagePath);
            }
        }
        else if(requestedCode==REQUEST_CODE_RECORD_VIDEO&&resultCode==RESULT_OK)
        {
            Uri videouri=data.getData();
            videoview.setVideoURI(videouri);
            imageView.setVisibility(View.INVISIBLE);
            videoview.start();
        }
    }

    private String getOutputMediaPath()
    {
        File MediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);//获得系统存放照片的路径
        String TimeStamp= new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//通过时间戳确保照片名不同
        File mediaFile = new File(MediaStorageDir,"IMG"+TimeStamp+".jpg");
        if(!mediaFile.exists())
        {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }
}