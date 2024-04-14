package com.example.Real_time_Object_Detection.util.position;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

public class FrameStabilizer {
    private float[] referenceOrientation = null; //(azimuth, pitch, roll)
    private float[] lastOrientation = null;

    public void setReferenceOrientation(float[] orientation) {
        if (orientation != null && orientation.length == 3) {
            referenceOrientation = orientation.clone();
        }
    }

    public void updateOrientation(float[] currentOrientation) {
        if (currentOrientation != null && currentOrientation.length == 3) {
            if (lastOrientation == null) {
                lastOrientation = currentOrientation.clone();
            } else {
                lastOrientation = currentOrientation.clone();
            }
        }
    }

    public Mat stabilizeFrame(Mat inputFrame, float[] currentOrientation) {
        if (referenceOrientation == null || currentOrientation == null || currentOrientation.length < 3) {
            return inputFrame; // original frame if no reference or current orientation is set
        }

        Log.d("FrameStabilizer", "Current orientation: " + Arrays.toString(currentOrientation));
        float alpha = 0.05f; // smoothing constant
        float[] smoothedOrientation = smoothOrientation(currentOrientation, lastOrientation, alpha);

        float rollDifference = smoothedOrientation[2] - referenceOrientation[2];
        Log.d("FrameStabilizer", "diff: " + rollDifference);

        final double correctionMinThreshold = 1.0;
        final double correctionMaxThreshold = 10.0;
        if ((Math.abs(rollDifference) > correctionMaxThreshold) || (Math.abs(rollDifference) < correctionMinThreshold)) {
            Log.d("FrameStabilizer", "smaller ");
            //don't apply here
            return inputFrame;
        }

        Mat rotationMatrix = getRotationMatrix(rollDifference, inputFrame);
        Mat stabilizedFrame = new Mat();
        Imgproc.warpAffine(inputFrame, stabilizedFrame, rotationMatrix, new Size(inputFrame.cols(), inputFrame.rows()));


        return stabilizedFrame;
    }

    private Mat getRotationMatrixForStabilization(double rollDifference, Mat inputFrame) {
        Point center = new Point(inputFrame.cols() / 2.0, inputFrame.rows() / 2.0);
        double scale = 1.0; // No scaling
        return Imgproc.getRotationMatrix2D(center, -rollDifference, scale); // Negative for corrective rotation
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