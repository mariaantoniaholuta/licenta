package com.example.Real_time_Object_Detection.util.position;

import android.util.Log;

import com.example.Real_time_Object_Detection.util.filters.KalmanFilter;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

public class FrameStabilizer {
    private float[] referenceOrientation = null; // (azimuth, pitch, roll)
    private float[] lastOrientation = null;
    private float lastAlpha = 0.9f; // initially high smooth to adapt quickly
    private long lastUpdateTime;
    private KalmanFilter[] kalmanFilters = new KalmanFilter[3];
    public FrameStabilizer() {
        for (int i = 0; i < 3; i++) {
            kalmanFilters[i] = new KalmanFilter(0, 1);
        }
    }

    public void setReferenceOrientation(float[] orientation) {
        if (orientation != null && orientation.length == 3) {
            referenceOrientation = orientation.clone();
            lastUpdateTime = System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                kalmanFilters[i].update(orientation[i]);
            }
        }
    }

    public void updateOrientation(float[] currentOrientation) {
        // dynamic alpha based on the rate of change and time - for smoothing
        if (currentOrientation != null && currentOrientation.length == 3) {
            if (lastOrientation == null) {
                lastOrientation = currentOrientation.clone();
            } else {
                long currentTime = System.currentTimeMillis();
                for (int i = 0; i < 3; i++) {
                    //Log.d("FrameStabilizer", "before kalman f: " +  currentOrientation[i]);
                    currentOrientation[i] = kalmanFilters[i].update(currentOrientation[i]);
                    //Log.d("FrameStabilizer", "after kalman f: " +  currentOrientation[i]);
                }
                float alpha = calculateAlpha(currentOrientation, currentTime - lastUpdateTime);
                float alpha1 = calculateAlpha1(currentOrientation);
                lastOrientation = smoothOrientation(currentOrientation, lastOrientation, alpha);
                lastUpdateTime = currentTime;
            }
        }
    }

    private float calculateAlpha(float[] currentOrientation, long elapsedTime) {
        float rateOfChange = Math.abs(lastOrientation[2] - currentOrientation[2]) / elapsedTime;
        float newAlpha = 1 / (1 + (float)Math.log1p(rateOfChange * 1000)); // Example calculation
        return 0.6f * lastAlpha + 0.4f * newAlpha;
    }

    private float calculateAlpha1(float[] currentOrientation) {
        // dynamic alpha based on the rate of change - for smoothing
        float rateOfChange = Math.abs(lastOrientation[2] - currentOrientation[2]);
        float newAlpha = Math.max(0.2f, Math.min(0.9f, 1.0f - (float)Math.pow(rateOfChange / 180.0f, 2)));

        return 0.5f * lastAlpha + 0.5f * newAlpha;
    }

    public Mat stabilizeFrame(Mat inputFrame, float[] currentOrientation) {
        if (referenceOrientation == null || currentOrientation == null || currentOrientation.length < 3) {
            return inputFrame;
        }

        Log.d("FrameStabilizer", "Current orientation: " + Arrays.toString(currentOrientation));

        float rollDifference = currentOrientation[2] - referenceOrientation[2];
        Log.d("FrameStabilizer", "Roll difference: " + rollDifference);

        if (Math.abs(rollDifference) < 1.0 || Math.abs(rollDifference) > 60.0 ) {
            // don't apply to minor or excessive rotations
            return inputFrame;
        }
        if(currentOrientation[1] > 86.5f || lastOrientation[1] > 86.5f) {
            return inputFrame;
        }

        Mat rotationMatrix = getRotationMatrix(rollDifference, inputFrame);
        Mat stabilizedFrame = new Mat();
        Imgproc.warpAffine(inputFrame, stabilizedFrame, rotationMatrix, new Size(inputFrame.cols(), inputFrame.rows()));

        return stabilizedFrame;
    }

    private Mat getRotationMatrix(float rollDifference, Mat inputFrame) {
        double dampingFactor = 0.003;
        double angle = Math.toDegrees(rollDifference) * -1 * dampingFactor;
        Point center = new Point(inputFrame.cols() / 2.0, inputFrame.rows() / 2.0);
        double scale = 1.0;
        return Imgproc.getRotationMatrix2D(center, angle, scale);
    }

    private float[] smoothOrientation(float[] currentOrientation, float[] lastOrientation, float alpha) {
        float[] smoothedOrientation = new float[3];
        for (int i = 0; i < 3; i++) {
            smoothedOrientation[i] = lastOrientation[i] + alpha * (currentOrientation[i] - lastOrientation[i]);
        }
        return smoothedOrientation;
    }
}
