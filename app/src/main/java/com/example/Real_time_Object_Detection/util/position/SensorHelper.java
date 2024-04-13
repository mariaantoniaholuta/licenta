package com.example.Real_time_Object_Detection.util.position;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class SensorHelper implements SensorEventListener {

    private TextView positionStatusTextView;
    private VibrationHelper vibrationHelper;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private float[] orientationAngles = new float[3];

    private float[] gyroscopeData = new float[3];

    private static final float ALPHA = 0.98f;

    private static final float FILTER_COEFFICIENT = 0.8f;
    public static final float VERTICAL_THRESHOLD_DEGREES = 25.0f;
    public static final float PITCH_UPRIGHT_THRESHOLD_DEGREES = 22.0f;

    public interface SensorListener {
        void onOrientationChanged(float azimuth, float pitch, float roll);
    }

    public SensorHelper(Context context, TextView positionStatusTextView, VibrationHelper vibrationHelper) {
        this.positionStatusTextView = positionStatusTextView;
        this.vibrationHelper = vibrationHelper;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = applyLowPassFilter(event.values.clone(), gravity);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = applyLowPassFilter(event.values.clone(), geomagnetic);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeData, 0, gyroscopeData.length);
                break;
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(R, null, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);

                // Azimuth
                float azimuth = (float) Math.toDegrees(orientation[0]);
                // Pitch - phone should be vertical
                float pitch = (float) -Math.toDegrees(orientation[1]);
                // Roll - phone should not have a rotation around Y axis
                float roll = (float) Math.toDegrees(orientation[2]); // Roll

                updatePositionStatusBasedOnOrientation(pitch, roll, azimuth);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private float[] applyLowPassFilter(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + FILTER_COEFFICIENT * (input[i] - output[i]);
        }
        return output;
    }

    private void updatePositionStatusBasedOnOrientation(float pitch, float roll, float azimuth) {
        //boolean isHorizontal = (Math.abs(pitch) < HORIZONTAL_THRESHOLD_DEGREES);
        boolean isHorizontal = (pitch <= -90 + PITCH_UPRIGHT_THRESHOLD_DEGREES) || (pitch >= 90 - PITCH_UPRIGHT_THRESHOLD_DEGREES);
        boolean isVertical = (Math.abs(roll) < VERTICAL_THRESHOLD_DEGREES);
        float normalAzimuth = 95f;
        float normalPitch = 85f;
        float normalRoll = 5f;

        float azimuthDeviation = Math.abs(azimuth - normalAzimuth);
        float pitchDeviation = Math.abs(pitch - normalPitch);
        float rollDeviation = Math.abs(roll - normalRoll);

        float totalDeviation = azimuthDeviation + pitchDeviation + rollDeviation;

        String statusText;
        int color;

        if (isHorizontal && isVertical) {
            statusText = String.format("Position OK\nAzimuth: %.2f°\nPitch: %.2f°\nRoll: %.2f°", azimuth, pitch, roll);
            color = Color.GREEN;
        } else {
            statusText = String.format("Adjust Position\nAzimuth: %.2f°\nPitch: %.2f°\nRoll: %.2f°", azimuth, pitch, roll);
            color = Color.RED;
            vibrationHelper.vibrateBasedOnDeviation(totalDeviation);
        }

        updatePositionStatus(statusText, color);
    }

    private void updatePositionStatus(String text, int color) {
        positionStatusTextView.post(new Runnable() {
            @Override
            public void run() {
                positionStatusTextView.setText(text);
                positionStatusTextView.setTextColor(color);
            }
        });
    }
}
