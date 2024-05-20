package com.example.Real_time_Object_Detection.util.fusion;

import static com.example.Real_time_Object_Detection.CameraActivity.DisplayHeightInPixels;
import static com.example.Real_time_Object_Detection.util.fusion.AverageObjectHeight.getAverageObjectHeight;

import android.graphics.Rect;
import android.util.Log;

public class DepthAndObjectFusion {
    private static final float METERS_CALIBRATOR = 1.9f;

    public float adjustDistanceBasedOnObjectSizeAndType(float estimatedDepth, Rect boundingBox, String objectType) {
        float adjustedDistance = estimatedDepth;
        float boundingBoxHeight = boundingBox.height();
        float boundingBoxWidth = boundingBox.width();
        float averageObjectHeight = getAverageObjectHeight(objectType);

        String[] heightLabels = {
                "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
                "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
                "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
                "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
                "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
                "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
                "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake",
                "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop",
                "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
                "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
        };

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
        return Math.max(0, estimatedDepth - METERS_CALIBRATOR);
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
