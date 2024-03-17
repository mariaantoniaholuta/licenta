package com.example.Real_time_Object_Detection.util.formats;

public class ValuesExtracter {
    //String.format("%s D: %.2f D.A: %.2f", objectLabel, estimatedDepth, adjustedDistance);
    public static float getAdjustedDistanceValue(String formattedString) {
        String[] parts = formattedString.split("D\\.A:\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Formatted string does not contain D.A.:");
        }

        String valueString = parts[1].trim();
        return Float.parseFloat(valueString);
    }
}
