package com.example.Real_time_Object_Detection.util.warnings;

import static com.example.Real_time_Object_Detection.util.tracker.TrackedObject.isVehicle;

import android.util.Log;
import android.widget.TextView;

import com.example.Real_time_Object_Detection.DepthRecognition;
import com.example.Real_time_Object_Detection.model.DetectionObject;

import org.opencv.core.Mat;

import java.util.List;


public class WarningText {

    private TextView  warningTextView;

    public WarningText(TextView  warningTextView) {
        this. warningTextView =  warningTextView;
    }

    private void setWarning(String objectName) {
        String text;
        text = String.format("Careful! A" + objectName + " is too Close");
        updatePositionStatus(text);
    }

    private void updatePositionStatus(String text) {
        warningTextView.post(new Runnable() {
            @Override
            public void run() {
                warningTextView.setText(text);
            }
        });
    }



    public static void generateWarnings(List<DetectionObject> currentFrameObjects, Mat processedImage, List<DetectionObject> trackedObjects, DepthRecognition.OnDepthWarningListener listener) {
        Log.d("Tracking Info", "Number of tracked objects in gener: " + trackedObjects.size());
        for (DetectionObject object : currentFrameObjects) {
            String objectLabel = object.getLabel();
            float adjustedDistance = object.getDepth();
            String warningMessage = "";
            boolean isGettingCloser = false;

            // if the object is getting closer based on tracking logic
            for (DetectionObject tracked : trackedObjects) {
                if (tracked.getId() == object.getId()) {
                    Log.d("Tracking Info", "adjusted current: " + adjustedDistance);
                    Log.d("Tracking Info", "adjusted tracked: " + tracked.getDepth());
                    if(adjustedDistance < tracked.getDepth()) {
                        isGettingCloser = true;
                    }
                    break;
                }
            }
            Log.d("Tracking Info", "is closer: " + isGettingCloser);

            if (isVehicle(objectLabel)) {
                if ((adjustedDistance < 5) && !objectLabel.equals("bicycle")) {
                    warningMessage = "Careful! A " + objectLabel + " is too Close";
                } else if ((adjustedDistance < 2.7f) && objectLabel.equals("bicycle")) {
                    warningMessage = "Careful! A " + objectLabel + " is too close";
                } else if (adjustedDistance < 15 && isGettingCloser) {
                    warningMessage = "Careful! A " + objectLabel + " is getting closer";
                } else {
                    warningMessage = "";
                }
            } else {
                if (adjustedDistance < 1 && objectLabel.equals("person")) {
                    warningMessage = "Careful! A " + objectLabel + " is too Close";
                } else if (adjustedDistance < 2 && isGettingCloser && objectLabel.equals("person")) {
                    warningMessage = "Careful! A " + objectLabel + " is getting closer";
                } else {
                    warningMessage = "";
                }
            }

            if (listener != null) {
                listener.onDepthWarningUpdate(warningMessage);
            }
        }
    }
}
