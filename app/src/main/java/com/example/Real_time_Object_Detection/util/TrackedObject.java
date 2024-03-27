package com.example.Real_time_Object_Detection.util;

import android.graphics.Rect;
import android.util.Log;

import com.example.Real_time_Object_Detection.model.DetectionObject;
import com.example.Real_time_Object_Detection.model.VehicleLabel;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class TrackedObject {

    static int DISTANCE_TO_NEXT_THRESHOLD = 10;
    static double IOU_THRESHOLD = 0.3;
    static double DISTANCE_WEIGHT = 0.6;
    static double IOU_WEIGHT = 0.4;

    static double VEHICLE_DISTANCE_WEIGHT = 0.7;
    static double VEHICLE_IOU_WEIGHT = 0.3;

    public static void drawBoundingBoxes(Mat processedImage, List<DetectionObject> trackedObjects) {
        ColorManager colorManager = new ColorManager();
        for (DetectionObject obj : trackedObjects) {
            //Scalar color = generateColor(obj.getId());
            Scalar color = colorManager.getColorForId(obj.getId());
            //Log.d("tracked:", String.valueOf(obj.getId()+color.hashCode()));
            Rect box = obj.getBoundingBox();
            Point startPoint = new Point(box.left, box.top);
            Point endPoint = new Point(box.right, box.bottom);

            String objectLabel = obj.getLabel();

            // check if the object label is a vehicle
            boolean isEqualToVehicle = false;
            for (VehicleLabel label : VehicleLabel.values()) {
                if (objectLabel.equalsIgnoreCase(label.toString())) {
                    isEqualToVehicle = true;
                    break;
                }
            }

            // if it's not too far or it is a vehicle, color with different colors
            if((obj.getDepth() < 8.5f || isEqualToVehicle) && (obj.getDepth() > 0)) {
                Imgproc.rectangle(processedImage, startPoint, endPoint, color, 2);
            } else {
                Imgproc.rectangle(processedImage,  startPoint, endPoint, new Scalar(255, 165, 0), 2);
            }
        }
    }
    public static void updateTrackedObjects(List<DetectionObject> currentFrameObjects, List<DetectionObject> trackedObjects) {
        List<DetectionObject> updatedTrackedObjects = new ArrayList<>();

        for (DetectionObject currentObject : currentFrameObjects) {
            DetectionObject bestMatch = null;
            double bestCombinedScore = -1;

            boolean isCurrentObjectVehicle = isVehicle(currentObject.getLabel());

            for (DetectionObject trackedObject : trackedObjects) {
                double iouScore = calculateIOU(currentObject.getBoundingBox(), trackedObject.getBoundingBox());
                double distance = currentObject.calculateDistanceBetweenObject(trackedObject);

                double combinedScore;
                if (isCurrentObjectVehicle) {
                    // different for vehicles
                    combinedScore = VEHICLE_IOU_WEIGHT * iouScore + VEHICLE_DISTANCE_WEIGHT * (1 - distance / DISTANCE_TO_NEXT_THRESHOLD);
                } else {
                    combinedScore = IOU_WEIGHT * iouScore + DISTANCE_WEIGHT * (1 - distance / DISTANCE_TO_NEXT_THRESHOLD);
                }

                if (combinedScore > bestCombinedScore) {
                    bestCombinedScore = combinedScore;
                    bestMatch = trackedObject;
                }
            }

            if (bestMatch != null && bestCombinedScore > calculateScoreThreshold(IOU_THRESHOLD, DISTANCE_TO_NEXT_THRESHOLD, isCurrentObjectVehicle)) {
                bestMatch.update(currentObject);
                updatedTrackedObjects.add(bestMatch);
            } else {
                trackedObjects.add(currentObject);
                updatedTrackedObjects.add(currentObject);
            }
        }

        trackedObjects.clear();
        trackedObjects.addAll(updatedTrackedObjects);
    }

    static Scalar  generateColor(int id) {
        return new Scalar(255 * (id % 2), 255 * ((id / 2) % 2), 255 * ((id / 4) % 2));
    }

    //Intersection Over Union
    private static double calculateIOU(Rect boxA, Rect boxB) {
        // intersection array
        int xA = Math.max(boxA.left, boxB.left);
        int yA = Math.max(boxA.top, boxB.top);
        int xB = Math.min(boxA.right, boxB.right);
        int yB = Math.min(boxA.bottom, boxB.bottom);

        int interArea = Math.max(0, xB - xA) * Math.max(0, yB - yA);

        // union A
        int boxAArea = (boxA.right - boxA.left) * (boxA.bottom - boxA.top);
        int boxBArea = (boxB.right - boxB.left) * (boxB.bottom - boxB.top);

        int unionArea = boxAArea + boxBArea - interArea;

        // IOU
        return unionArea > 0 ? (double) interArea / unionArea : 0;
    }

    private static double calculateCombinedScore(double iouScore, double distance, double maxDistance) {
        double normalizedDistanceScore = (maxDistance - distance) / maxDistance;
        return IOU_WEIGHT * iouScore + DISTANCE_WEIGHT * normalizedDistanceScore;
    }
    private static double calculateScoreThreshold(double iouThreshold, double maxDistance, boolean isVehicle) {
        //return IOU_WEIGHT * iouThreshold + DISTANCE_WEIGHT * (maxDistance - maxDistance / 2) / maxDistance;
        double weightIou = isVehicle ? VEHICLE_IOU_WEIGHT : IOU_WEIGHT;
        double weightDistance = isVehicle ? VEHICLE_DISTANCE_WEIGHT : DISTANCE_WEIGHT;

        return weightIou * iouThreshold + weightDistance * (maxDistance - maxDistance / 2) / maxDistance;
    }

    public static boolean isVehicle(String objectLabel) {
        boolean isEqualToVehicle = false;
        for (VehicleLabel label : VehicleLabel.values()) {
            if (objectLabel.equalsIgnoreCase(label.toString())) {
                isEqualToVehicle = true;
                break;
            }
        }
        return isEqualToVehicle;
    }

}
