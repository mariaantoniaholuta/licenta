package com.example.Real_time_Object_Detection;

import static com.example.Real_time_Object_Detection.util.formats.ValuesExtracter.parseOrientationFromText;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import com.example.Real_time_Object_Detection.depthMap.MiDASModel;
import com.example.Real_time_Object_Detection.util.position.FrameStabilizer;
import com.example.Real_time_Object_Detection.util.position.SensorHelper;
import com.example.Real_time_Object_Detection.util.position.VibrationHelper;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.util.DisplayMetrics;
import android.widget.TextView;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, DepthRecognition.OnDepthWarningListener {
    private static final String LOG_TAG = "CameraActivity";
    private CameraBridgeViewBase cameraView;
    private Mat rgbaMat;
    private Mat grayMat;
    private boolean depthEnabled = false;
    private boolean surroundingsEnabled = false;
    private MiDASModel miDASModel;

    public static float DisplayHeightInPixels;

    private SensorHelper sensorHelper;
    private VibrationHelper vibrationHelper;

    private TextView positionStatusTextView;

    private TextView warningTextView;

    private FrameStabilizer stabilizer;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Mat processSurroundingsCheck(Mat stabilizedMat) {
        Future<Mat> future = executor.submit(() -> {
            Bitmap bitmap = Bitmap.createBitmap(stabilizedMat.cols(), stabilizedMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(stabilizedMat, bitmap);

            Bitmap depthMapBitmap = miDASModel.getDepthMap(bitmap);
            ImageRecognition imageRecognition;
            try {
                imageRecognition = new ImageRecognition(300, getAssets(), "ssd_mobilenet.tflite", "labelmap.txt");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error loading model", e);
                return stabilizedMat;
            }
            return imageRecognition.detectObjectsInImageAndAnalyzeDepth(stabilizedMat, depthMapBitmap);
        });

        try {
            return future.get(); // block until the computation is done
        } catch (InterruptedException | ExecutionException e) {
            Log.e(LOG_TAG, "Error processing image", e);
            return stabilizedMat;
        }
    }



    private final BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(LOG_TAG, "OpenCV loaded successfully");
                cameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stabilizer = new FrameStabilizer();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float screenHeightInPixels = displayMetrics.heightPixels;
        Log.i(LOG_TAG, "height in pixels is" + screenHeightInPixels);
        DisplayHeightInPixels = screenHeightInPixels;

        initializeScreen();
        checkCameraPermission();

        setContentView(R.layout.camera_view_activity);

        setupCameraView();
        loadMiDASModel();
        setupButtons();

        positionStatusTextView = findViewById(R.id.positionStatusTextView);
        warningTextView = findViewById(R.id.warningTextView);
        DepthRecognition depthRecognition = new DepthRecognition((DepthRecognition.OnDepthWarningListener) this);

        vibrationHelper = new VibrationHelper(this);
        sensorHelper = new SensorHelper(this, positionStatusTextView, vibrationHelper);
    }

    @Override
    public void onDepthWarningUpdate(String warningMessage) {
        runOnUiThread(() -> {
            warningTextView.setText(warningMessage);
        });
    }

    private void initializeScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void checkCameraPermission() {
        final int CAMERA_PERMISSION_REQUEST = 1;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private void setupCameraView() {
        cameraView = findViewById(R.id.frame_Surface);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        cameraView.setMaxFrameSize(740, 580);
    }

    private void loadMiDASModel() {
        try {
            miDASModel = new MiDASModel(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error loading MiDAS model", e);
        }
    }

    private void setupButtons() {
        Button depthToggleButton = findViewById(R.id.depth_toggle_button);
        depthToggleButton.setOnClickListener(v -> {
            surroundingsEnabled = false;
            depthEnabled = !depthEnabled;
            Log.d(LOG_TAG, "Depth Mode toggled");
        });

        Button surroundingsCheckButton = findViewById(R.id.surroundings_check_button);
        surroundingsCheckButton.setOnClickListener(v -> {
            depthEnabled = false;
            surroundingsEnabled = !surroundingsEnabled;
            Log.d(LOG_TAG, "Surroundings Check toggled");
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorHelper.start();
        if (!OpenCVLoader.initDebug()) {
            Log.d(LOG_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, loaderCallback);
        } else {
            Log.d(LOG_TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.stop();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgbaMat = new Mat(height, width, CvType.CV_8UC4);
        grayMat = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        rgbaMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        long startTime = System.currentTimeMillis();
        rgbaMat = inputFrame.rgba();
        grayMat = inputFrame.gray();

        Point startTopRight = new Point(0, rgbaMat.rows() * 1 / 8);
        Point endRight = new Point(rgbaMat.cols()/1.2, rgbaMat.rows() * 1 / 8);
        Point cornerBottomRight = new Point(rgbaMat.cols(), 0);
        //right lines
        Imgproc.line(rgbaMat, startTopRight, endRight, new Scalar(140,120,255), 1);
        Imgproc.line(rgbaMat, endRight, cornerBottomRight, new Scalar(140,120,255), 2);

        //left lines
        Point startTopLeft = new Point(0, rgbaMat.rows() * 1 / 1.15);
        Point endLeft = new Point(endRight.x, rgbaMat.rows() - (rgbaMat.rows() * 1 / 8));
        Point cornerBottomLeft = new Point(rgbaMat.cols(), rgbaMat.rows());

        Imgproc.line(rgbaMat, startTopLeft, endLeft, new Scalar(140,120,255), 1);
        Imgproc.line(rgbaMat, endLeft, cornerBottomLeft, new Scalar(140,120,255), 2);

        Bitmap bitmap = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaMat, bitmap);

        Log.d(LOG_TAG, (String) positionStatusTextView.getText());

        float[] currentOrientation = parseOrientationFromText((String) positionStatusTextView.getText());

        float[] normalOrientation = {104.76f, 80.75f, 1.02f};
        stabilizer.setReferenceOrientation(normalOrientation);

        stabilizer.updateOrientation(currentOrientation);

        Mat stabilizedMat = stabilizer.stabilizeFrame(rgbaMat, currentOrientation);
        //Mat stabilizedMat = rgbaMat;
        Mat outputMat = new Mat();

        if (depthEnabled) {
            outputMat = processDepthMode();
        } else if (surroundingsEnabled) {
            outputMat = processSurroundingsCheck(stabilizedMat);
        } else {
            outputMat = processNormalMode();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        Log.d(LOG_TAG, "Frame processed" + surroundingsEnabled + duration + " ms");

        return outputMat;
    }

    private Mat processDepthMode() {
        Bitmap rgbaBitmap = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaMat, rgbaBitmap);

        Bitmap depthBitmap = miDASModel.getDepthMap(rgbaBitmap);

        Mat depthMat = new Mat();
        Utils.bitmapToMat(depthBitmap, depthMat);

        Mat resizedDepthMat = new Mat();
        Imgproc.resize(depthMat, resizedDepthMat, new Size(rgbaMat.cols(), rgbaMat.rows()));

        return resizedDepthMat;
    }


    private Mat processSurroundingsCheck1(Mat stabilizedMat) {
        ImageRecognition imageRecognition = null;
        Bitmap bitmap = Bitmap.createBitmap(stabilizedMat.cols(), stabilizedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(stabilizedMat, bitmap);

        Bitmap depthMapBitmap = miDASModel.getDepthMap(bitmap);
        try {
            imageRecognition = new ImageRecognition(300, getAssets(), "ssd_mobilenet.tflite", "labelmap.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageRecognition.detectObjectsInImageAndAnalyzeDepth(stabilizedMat, depthMapBitmap);

    }

    private Mat processNormalMode() {
        ImageRecognition imageRecognition = null;
        try {
            imageRecognition = new ImageRecognition(300, getAssets(), "ssd_mobilenet.tflite", "labelmap.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageRecognition.detectObjectsInImage(rgbaMat);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
