package com.example.Real_time_Object_Detection;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


import java.io.IOException;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean isDepthMode = false;
    private boolean isSurroundingsCheck = false;
    private MiDASModel depthModel;

    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        try {
            depthModel = new MiDASModel(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button toggleDepthButton = findViewById(R.id.depth_toggle_button);
        toggleDepthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDepthMode = !isDepthMode;
                Log.d(TAG,"Depth Mode Clicked");
            }
        });

        Button surroundingsCheckButton = findViewById(R.id.surroundings_check_button);
        surroundingsCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSurroundingsCheck = !isSurroundingsCheck;
                Log.d(TAG,"Surroundings Check Clicked");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Mat moutput = new Mat();

        if (isDepthMode) {
            Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, bitmap);

            // returning a Bitmap with depth information
            Bitmap depthMapBitmap = depthModel.getDepthMap(bitmap);

            Mat depthMapMat = new Mat();
            Utils.bitmapToMat(depthMapBitmap, depthMapMat);

            Mat resizedDepthMapMat = new Mat();
            Imgproc.resize(depthMapMat, resizedDepthMapMat, new Size(mRgba.cols(), mRgba.rows()));
            moutput = resizedDepthMapMat;
            return moutput;

        } else if(isSurroundingsCheck){
            ObjectDetection objectDetection = null;
            Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, bitmap);

            Bitmap depthMapBitmap = depthModel.getDepthMap(bitmap);
            try {
                objectDetection = new ObjectDetection(300, getAssets(), "ssd_mobilenet.tflite", "labelmap.txt");
                Log.d("MainActivity", "Model Depth Check is Successfully loaded");
            } catch (IOException e) {
                Log.d("MainActivity", "Model Depth Check Failed to load");
                e.printStackTrace();
            }
            moutput = objectDetection.recongizeImageObjectsAndDepth(mRgba, depthMapBitmap);
        } else {
            ObjectDetection objectDetection = null;

            try {
                objectDetection = new ObjectDetection(300, getAssets(), "ssd_mobilenet.tflite", "labelmap.txt");
                Log.d("MainActivity", "Model is Successfully loaded");
            } catch (IOException e) {
                Log.d("MainActivity", "Model is Failed to load");
                e.printStackTrace();
            }
            moutput = objectDetection.recongizeImageObjects(mRgba);
        }

        return moutput;
    }

}