package com.example.Real_time_Object_Detection;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ObjectDetection {
    private  final List<String> labelList;
    private final Interpreter interpreter;
    private final int inputSize;
    private final GpuDelegate gpuDelegate;
    private static final int NUM_THREADS = 4;

    private int height;
    private int width;

    ObjectDetection(int input_Size, AssetManager assetManger, String modelPath, String labelPath) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        this.gpuDelegate = new GpuDelegate();
        this.inputSize = input_Size;
        options.addDelegate(gpuDelegate);
        options.setNumThreads(NUM_THREADS);

        interpreter = new Interpreter(loadModelFile(assetManger, modelPath), options);
        labelList = loadLabelList(assetManger, labelPath);

    }

    private List<String> loadLabelList(AssetManager assetManger, String labelPath) throws IOException {

        // use to store Label
        List<String> labelList = new ArrayList<>();
        // use to create a new reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManger.open(labelPath)));
        String line;

        //loop through each line and store it to LabelList

        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManger, String modelPath) throws IOException {

        // use to get description of the file
        AssetFileDescriptor fileDescriptor = assetManger.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // create new Mat function

    public Mat recongizeImageObjects(Mat mat_image) {
        // rotate original image by 90 degree to get potrait mode
        Mat rotated_image = new Mat();
        Core.flip(mat_image.t(), rotated_image, 1);

        // now convert it to bitmap
        Bitmap objectBitMap = Bitmap.createBitmap(rotated_image.cols(), rotated_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_image,objectBitMap);

        //define height and width
        height = objectBitMap.getHeight();
        width = objectBitMap.getWidth();

        //Scale the bitmap image to the inputSize of the model
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(objectBitMap, inputSize, inputSize, false);

        //now define a method that convert the Scaled bitmap to bufferByte as the model input should be in it
        ByteBuffer byteBuffer = ConvertBitmapToByteBuffer(scaledBitmap);

        //store input ByteBuffer in arrayObject of size 1;
        Object[] input = new Object[1];
        input[0] = byteBuffer;

        // defineing the output by treemap of three arrays (boxes,classes,score)
        Map<Integer,Object> output_map = new TreeMap<>();
        int maxNumberOfObjects = 10;

        float [][][] boxes = new float[1][maxNumberOfObjects][4];

        // store classes of 10 objects
        float[][] classes = new float [1][maxNumberOfObjects];

        // store score of 10 objects
        float[][] score = new float [1][maxNumberOfObjects];

        //add it to object_map
        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,score);

        // now predict
        interpreter.runForMultipleInputsOutputs(input,output_map);

        // we will get the output from model and then we draw boxes
        Object object_boxes = output_map.get(0);
        Object object_classes = output_map.get(1);
        Object object_score = output_map.get(2);

        //loop through each object
        //as ouput has only 10 boxes
        for (int i = 0; i < maxNumberOfObjects; i++){
            //Object value_box = (float) Array.get(Array.get(object_boxes,0),i);
            float detectedClass = (float) Array.get(Array.get(object_classes,0),i);
            float probabilityScore= (float) Array.get(Array.get(object_score,0),i);
            if(probabilityScore > 0.6){
                Object value_box1 = (Object) Array.get(Array.get(object_boxes,0),i);

                // mulltiplying by Original Frame
                float top =(float) Array.get(value_box1,0)*height;
                float left =(float) Array.get(value_box1,1)*width;
                float bottom =(float) Array.get(value_box1,2)*height;
                float right =(float) Array.get(value_box1,3)*width;

                Imgproc.rectangle(rotated_image,new Point(left,top),new Point(right,bottom),new Scalar(255,155,155),2);

                int centerX = (int) ((left + right) / 2);
                int centerY = (int) ((top + bottom) / 2);

                // Annotate the detected object with its label, score, and depth value
                String label = labelList.get((int)detectedClass);
                String annotation = String.format("%s:, P: %.2f", label,  probabilityScore);

                Imgproc.putText(rotated_image, annotation, new Point(left, top), 3, 1, new Scalar(100,100,100), 2);
            }
        }

        // before returning rotate it back by -90 degree
        Core.flip(rotated_image.t(), mat_image, 0);
        return mat_image;
    }

    public Mat recongizeImageObjectsAndDepth(Mat mat_image, Bitmap depthMap) {
        // rotate original image by 90 degree to get potrait mode
        Mat rotated_image = new Mat();
        Core.flip(mat_image.t(), rotated_image, 1);

        // now convert it to bitmap
        Bitmap objectBitMap = Bitmap.createBitmap(rotated_image.cols(), rotated_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_image,objectBitMap);

        //define height and width
        height = objectBitMap.getHeight();
        width = objectBitMap.getWidth();

        //Scale the bitmap image to the inputSize of the model
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(objectBitMap, inputSize, inputSize, false);

        //now define a method that convert the Scaled bitmap to bufferByte as the model input should be in it
        ByteBuffer byteBuffer = ConvertBitmapToByteBuffer(scaledBitmap);

        //store input ByteBuffer in arrayObject of size 1;
        Object[] input = new Object[1];
        input[0] = byteBuffer;

        // defineing the output by treemap of three arrays (boxes,classes,score)
        Map<Integer,Object> output_map = new TreeMap<>();
        int maxNumberOfObjects = 10;

        float [][][] boxes = new float[1][maxNumberOfObjects][4];

        // store classes of 10 objects
        float[][] classes = new float [1][maxNumberOfObjects];

        // store score of 10 objects
        float[][] score = new float [1][maxNumberOfObjects];

        //add it to object_map
        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,score);

        // now predict
        interpreter.runForMultipleInputsOutputs(input,output_map);

        // we will get the output from model and then we draw boxes
        Object object_boxes = output_map.get(0);
        Object object_classes = output_map.get(1);
        Object object_score = output_map.get(2);

        //loop through each object
        //as ouput has only 10 boxes
        for (int i = 0; i < maxNumberOfObjects; i++){
            //Object value_box = (float) Array.get(Array.get(object_boxes,0),i);
            float detectedClass = (float) Array.get(Array.get(object_classes,0),i);
            float probabilityScore= (float) Array.get(Array.get(object_score,0),i);
            if(probabilityScore > 0.6){
                Object value_box1 = (Object) Array.get(Array.get(object_boxes,0),i);

                // mulltiplying by Original Frame
                float top =(float) Array.get(value_box1,0)*height;
                float left =(float) Array.get(value_box1,1)*width;
                float bottom =(float) Array.get(value_box1,2)*height;
                float right =(float) Array.get(value_box1,3)*width;

                Imgproc.rectangle(rotated_image,new Point(left,top),new Point(right,bottom),new Scalar(255,155,155),2);

                int centerX = (int) ((left + right) / 2);
                int centerY = (int) ((top + bottom) / 2);

                Rect objectRect = scaleRectToDepthMap(new Rect((int)left, (int)top, (int)right, (int)bottom), depthMap.getWidth(), depthMap.getHeight(), mat_image.width(), mat_image.height());

                // Crop the depth map to the object's rectangle
                Bitmap croppedDepthMap = safeCreateCroppedBitmap(depthMap, objectRect);
                float objectClosestDepth = 10;

                // cropped depth map to find the depth value
                if(croppedDepthMap != null){
                    objectClosestDepth = analyzeCroppedDepthMap(croppedDepthMap, objectBitMap);
                }

                int depthValue = getDepthValueAtPoint(depthMap, objectBitMap,centerX, centerY);
                float normalizedDepthValue = normalizeDepthValue(depthValue);

                // Annotate the detected object with its label, score, and depth value
                String label = labelList.get((int)detectedClass);

                String annotation = String.format("%s:, Depth: %.2f", label,  objectClosestDepth);
                Log.d("depthcalculated", String.valueOf(depthValue));
                Imgproc.putText(rotated_image, annotation, new Point(left, top), 3, 1, new Scalar(100,100,100), 2);
            }
        }

        // before returning rotate it back by -90 degree
        Core.flip(rotated_image.t(), mat_image, 0);
        return mat_image;
    }

    private Bitmap safeCreateCroppedBitmap(Bitmap depthMap, Rect objectRect) {
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

    private Rect scaleRectToDepthMap(Rect objectRect, int depthMapWidth, int depthMapHeight, int originalWidth, int originalHeight) {
        float scaleX = (float) depthMapWidth / originalWidth;
        float scaleY = (float) depthMapHeight / originalHeight;

        int scaledLeft = Math.round(objectRect.left * scaleX);
        int scaledTop = Math.round(objectRect.top * scaleY);
        int scaledRight = Math.round(objectRect.right * scaleX);
        int scaledBottom = Math.round(objectRect.bottom * scaleY);

        return new Rect(scaledLeft, scaledTop, scaledRight, scaledBottom);
    }

    private float analyzeCroppedDepthMap(Bitmap croppedDepthMap,  Bitmap objectBitMap) {
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

        // Normalize the minimum depth value to a range or scale as needed
        return normalizeDepthValue(minDepthValue);
    }

    private int getDepthValueAtPoint(Bitmap depthMap, Bitmap objectBitMap, int X, int Y) {

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

    private float normalizeDepthValue(int depthValue) {

        int redValue = (depthValue >> 16) & 0xFF;
        float depthRangeMax = 10f;

        return (redValue / 255.0f) * depthRangeMax;
    }

    private ByteBuffer ConvertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        // some model input should be quant=0 and for some quant=0
        // for this model quant=0
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
