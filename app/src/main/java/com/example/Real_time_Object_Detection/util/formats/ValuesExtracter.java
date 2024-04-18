package com.example.Real_time_Object_Detection.util.formats;

import android.util.Log;

public class ValuesExtracter {
    //String.format("%s: %.2fm", objectLabel, estimatedDepth, adjustedDistance);
    public static float getAdjustedDistanceValue(String formattedString) {
        String[] parts = formattedString.split(": ");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Formatted string does not contain :");
        }

        String valuePart = parts[1].trim();
        String numberString = valuePart.split("m")[0].trim();
        return Float.parseFloat(numberString);
    }

    public static float[] parseOrientationFromText(String text) {
        float[] orientation = new float[3]; // azimuth, pitch, roll
        try {
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.startsWith("Azimuth")) {
                    orientation[0] = Float.parseFloat(line.split(": ")[1].replace("°", ""));
                } else if (line.startsWith("Pitch")) {
                    orientation[1] = Float.parseFloat(line.split(": ")[1].replace("°", ""));
                } else if (line.startsWith("Roll")) {
                    orientation[2] = Float.parseFloat(line.split(": ")[1].replace("°", ""));
                }
            }
        } catch (Exception e) {
            Log.e("ParseError", "Failed to parse orientation: " + e.getMessage());
        }
        return orientation;
    }

}
