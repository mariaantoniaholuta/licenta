package com.example.Real_time_Object_Detection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.example.Real_time_Object_Detection.model.VehicleLabel;
import com.example.Real_time_Object_Detection.util.DepthAndObjectFusion;
import com.example.Real_time_Object_Detection.util.DepthMapUtil;

import org.opencv.core.Mat;

import java.util.List;

public class DepthRecognition {

    public static String analyzeDepthAndAdjustDistance(Rect boundingBox, Bitmap bitmapForProcessing, Bitmap depthBitmap, Mat processedImage, List<String> categories, double classOfDetectedObject, int depthMapWidth, int depthMapHeight) {
        DepthMapUtil depthAnalyzer = new DepthMapUtil();
        DepthAndObjectFusion fusionUtil = new DepthAndObjectFusion();
        Rect scaledRectForDepth = depthAnalyzer.scaleRectToDepthMap(boundingBox, depthMapWidth, depthMapHeight, processedImage.width(), processedImage.height());
        Bitmap croppedDepthBitmap = depthAnalyzer.safeCreateCroppedBitmap(depthBitmap, scaledRectForDepth);

        String objectLabel = categories.get((int) classOfDetectedObject);
        //String[] vehicleLabels = {"bus", "train", "car", "truck", "motorcycle", "bicycle"};

        // check if the object label is a vehicle
        boolean isEqualToVehicle = false;
        for (VehicleLabel label : VehicleLabel.values()) {
            if (objectLabel.equalsIgnoreCase(label.toString())) {
                isEqualToVehicle = true;
                break;
            }
        }

        // based on object type, if it's a vehicle calculate max
        float estimatedDepth = -1.0f;
        if (!isEqualToVehicle) {
            estimatedDepth = depthAnalyzer.analyzeCroppedDepthMap(croppedDepthBitmap, bitmapForProcessing);
        } else {
            estimatedDepth = depthAnalyzer.analyzeMaxCroppedDepthMap(croppedDepthBitmap, bitmapForProcessing);
        }

        if (estimatedDepth == -1) {
            Log.d("it is -1:", String.valueOf(estimatedDepth));
            estimatedDepth = fusionUtil.estimateDistanceBasedOnSizeAndType(boundingBox, objectLabel);
            Log.d("it is -1:", String.valueOf(estimatedDepth));
        }

        float adjustedDistance = fusionUtil.adjustDistanceBasedOnObjectSizeAndType(estimatedDepth, boundingBox, objectLabel);

        Log.d("estimated d:", String.valueOf(estimatedDepth));
        Log.d("adjusted d:", String.valueOf(adjustedDistance));
        //return String.format("%s D: %.2f D.A: %.2f", objectLabel, estimatedDepth, adjustedDistance);
        return String.format("%s D.A: %.2f", objectLabel, adjustedDistance);
    }
}
