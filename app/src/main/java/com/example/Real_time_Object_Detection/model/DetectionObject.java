package com.example.Real_time_Object_Detection.model;

import android.graphics.Rect;

public class DetectionObject {
    Rect boundingBox;
    String label;
    float score;
    int id;

    float depth;

    public DetectionObject(Rect boundingBox, String label, float score, float depth, int id) {
        this.boundingBox = boundingBox;
        this.label = label;
        this.score = score;
        this.id = id;
        this.depth = depth;
    }

    public double distanceTo(DetectionObject other) {
        double centerXThis = this.boundingBox.centerX();
        double centerYThis = this.boundingBox.bottom - this.boundingBox.height()/ 2.0;
        double centerXOther = other.boundingBox.centerX();
        double centerYOther = other.boundingBox.bottom - other.boundingBox.height() / 2.0;

        return Math.sqrt(Math.pow(centerXThis - centerXOther, 2) + Math.pow(centerYThis - centerYOther, 2));
    }

    public void update(DetectionObject newObj) {
        this.boundingBox = newObj.getBoundingBox();
        this.label = newObj.getLabel();
        this.score = newObj.getScore();
    }

    public double calculateDistanceBetweenObject(DetectionObject obj2) {
        // euclidian distance
        int centerXA = this.getBoundingBox().centerX();
        int centerYA = this.getBoundingBox().centerY();
        int centerXB = obj2.getBoundingBox().centerX();
        int centerYB = obj2.getBoundingBox().centerY();

        return Math.sqrt(Math.pow(centerXA - centerXB, 2) + Math.pow(centerYA - centerYB, 2));
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(Rect boundingBox) {
        this.boundingBox = boundingBox;
    }

    public String getLabel() {
        return label;
    }

    public float getDepth() {
        return depth;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

