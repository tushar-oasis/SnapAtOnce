package com.oasis.snapatonce;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.oasis.snapatonce.views.CameraPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by tushar on 16-03-2017.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Bind(R.id.capture_container_layout)
    RelativeLayout cameraPreviewFrameContainer;

    @Bind(R.id.camera_preview_frame)
    FrameLayout cameraPreviewFrame;

    @Bind(R.id.flash)
    ImageView flashButton;

    @Bind(R.id.capture)
    ImageView captureButtonHR;

    @Bind(R.id.switch_camera_container)
    RelativeLayout switchCameraButton;

    final static int FRONT_CAMERA = 1;
    final static int BACK_CAMERA = 0;

    private Camera camera;
    private Camera.Parameters cameraParameters;
    private CameraPreview cameraPreview;
    private List<Camera.Size> availablePreviewSizes;
    private int deviceHeight;
    private int deviceWidth;
    private int currentSelectedCamera = BACK_CAMERA;
    public static int currentSelectedCameraId;
    private final String directoryPath = Environment.getExternalStorageDirectory() + "/SnapAtOnce/";
    private String fullFilePath = "";
    private String currentFlashMode = Camera.Parameters.FLASH_MODE_AUTO;

    private android.os.Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        deviceHeight = Utils.getDeviceHeightInPx(this);
        deviceWidth = Utils.getDeviceWidthInPx(this);
        createCamera(100);
        cameraPreviewFrame.setBackgroundColor(Color.BLACK);
        cameraPreviewFrame.addView(cameraPreview);
        captureButtonHR.setOnClickListener(this);
    }

    private void createCamera(int pictureQuality){
        if(camera != null){
            camera = null;
        }
        camera = getCameraInstance(currentSelectedCamera);
        cameraParameters = camera.getParameters();
        availablePreviewSizes = cameraParameters.getSupportedPreviewSizes();
        Collections.sort(availablePreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return lhs.width-rhs.width;
            }
        });
        int rotation = getCameraRotation();
        if(currentSelectedCamera == BACK_CAMERA){
            cameraParameters.setRotation(rotation);
        } else{
            cameraParameters.setRotation((rotation + 180) % 360);
        }
        Camera.Size optimalPreviewSize = getOptimalPreviewSize();
        cameraParameters.setPictureSize(optimalPreviewSize.width, optimalPreviewSize.height);
        cameraParameters.setPictureFormat(PixelFormat.JPEG);
        cameraParameters.setJpegQuality(pictureQuality);
        if(currentSelectedCamera == BACK_CAMERA){
            cameraParameters.setFlashMode(currentFlashMode);
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        }
        camera.setParameters(cameraParameters);
        cameraPreview = new CameraPreview((Activity)this, this, camera, optimalPreviewSize);
    }

    private void releaseCamera(){
        if(camera != null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize() {
        double targetRatio = (double) deviceHeight/deviceWidth;
        double aspectTolerance = Double.MAX_VALUE;
        double minDiff = Double.MAX_VALUE;
        Camera.Size optimalPreviewSize = null;
        if(availablePreviewSizes == null){
            return null;
        }
        for(Camera.Size size:availablePreviewSizes){
            double ratio = (double)size.width/size.height;;
            if(size.width > deviceHeight){
                if(Math.abs(ratio - targetRatio) < aspectTolerance){
                    aspectTolerance = Math.abs(ratio - targetRatio);
                }
            }
        }
        for(Camera.Size size:availablePreviewSizes){
            double ratio = (double)size.width/size.height;
            if(Math.abs(ratio - targetRatio) > aspectTolerance){
                continue;
            } else{
                if(Math.abs(size.width - deviceHeight) < minDiff){
                    optimalPreviewSize = size;
                    minDiff = Math.abs(size. width - deviceHeight);
                }
            }
        }
        if(optimalPreviewSize == null){
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size:availablePreviewSizes){
                if(Math.abs(size.width - deviceHeight) < minDiff){
                    optimalPreviewSize = size;
                    minDiff = Math.abs(size.width - deviceHeight);
                }
            }
        }
        return optimalPreviewSize;
    }

    private int getCameraRotation(){
        // TODO change this. I guess,, no need of camera info
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(currentSelectedCameraId, cameraInfo);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int cameraRotation;
        if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            cameraRotation = (cameraInfo.orientation + degrees) % 360;
            cameraRotation = (360 - cameraRotation) % 360;
        } else{
            cameraRotation = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return cameraRotation;
    }

    private Camera getCameraInstance(int currentSelectedCamera){
        Camera camera = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        int cameraId;
        if(currentSelectedCamera == FRONT_CAMERA){
            for(cameraId=0; cameraId < cameraCount; cameraId++){
                Camera.getCameraInfo(cameraId, cameraInfo);
                if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    break;
                }
            }
        } else{
            for (cameraId = 0; cameraId < cameraCount; cameraId++){
                Camera.getCameraInfo(cameraId, cameraInfo);
                if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                    break;
                }
            }
        }
        currentSelectedCameraId = cameraId;
        try{
            camera = Camera.open(cameraId);
        } catch (Exception e){
            e.printStackTrace();
        }
        return camera;
    }

    private void handleCaptureButtonClick(){
        Log.d("tushar", "in handleCaptureButtonClick");
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success){
                    camera.takePicture(null, null, pictureCallback);
                }
            }
        });
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".jpg";
            File externalStorageDirectory = new File(directoryPath);
            externalStorageDirectory.mkdirs();
            File picture = new File(externalStorageDirectory, fileName);
            fullFilePath = picture.getAbsolutePath();
            Log.d("tushar", fullFilePath);
            try{
                FileOutputStream outputStream = new FileOutputStream(picture);
                outputStream.write(data);
                outputStream.close();
            } catch (FileNotFoundException fe){
                fe.printStackTrace();
            } catch (IOException ioe){
                ioe.printStackTrace();
            }

        }
    };

    @Override
    public void onClick(View v) {
        if(v.getId() == captureButtonHR.getId()) {
            handleCaptureButtonClick();
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    releaseCamera();
                    createCamera(50);
                    cameraPreviewFrame.removeAllViews();
                    cameraPreviewFrame.addView(cameraPreview);
                }
            }, 1000);
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleCaptureButtonClick();
                }
            }, 2000);
        }
    }
}
