package com.example.Real_time_Object_Detection.util.tracker;

import org.opencv.core.Scalar;

import java.util.HashMap;
import java.util.Map;
public class ColorManager {
    private static final Scalar[] COLORS = {
            new Scalar(0,255,0),
            new Scalar(0,0,255),
            new Scalar(255,255,0),
            new Scalar(255,0,255),
            new Scalar(0,255,255),
            new Scalar(192,192,192),
            new Scalar(128,0,128),
            new Scalar(128,128,0),
            new Scalar(0,128,128),
            new Scalar(0,128,0),
            new Scalar(128,0,0),
            new Scalar(255,127,80)
    };

    private Map<Integer, Scalar> idToColorMap = new HashMap<>();

    public Scalar getColorForId(int id) {
        if (!idToColorMap.containsKey(id)) {
            idToColorMap.put(id, COLORS[id % COLORS.length]);
        }
        return idToColorMap.get(id);
    }
}
