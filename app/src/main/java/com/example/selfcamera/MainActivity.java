package com.example.selfcamera;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.PathUtils;

import android.Manifest;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private String[] mPermissionsArrays = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private int CAMERA_REQUEST_CODE= 1;
    private Camera mCamera;
    private ImageView take_photo;
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    private ImageView imageView;
    private VideoView videoView;
    private MediaRecorder mMediaRecorder;
    Camera.PictureCallback mPictureCallback;
    private String mp4Path;
    private Boolean isRecording=false;
    private Button mRecordButton;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        take_photo =findViewById(R.id.take_photo);
        take_photo.bringToFront();
        imageView =findViewById(R.id.imageshow);
        mRecordButton=findViewById(R.id.recordButton);
        mRecordButton.bringToFront();
        imageView.setVisibility(View.INVISIBLE);
        imageView.bringToFront();
        videoView=findViewById(R.id.videoView);
        videoView.setVisibility(View.INVISIBLE);
        mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                FileOutputStream fos = null;
                String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + "1.jpg";
                File file = new File(filePath);
                try {
                    fos = new FileOutputStream(file);
                    fos.write(bytes);
                    fos.flush();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    //Bitmap rotateBitmap = PathUtils.rotateImage(bitmap, filePath);//不可用，不存在该函数
                    imageView.bringToFront();
                    imageView.setVisibility(View.VISIBLE);
                    //videoView.setVisibility(View.GONE);
                    //imageView.setImageBitmap(rotateBitmap);
                    /*这里不可以直接使用imageView.setImageBitmap(bitmap),使用的话图片会不可见，原因应该是我把imageView设置
                    为固定大小的，而bitmap尺寸太大了，导致出了问题。
                    */
                    ScalePhoto(filePath);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mCamera.startPreview();
                    if(fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null,null,mPictureCallback);
            }
        });
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record();
            }
        });
        PermissionCheck();
        initCamera();
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
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
        //停止预览效果
        mCamera.stopPreview();
        //重新设置预览效果
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCamera == null) {
            initCamera();
        }
        mCamera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }

    private void initCamera() {
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.set("orientation", "portrait");
        parameters.set("rotation", 90);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
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

    private boolean prepareVideoRecorder() {
        mMediaRecorder= new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera .unlock();
        mMediaRecorder.setCamera(mCamera) ;
        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA) ;
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        // Step 4: Set output file
        mp4Path = getOutputMediaPath();
        mMediaRecorder.setOutputFile(mp4Path) ;
        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mHolder . getSurface());
        mMediaRecorder.setOrientationHint(90);
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
            }catch (Exception e) {
            //releaseMediaRecorder();
            return false;
            }
            return true;
    }

    String getOutputMediaPath()
    {
        return getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath()+File.separator + "1.mp4";
    }

    public void record() {
        if (isRecording) {
            mRecordButton.setText("录制");
            mMediaRecorder.stop() ;
            mMediaRecorder.reset() ;
            mMediaRecorder.release() ;
            mMediaRecorder= null ;
            mCamera.lock();
            videoView.setVisibility (View.VISIBLE);
            imageView.setVisibility (View.GONE) ;
            videoView.setVideoPath (mp4Path) ;
            videoView.start();
        } else {
            if (prepareVideoRecorder()) {
                mRecordButton.setText("暂停") ;
                mMediaRecorder.start();
            }
            isRecording = !isRecording;
        }
    }
}