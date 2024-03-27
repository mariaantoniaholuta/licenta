package com.example.Real_time_Object_Detection.util;

import static com.example.Real_time_Object_Detection.CameraActivity.DisplayHeightInPixels;

import android.graphics.Rect;
import android.util.Log;

public class DepthAndObjectFusion {

    public float adjustDistanceBasedOnObjectSizeAndType(float estimatedDepth, Rect boundingBox, String objectType) {
        float adjustedDistance = estimatedDepth;
        float boundingBoxHeight = boundingBox.height();
        float boundingBoxWidth = boundingBox.width();
        float averageObjectHeight = getAverageObjectHeight(objectType);

        String[] heightLabels = {"person", "car", "car", "truck", "motorcycle", "bicycle", "dog", "cat", "traffic light", "laptop"};

        // check if the object label is in the height list
        boolean isHeightLabels = false;
        for (String label : heightLabels) {
            if (objectType.equals(label)) {
                isHeightLabels = true;
                break;
            }
        }

        float heightInMeters = pixelHeightToMeters(boundingBoxHeight, adjustedDistance);
        Log.d("height in meters:", (objectType + heightInMeters));

        if(isHeightLabels) {
            if (heightInMeters > averageObjectHeight) {
                // if calculated height is higher than average object height, it might be closer
                adjustedDistance *= (averageObjectHeight / heightInMeters);
            }
        }

        return adjustedDistance;
    }

    public float pixelHeightToMeters(float pixelHeight, float distanceToCamera) {
        return (pixelHeight / 1000f) * distanceToCamera;  // using estimated distance and camera calibration
    }

    public float estimateDistanceBasedOnSizeAndType(Rect boundingBox, String objectType) {
        float averageObjectHeight = getAverageObjectHeight(objectType);
        float objectHeightInPixels = boundingBox.height();
        float screenHeightInPixels = DisplayHeightInPixels;

        float estimatedDistance = averageObjectHeight * (screenHeightInPixels / objectHeightInPixels);

        return estimatedDistance;
    }

    public float getAverageObjectHeight(String objectType) {
        float averageObjectHeight = 0.5f;
        switch (objectType) {
            case "person":
                averageObjectHeight = 1.7f;
                break;
            case "car":
                averageObjectHeight = 1.5f;
                break;
            case "truck":
                averageObjectHeight = 3.6f;
                break;
            case "motorcycle":
                averageObjectHeight = 0.9f;
                break;
            case "bicycle":
                averageObjectHeight = 0.8f;
                break;
            case "dog":
                averageObjectHeight = 0.5f;
                break;
            case "cat":
                averageObjectHeight = 0.3f;
                break;
            case "traffic light":
                averageObjectHeight = 0.6f;
                break;
            case "laptop":
                averageObjectHeight = 0.3f;
                break;
        }
        return averageObjectHeight;
    }

}
