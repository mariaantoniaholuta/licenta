package com.example.Real_time_Object_Detection.util.position;

import static com.example.Real_time_Object_Detection.util.position.SensorHelper.PITCH_UPRIGHT_THRESHOLD_DEGREES;
import static com.example.Real_time_Object_Detection.util.position.SensorHelper.VERTICAL_THRESHOLD_DEGREES;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.util.Arrays;

public class VibrationHelper {
    private Vibrator vibrator;
    private final int MAX_INTENSITY = 255;

    float maxExpectedDeviation = 100.0f;

    float scalingFactor = 254.0f / maxExpectedDeviation;

    public VibrationHelper(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void vibrateBasedOnDeviation(float totalDeviation) {
        int intensity = calculateIntensityBasedOnTotalDeviation(totalDeviation);
        Log.d("VibrationHelper", "totalDeviation: " + totalDeviation);
        Log.d("VibrationHelper", "Vibrating with intensity: " + intensity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, intensity));
        } else {
            vibrator.vibrate(100);
        }
    }

    private int calculateIntensityBasedOnTotalDeviation(float totalDeviation) {
        int adjustedIntensity = (int) (Math.abs(totalDeviation));
        return Math.min(adjustedIntensity, 255);
    }

    private int calculateIntensity(float totalDeviation) {
        int intensity = (int) (VibrationEffect.DEFAULT_AMPLITUDE + totalDeviation * 2);
        intensity = Math.min(intensity, MAX_INTENSITY);
        intensity = Math.max(intensity, 0);

        return intensity;
    }


    private int getAmplitudeForVersion(int intensity) {
      //to convert intensity to the [0, 1] range
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return intensity > 0 ? 1 : 0;
        }
        // else with[0, 255] range directly
        return intensity;
    }

}
