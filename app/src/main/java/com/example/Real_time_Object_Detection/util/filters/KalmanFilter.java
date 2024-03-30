package com.example.Real_time_Object_Detection.util.filters;

public class KalmanFilter {
    // Q - noise process, R - noise to measure, A - state transitional factor
    // B - in control factor, C - measurement factor
    private float Q = 0.1f;
    private float R = 0.2f;
    private float A = 1;
    private float B = 0;
    private float C = 1;

    private Float stateEstimate;
    private float errorCovariance;
    private float kalmanGain;

    public KalmanFilter(float initialState, float initialErrorCovariance) {
        this.stateEstimate = initialState;
        this.errorCovariance = initialErrorCovariance;
    }

    public float update(float measurement) {
        stateEstimate = A * stateEstimate + B;
        errorCovariance = A * A * errorCovariance + Q;

        kalmanGain = errorCovariance * C / (C * C * errorCovariance + R);
        stateEstimate = stateEstimate + kalmanGain * (measurement - C * stateEstimate);
        errorCovariance = (1 - kalmanGain * C) * errorCovariance;

        return stateEstimate;
    }
}
