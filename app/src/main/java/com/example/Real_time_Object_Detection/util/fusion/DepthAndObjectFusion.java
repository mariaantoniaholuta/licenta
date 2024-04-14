package com.example.Real_time_Object_Detection.util.fusion;

import static com.example.Real_time_Object_Detection.CameraActivity.DisplayHeightInPixels;
import static com.example.Real_time_Object_Detection.util.fusion.AverageObjectHeight.getAverageObjectHeight;

import android.graphics.Rect;
import android.util.Log;

public class DepthAndObjectFusion {
    private static final float METERS_CALIBRATOR = 2.4f;

    public float adjustDistanceBasedOnObjectSizeAndType(float estimatedDepth, Rect boundingBox, String objectType) {
        float adjustedDistance = estimatedDepth;
        float boundingBoxHeight = boundingBox.height();
        float boundingBoxWidth = boundingBox.width();
        float averageObjectHeight = getAverageObjectHeight(objectType);

        String[] heightLabels = {"person", "car", "car", "truck", "motorcycle", "bicycle", "dog", "cat", "traffic light", "laptop", "keyboard", "tv"};

        // check if the object label is in the height list
        boolean isHeightLabels = false;
        for (String label : heightLabels) {
            if (objectType.equals(label)) {
                isHeightLabels = true;
                break;
            }
        }

        float heightInMeters = pixelHeightToMeters(boundingBoxHeight, adjustedDistance);

        if(isHeightLabels) {
            if (heightInMeters < averageObjectHeight) {
                float distanceAdjustmentFactor = heightInMeters / averageObjectHeight;
                adjustedDistance /= distanceAdjustmentFactor;
            } else if (heightInMeters > averageObjectHeight) {
                float distanceAdjustmentFactor = averageObjectHeight / heightInMeters;
                adjustedDistance *= distanceAdjustmentFactor;
            }
        }

        return adjustedDistance;
    }

    public float pixelHeightToMeters(float pixelHeight, float distanceToCamera) {
        return (pixelHeight / 1000f) * distanceToCamera;
    }

    public float estimateDistanceBasedOnSizeAndType(Rect boundingBox, String objectType) {
        float averageObjectHeight = getAverageObjectHeight(objectType);
        float objectHeightInPixels = boundingBox.height();
        float screenHeightInPixels = DisplayHeightInPixels;

        float estimatedDistance = averageObjectHeight * (screenHeightInPixels / objectHeightInPixels);

        return estimatedDistance;
    }

    public float adjustDistanceForClosenessPrecision(float estimatedDepth) {
        if (estimatedDepth >= METERS_CALIBRATOR) {
            return estimatedDepth - METERS_CALIBRATOR;
        } else {
            return estimatedDepth;
        }
    }

    public float getAverageObjectHeight1(String objectType) {
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
            case "keyboard":
                averageObjectHeight = 0.2f;
                break;
        }
        return averageObjectHeight;
    }

}
