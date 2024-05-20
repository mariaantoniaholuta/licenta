package com.example.Real_time_Object_Detection.util.warnings_environment;


import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

public class TrafficLight {

    public static String trafficLightText = "";

    public enum TrafficLightColor {
        RED, GREEN, UNDETERMINED
    }

    public static TrafficLightColor analyzeColor(Bitmap image, Rect boundingBox) {
        trafficLightText = "";

        int redSum = 0;
        int greenSum = 0;
        int pixelCount = 0;
        int darkPixelCount = 0;

        int validatedTop = Math.max(0, boundingBox.top);
        int validatedBottom = Math.min(image.getHeight(), boundingBox.bottom);
        int validatedLeft = Math.max(0, boundingBox.left);
        int validatedRight = Math.min(image.getWidth(), boundingBox.right);

        for (int y = validatedTop; y < validatedBottom; y++) {
            for (int x = validatedLeft; x < validatedRight; x++) {
                int pixel = image.getPixel(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                int brightness = (int)(0.299 * red + 0.587 * green + 0.114 * blue);

                //Log.d("Traffic Light Detection", "bright"+brightness);
                if (brightness > 90) {
                    redSum += red;
                    greenSum += green;
                    darkPixelCount++;
                }
            }
        }

        if (darkPixelCount == 0) {
            Log.d("Traffic Light Detection", "No dark pixels found in the area. Unable to determine the traffic light color.");
            trafficLightText = "Can't determine color.";
            return TrafficLightColor.UNDETERMINED;
        }

        int avgRed = redSum / darkPixelCount;
        int avgGreen = greenSum / darkPixelCount;

        Log.d("Traffic Light Detection", "Computed average Red: " + avgRed + ", average Green: " + avgGreen);

        int colorDifferenceThreshold = 15;
        Log.d("Traffic Light Detection", "Difference color"+Math.abs(avgRed - avgGreen));
        if (Math.abs(avgRed - avgGreen) < colorDifferenceThreshold) {
            Log.d("Traffic Light Detection", "Difference between red and green values is too small.");
            trafficLightText = "Color difference is too small.";
            return TrafficLightColor.UNDETERMINED;
        }

        TrafficLightColor colorResult = (avgGreen > avgRed) ? TrafficLightColor.GREEN : TrafficLightColor.RED;
        String actionMessage = (colorResult == TrafficLightColor.GREEN) ? "Light is green." : "Light is red.";
        Log.d("Traffic Light Action", actionMessage);

        if(colorResult == TrafficLightColor.GREEN) {
            trafficLightText = "GO! Light is green.";
        } else {
            trafficLightText = "Wait! Light is red. ";
        }

        return colorResult;
    }

}
