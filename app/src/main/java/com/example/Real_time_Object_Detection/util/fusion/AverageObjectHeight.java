package com.example.Real_time_Object_Detection.util.fusion;

public class AverageObjectHeight {
    public static float getAverageObjectHeight(String objectType) {
        float averageObjectHeight = 0.5f;

        switch (objectType.toLowerCase()) {
            case "person":
                averageObjectHeight = 1.6f;
                break;
            case "bicycle":
                averageObjectHeight = 0.6f;
                break;
            case "car":
                averageObjectHeight = 1.3f;
                break;
            case "motorcycle":
                averageObjectHeight = 1.2f;
                break;
            case "airplane":
                averageObjectHeight = 10.0f;
                break;
            case "bus":
                averageObjectHeight = 3.2f;
                break;
            case "train":
                averageObjectHeight = 4.1f;
                break;
            case "truck":
                averageObjectHeight = 3.6f;
                break;
            case "boat":
                averageObjectHeight = 3.5f;
                break;
            case "traffic light":
                averageObjectHeight = 0.9f;
                break;
            case "fire hydrant":
                averageObjectHeight = 0.75f;
                break;
            case "stop sign":
                averageObjectHeight = 0.5f;
                break;
            case "parking meter":
                averageObjectHeight = 1.25f;
                break;
            case "bench":
                averageObjectHeight = 0.45f;
                break;
            case "bird":
                averageObjectHeight = 0.25f;
                break;
            case "cat":
                averageObjectHeight = 0.25f;
                break;
            case "dog":
                averageObjectHeight = 0.6f;
                break;
            case "horse":
                averageObjectHeight = 1.6f;
                break;
            case "sheep":
                averageObjectHeight = 1.0f;
                break;
            case "cow":
                averageObjectHeight = 1.5f;
                break;
            case "elephant":
                averageObjectHeight = 2.7f;
                break;
            case "bear":
                averageObjectHeight = 1.2f;
                break;
            case "zebra":
                averageObjectHeight = 1.3f;
                break;
            case "giraffe":
                averageObjectHeight = 4.5f;
                break;
            case "backpack":
                averageObjectHeight = 0.4f;
                break;
            case "umbrella":
                averageObjectHeight = 1.0f;
                break;
            case "handbag":
                averageObjectHeight = 0.12f;
                break;
            case "tie":
                averageObjectHeight = 0.15f;
                break;
            case "suitcase":
                averageObjectHeight = 0.7f;
                break;
            case "frisbee":
                averageObjectHeight = 0.05f;
                break;
            case "skis":
                averageObjectHeight = 1.7f;
                break;
            case "snowboard":
                averageObjectHeight = 1.5f;
                break;
            case "sports ball":
                averageObjectHeight = 0.25f;
                break;
            case "kite":
                averageObjectHeight = 1.0f;
                break;
            case "baseball bat":
                averageObjectHeight = 0.85f;
                break;
            case "baseball glove":
                averageObjectHeight = 0.3f;
                break;
            case "skateboard":
                averageObjectHeight = 0.1f;
                break;
            case "surfboard":
                averageObjectHeight = 1.8f;
                break;
            case "tennis racket":
                averageObjectHeight = 0.7f;
                break;
            case "bottle":
                averageObjectHeight = 0.3f;
                break;
            case "wine glass":
                averageObjectHeight = 0.2f;
                break;
            case "cup":
                averageObjectHeight = 0.04f;
                break;
            case "fork":
                averageObjectHeight = 0.08f;
                break;
            case "knife":
                averageObjectHeight = 0.08f;
                break;
            case "spoon":
                averageObjectHeight = 0.07f;
                break;
            case "bowl":
                averageObjectHeight = 0.15f;
                break;
            case "banana":
                averageObjectHeight = 0.2f;
                break;
            case "apple":
                averageObjectHeight = 0.03f;
                break;
            case "sandwich":
                averageObjectHeight = 0.1f;
                break;
            case "orange":
                averageObjectHeight = 0.12f;
                break;
            case "broccoli":
                averageObjectHeight = 0.2f;
                break;
            case "carrot":
                averageObjectHeight = 0.15f;
                break;
            case "hot dog":
                averageObjectHeight = 0.1f;
                break;
            case "pizza":
                averageObjectHeight = 0.05f;
                break;
            case "donut":
                averageObjectHeight = 0.1f;
                break;
            case "cake":
                averageObjectHeight = 0.1f;
                break;
            case "chair":
                averageObjectHeight = 0.85f;
                break;
            case "couch":
                averageObjectHeight = 0.75f;
                break;
            case "potted plant":
                averageObjectHeight = 0.5f;
                break;
            case "bed":
                averageObjectHeight = 0.6f;
                break;
            case "dining table":
                averageObjectHeight = 0.75f;
                break;
            case "toilet":
                averageObjectHeight = 0.45f;
                break;
            case "tv":
                averageObjectHeight = 0.9f;
                break;
            case "laptop":
                averageObjectHeight = 0.5f;
                break;
            case "mouse":
                averageObjectHeight = 0.04f;
                break;
            case "remote":
                averageObjectHeight = 0.04f;
                break;
            case "keyboard":
                averageObjectHeight = 0.1f;
                break;
            case "cell phone":
                averageObjectHeight = 0.1f;
                break;
            case "microwave":
                averageObjectHeight = 0.4f;
                break;
            case "oven":
                averageObjectHeight = 0.9f;
                break;
            case "toaster":
                averageObjectHeight = 0.2f;
                break;
            case "sink":
                averageObjectHeight = 0.9f;
                break;
            case "refrigerator":
                averageObjectHeight = 1.7f;
                break;
            case "book":
                averageObjectHeight = 0.2f;
                break;
            case "clock":
                averageObjectHeight = 0.25f;
                break;
            case "vase":
                averageObjectHeight = 0.4f;
                break;
            case "scissors":
                averageObjectHeight = 0.1f;
                break;
            case "teddy bear":
                averageObjectHeight = 0.3f;
                break;
            case "hair drier":
                averageObjectHeight = 0.25f;
                break;
            case "toothbrush":
                averageObjectHeight = 0.2f;
                break;
            default:
                averageObjectHeight = 0.5f;
                break;
        }
        return averageObjectHeight;
    }

}
