package com.example.Real_time_Object_Detection.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtil {

    public static Bitmap rotate(Bitmap source, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

    public static Bitmap flip(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap resize(Bitmap src, int targetWidth, int targetHeight) {
        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, false);
    }

    public static Bitmap floatArrayToBitmap(float[] depthArray, int width, int height) {
        Bitmap depthBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] depthPixels = new int[depthArray.length];
        for (int i = 0; i < depthArray.length; i++) {
            int depthValue = (int) (255 * depthArray[i]);
            // ARGB pixel with depth value for R, G și B
            depthPixels[i] = 0xFF000000 | (depthValue << 16) | (depthValue << 8) | depthValue;
        }

        depthBitmap.setPixels(depthPixels, 0, width, 0, 0, width, height);

        return depthBitmap;
    }

    private static float getMinValue(float[] floatArray) {
        float minValue = floatArray[0];
        for (float v : floatArray) {
            if (v < minValue) {
                minValue = v;
            }
        }
        return minValue;
    }

    private static float getMaxValue(float[] floatArray) {
        float maxValue = floatArray[0];
        for (float v : floatArray) {
            if (v > maxValue) {
                maxValue = v;
            }
        }
        return maxValue;
    }
}
