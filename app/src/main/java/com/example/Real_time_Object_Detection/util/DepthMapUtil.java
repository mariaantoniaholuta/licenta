package com.example.Real_time_Object_Detection.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

public class DepthMapUtil {

    public static Bitmap byteBufferToBitmap(float[] imageArray, int imageDim) {
        Bitmap bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.RGB_565);
        for (int i = 0; i < imageDim; i++) {
            for (int j = 0; j < imageDim; j++) {
                int p = (int) imageArray[i * imageDim + j];
                bitmap.setPixel(j, i, Color.rgb(p, p, p));
            }
        }
        return bitmap;
    }
    public Bitmap safeCreateCroppedBitmap(Bitmap depthMap, Rect objectRect) {
        int clampedLeft = Math.max(0, objectRect.left);
        int clampedTop = Math.max(0, objectRect.top);
        int clampedRight = Math.min(depthMap.getWidth(), objectRect.right);
        int clampedBottom = Math.min(depthMap.getHeight(), objectRect.bottom);

        int clampedWidth = clampedRight - clampedLeft;
        int clampedHeight = clampedBottom - clampedTop;

        if (clampedWidth > 0 && clampedHeight > 0) {
            return Bitmap.createBitmap(depthMap, clampedLeft, clampedTop, clampedWidth, clampedHeight);
        } else {
            return null;
        }
    }

    public Rect scaleRectToDepthMap(Rect objectRect, int depthMapWidth, int depthMapHeight, int originalWidth, int originalHeight) {
        float scaleX = (float) depthMapWidth / originalWidth;
        float scaleY = (float) depthMapHeight / originalHeight;

        int scaledLeft = Math.round(objectRect.left * scaleX);
        int scaledTop = Math.round(objectRect.top * scaleY);
        int scaledRight = Math.round(objectRect.right * scaleX);
        int scaledBottom = Math.round(objectRect.bottom * scaleY);

        return new Rect(scaledLeft, scaledTop, scaledRight, scaledBottom);
    }

    public float analyzeCroppedDepthMap(Bitmap croppedDepthMap,  Bitmap objectBitMap) {
        int width = croppedDepthMap.getWidth();
        int height = croppedDepthMap.getHeight();
        long sumDepthValue = 0;
        int count = 0;
        float result = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelDepthValue = getDepthValueAtPoint(croppedDepthMap, objectBitMap, x, y);
                sumDepthValue += pixelDepthValue;
                count++;
            }
        }
        if (count > 0) {
            float averageDepthValue = sumDepthValue / (float) count;
            Log.d("average depth pixel", String.valueOf(averageDepthValue));
            result = estimateDepthInMeters(averageDepthValue);

            if(result > 0) {
                return result;
            }
        }
        return 0;
    }

    public float analyzeMaxCroppedDepthMap(Bitmap croppedDepthMap,  Bitmap objectBitMap) {
        int width = croppedDepthMap.getWidth();
        int height = croppedDepthMap.getHeight();
        int maxDepthValue = 0;
        int maxX = 0, maxY  = 0;
        float result = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelDepthValue = getDepthValueAtPoint(croppedDepthMap, objectBitMap, x, y);
                if (pixelDepthValue > maxDepthValue) {
                    maxDepthValue = pixelDepthValue;
                    maxX = x;
                    maxY = y;
                }
            }
        }
        result = estimateDepthInMeters(maxDepthValue);
        Log.d("max depth pixel", String.valueOf(maxDepthValue));
        Log.d("norm depth in meters", String.valueOf(result));

        if(result > 0) {
            return result;
        }

        return 0;
    }

    public int getDepthValueAtPoint(Bitmap depthMap, Bitmap objectBitMap, int X, int Y) {

        float scaleX = (float) depthMap.getWidth() / objectBitMap.getWidth();
        float scaleY = (float) depthMap.getHeight() / objectBitMap.getHeight();

        int scaledCenterX = (int) (X * scaleX);
        int scaledCenterY = (int) (Y * scaleY);

        int depthValue = 0;

        if (scaledCenterX >= 0 && scaledCenterX < depthMap.getWidth() && scaledCenterY >= 0 && scaledCenterY < depthMap.getHeight()) {
            int depthPixel = depthMap.getPixel(scaledCenterX, scaledCenterY);
            depthValue = Color.red(depthPixel);
        } else {
            Log.d("depthMap", "Scaled coordinates out of bounds");
            return -1;
        }
        return depthValue;
    }

    public float normalizeDepthValue(int depthValue) {
        float depthRangeMax = 20f;
        return (depthValue / 255.0f) * depthRangeMax;
    }

    public float estimateDepthInMeters(float depthValue) {
        float maxDepthRangeInMeters = 10.0f;
        int depthToMeteresRatio = 2;
        float normalizedDepth = depthValue / 255.0f;
        float distanceInMeters = (1.0f - normalizedDepth) * maxDepthRangeInMeters - depthToMeteresRatio;

        return distanceInMeters;
    }

}