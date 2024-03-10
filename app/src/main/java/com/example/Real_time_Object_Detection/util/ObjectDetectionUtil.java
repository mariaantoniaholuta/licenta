package com.example.Real_time_Object_Detection.util;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ObjectDetectionUtil {

    public static ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    public static ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap, int inputSize) {
        ByteBuffer byteBuffer;
        int quant = 0;
        int size_images = inputSize;

        if (quant == 0) {
            byteBuffer = ByteBuffer.allocateDirect(1 * size_images * size_images * 3);

        } else {
            byteBuffer = ByteBuffer.allocateDirect(4 * 1 * size_images * size_images * 3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] number_Pixels = new int[size_images * size_images];
        //
        scaledBitmap.getPixels(number_Pixels, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        int pixels = 0;
        for (int i = 0; i < size_images; i++) {
            for (int j = 0; j < size_images; j++) {
                final int val = number_Pixels[pixels++];
                if (quant == 0) {
                    byteBuffer.put((byte) ((val >> 16) & 0xFF));
                    byteBuffer.put((byte) ((val >> 8) & 0xFF));
                    byteBuffer.put((byte) (val & 0xFF));
                } else {
                    byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val) & 0xFF)) / 255.0f);
                }
            }

        }
        return byteBuffer;
    }
}