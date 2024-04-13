package com.example.Real_time_Object_Detection.util.fusion;

public class AverageObjectHeight {
    public static float getAverageObjectHeight(String objectType) {
        float averageObjectHeight = 0.5f;

        switch (objectType) {
            case "person":
                averageObjectHeight = 1.6f;
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
                averageObjectHeight = 0.45f;
                break;
            case "cat":
                averageObjectHeight = 0.2f;
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
