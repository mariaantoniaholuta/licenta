package com.example.Real_time_Object_Detection.util.warnings_environment;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.Real_time_Object_Detection.model.DetectionObject;

import java.util.List;
import java.util.Objects;

public class Environment {

    private TextToSpeech tts;
    private boolean spoke = false;
    private static Environment instance;

    private static final long SPEAK_INTERVAL_MS = 5000;

    private long lastSpeakTime = 0;

    public static StringBuilder speakText = new StringBuilder("Detected ");

    public static String speakTop3Text = "Environment: ";

    public Environment(TextToSpeech tts) {
        this.tts = tts;
    }

    public static Environment getInstance(TextToSpeech tts) {
        if (instance == null) {
            instance = new Environment(tts);
        }
        return instance;
    }

    public void topThreeObjects(List<DetectionObject> objects, int imageWidth) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            objects.sort((o1, o2) -> {
                int scoreCompare = Float.compare(o2.getScore(), o1.getScore());
                if (scoreCompare != 0) return scoreCompare;
                return Float.compare(o1.getDepth(), o2.getDepth());
            });
        }

        speakTop3Text = "Environment: ";
        int count = Math.min(objects.size(), 3);
        for (int i = 0; i < count; i++) {
            DetectionObject obj = objects.get(i);
            int centerX = obj.getBoundingBox().centerX();

            String position;
            if (centerX < imageWidth / 3) {
                position = " to the left ";
            } else if (centerX > 2 * imageWidth / 3) {
                position = " to the right ";
            } else {
                position = " in front of you ";
            }

            if(!speakTop3Text.equals("Environment: ")) {
                speakTop3Text += ", ... and " + obj.getLabel() + obj.getDepth() + " meters " + position;
            } else {
                speakTop3Text += obj.getLabel() + obj.getDepth() + " meters " + position;
            }
        }
    }

    public void shutdownTTS() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void speakOut() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpeakTime > SPEAK_INTERVAL_MS && !spoke) {
            if (tts != null) {
                if (!Objects.equals(speakTop3Text, "")) {
                    tts.speak(speakTop3Text, TextToSpeech.QUEUE_FLUSH, null, null);
                    lastSpeakTime = currentTime;
                    spoke = true;
                }
            }
        }
    }

    public void resetSpoke() {
        spoke = false;
    }
}
