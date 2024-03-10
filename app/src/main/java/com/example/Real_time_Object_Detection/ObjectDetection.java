package com.example.Real_time_Object_Detection;

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

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.Real_time_Object_Detection.util.ObjectDetectionUtil;
import com.example.Real_time_Object_Detection.util.DepthMapUtil;

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

        interpreter = new Interpreter(ObjectDetectionUtil.loadModelFile(assetManger, modelPath), options);
        labelList = ObjectDetectionUtil.loadLabelList(assetManger, labelPath);
    }

    // create new Mat function

    public Mat recongizeImageObjects(Mat mat_image) {
        Mat rotated_image = new Mat();
        Core.flip(mat_image.t(), rotated_image, 1);

        Bitmap objectBitMap = Bitmap.createBitmap(rotated_image.cols(), rotated_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_image,objectBitMap);

        height = objectBitMap.getHeight();
        width = objectBitMap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(objectBitMap, inputSize, inputSize, false);

        ByteBuffer byteBuffer = ObjectDetectionUtil.convertBitmapToByteBuffer(scaledBitmap, inputSize);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        Map<Integer,Object> output_map = new TreeMap<>();
        int maxNumberOfObjects = 10;

        float [][][] boxes = new float[1][maxNumberOfObjects][4];

        float[][] classes = new float [1][maxNumberOfObjects];

        float[][] score = new float [1][maxNumberOfObjects];

        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,score);

        // prediction
        interpreter.runForMultipleInputsOutputs(input,output_map);

        Object object_boxes = output_map.get(0);
        Object object_classes = output_map.get(1);
        Object object_score = output_map.get(2);

        for (int i = 0; i < maxNumberOfObjects; i++){
            float detectedClass = (float) Array.get(Array.get(object_classes,0),i);
            float probabilityScore= (float) Array.get(Array.get(object_score,0),i);
            if(probabilityScore > 0.6){
                Object value_box1 = (Object) Array.get(Array.get(object_boxes,0),i);

                float top =(float) Array.get(value_box1,0)*height;
                float bottom =(float) Array.get(value_box1,2)*height;

                float left =(float) Array.get(value_box1,1)*width;
                float right =(float) Array.get(value_box1,3)*width;

                Imgproc.rectangle(rotated_image,new Point(left,top),new Point(right,bottom),new Scalar(255,155,155),2);

                String label = labelList.get((int)detectedClass);
                String annotation = String.format("%s:, P: %.2f", label,  probabilityScore);

                Imgproc.putText(rotated_image, annotation, new Point(left, top), 3, 1, new Scalar(100,100,100), 2);
            }
        }

        Core.flip(rotated_image.t(), mat_image, 0);
        return mat_image;
    }

    public Mat recongizeImageObjectsAndDepth(Mat mat_image, Bitmap depthMap) {
        Mat rotated_image = new Mat();
        DepthMapUtil depthMapUtil = new DepthMapUtil();
        Core.flip(mat_image.t(), rotated_image, 1);

        Bitmap objectBitMap = Bitmap.createBitmap(rotated_image.cols(), rotated_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_image,objectBitMap);

        height = objectBitMap.getHeight();
        width = objectBitMap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(objectBitMap, inputSize, inputSize, false);
        ByteBuffer byteBuffer = ObjectDetectionUtil.convertBitmapToByteBuffer(scaledBitmap, inputSize);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        Map<Integer,Object> output_map = new TreeMap<>();
        int maxNumberOfObjects = 10;

        float [][][] boxes = new float[1][maxNumberOfObjects][4];
        float[][] classes = new float [1][maxNumberOfObjects];
        float[][] score = new float [1][maxNumberOfObjects];

        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,score);

        interpreter.runForMultipleInputsOutputs(input,output_map);

        Object object_boxes = output_map.get(0);
        Object object_classes = output_map.get(1);
        Object object_score = output_map.get(2);

        for (int i = 0; i < maxNumberOfObjects; i++){
            float detectedClass = (float) Array.get(Array.get(object_classes,0),i);
            float probabilityScore= (float) Array.get(Array.get(object_score,0),i);
            if(probabilityScore > 0.6){
                Object value_box1 = (Object) Array.get(Array.get(object_boxes,0),i);

                float top =(float) Array.get(value_box1,0)*height;
                float bottom =(float) Array.get(value_box1,2)*height;

                float left =(float) Array.get(value_box1,1)*width;
                float right =(float) Array.get(value_box1,3)*width;

                Imgproc.rectangle(rotated_image,new Point(left,top),new Point(right,bottom),new Scalar(255,155,155),2);

                int centerX = (int) ((left + right) / 2);
                int centerY = (int) ((top + bottom) / 2);

                Rect objectRect = depthMapUtil.scaleRectToDepthMap(new Rect((int)left, (int)top, (int)right, (int)bottom), depthMap.getWidth(), depthMap.getHeight(), mat_image.width(), mat_image.height());

                Bitmap croppedDepthMap = depthMapUtil.safeCreateCroppedBitmap(depthMap, objectRect);
                float objectClosestDepth = 10;

                if(croppedDepthMap != null){
                    objectClosestDepth = depthMapUtil.analyzeCroppedDepthMap(croppedDepthMap, objectBitMap);
                }

                int depthValue = depthMapUtil.getDepthValueAtPoint(depthMap, objectBitMap,centerX, centerY);
                float normalizedDepthValue = depthMapUtil.normalizeDepthValue(depthValue);

                String label = labelList.get((int)detectedClass);

                String annotation = String.format("%s:, Depth: %.2f", label,  objectClosestDepth);
                Log.d("depthcalculated", String.valueOf(depthValue));
                Imgproc.putText(rotated_image, annotation, new Point(left, top), 3, 1, new Scalar(100,100,100), 2);
            }
        }

        Core.flip(rotated_image.t(), mat_image, 0);
        return mat_image;
    }
}
