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
        int minDepthValue = 255;
        int minX = 0, minY  = 255;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelDepthValue = getDepthValueAtPoint(croppedDepthMap, objectBitMap, x, y);
                if (pixelDepthValue < minDepthValue) {
                    minDepthValue = pixelDepthValue;
                    minX = x;
                    minY = y;
                }
            }
        }
        Log.d("min depth pixel", String.valueOf(minDepthValue));

        return normalizeDepthValue(minDepthValue);
    }

    public int getDepthValueAtPoint(Bitmap depthMap, Bitmap objectBitMap, int X, int Y) {

        float scaleX = (float) depthMap.getWidth() / objectBitMap.getWidth();
        float scaleY = (float) depthMap.getHeight() / objectBitMap.getHeight();

        int scaledCenterX = (int) (X * scaleX);
        int scaledCenterY = (int) (Y * scaleY);

        Log.d("Scaled locationX", String.valueOf(scaledCenterX));
        Log.d("Scaled locationY", String.valueOf(scaledCenterY));
        int depthValue = 0;

        if (scaledCenterX >= 0 && scaledCenterX < depthMap.getWidth() && scaledCenterY >= 0 && scaledCenterY < depthMap.getHeight()) {
            depthValue = depthMap.getPixel(scaledCenterX, scaledCenterY);
            Log.d("depthValue-ARGB", String.valueOf(depthValue));
        } else {
            Log.d("depthMap", "Scaled coordinates out of bounds");
        }
        return depthValue;
    }

    public float normalizeDepthValue(int depthValue) {

        int redValue = (depthValue >> 16) & 0xFF;
        float depthRangeMax = 10f;

        return (redValue / 255.0f) * depthRangeMax;
    }
}