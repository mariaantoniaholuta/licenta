package com.example.Real_time_Object_Detection;

import static com.example.Real_time_Object_Detection.util.tracker.TrackedObject.drawBoundingBoxes;
import static com.example.Real_time_Object_Detection.util.tracker.TrackedObject.isVehicle;
import static com.example.Real_time_Object_Detection.util.tracker.TrackedObject.updateTrackedObjects;
import static com.example.Real_time_Object_Detection.util.formats.ValuesExtracter.getAdjustedDistanceValue;
import static com.example.Real_time_Object_Detection.util.warnings_environment.WarningText.generateWarnings;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.speech.tts.TextToSpeech;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.Real_time_Object_Detection.model.DetectionObject;
import com.example.Real_time_Object_Detection.util.ObjectDetectionUtil;
import com.example.Real_time_Object_Detection.util.warnings_environment.Environment;
import com.example.Real_time_Object_Detection.util.warnings_environment.TrafficLight;

public class ImageRecognition {
    private final List<String> categories;
    private final Interpreter tfliteInterpreter;
    private final int imageSize;
    private final GpuDelegate delegateGPU;
    private static final int THREAD_COUNT = 4;

    private int picHeight;
    private int picWidth;

    List<DetectionObject> trackedObjects = new ArrayList<>();
    int nextObjectId = 1;

    private TextToSpeech tts;

    private Environment environment;

    private boolean spoke = false;

    public static TrafficLight.TrafficLightColor lastSpokenColor = null;

    public interface OnDepthWarningListener {
        void onDepthWarningUpdate(String warningMessage);
    }

    private static DepthRecognition.OnDepthWarningListener listener;


    ImageRecognition(int targetSize, AssetManager assets, String modelFile, String labelsFile, DepthRecognition.OnDepthWarningListener listener, TextToSpeech tts, Environment environment) throws IOException {
        Interpreter.Options interpreterOptions = new Interpreter.Options();
        this.delegateGPU = new GpuDelegate();
        this.imageSize = targetSize;
        this.listener = listener;
        interpreterOptions.addDelegate(delegateGPU);
        interpreterOptions.setNumThreads(THREAD_COUNT);

        tfliteInterpreter = new Interpreter(ObjectDetectionUtil.loadModelFile(assets, modelFile), interpreterOptions);
        categories = ObjectDetectionUtil.loadLabelList(assets, labelsFile);

        this.tts = tts;
        this.environment = environment;
    }

    private Map<Integer, Object> detectObjects(Mat inputImage, boolean includeDepthAnalysis, Bitmap depthBitmap) {
        Mat processedImage = new Mat();
        Core.flip(inputImage.t(), processedImage, 1);

        Bitmap bitmapForProcessing = Bitmap.createBitmap(processedImage.cols(), processedImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(processedImage, bitmapForProcessing);

        picHeight = bitmapForProcessing.getHeight();
        picWidth = bitmapForProcessing.getWidth();

        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmapForProcessing, imageSize, imageSize, false);
        ByteBuffer bufferForTFLite = ObjectDetectionUtil.convertBitmapToByteBuffer(bitmapResized, imageSize);

        Object[] tfInput = new Object[1];
        tfInput[0] = bufferForTFLite;

        Map<Integer, Object> tfOutputMap = new TreeMap<>();
        final int MAX_DETECTIONS = 10;

        float[][][] boxes = new float[1][MAX_DETECTIONS][4];
        float[][] classes = new float[1][MAX_DETECTIONS];
        float[][] scores = new float[1][MAX_DETECTIONS];

        tfOutputMap.put(0, boxes);
        tfOutputMap.put(1, classes);
        tfOutputMap.put(2, scores);

        tfliteInterpreter.runForMultipleInputsOutputs(tfInput, tfOutputMap);

        return tfOutputMap;
    }


    public Mat detectObjectsInImage(Mat sourceImage) {
        Mat imageForDetection = new Mat();
        Core.flip(sourceImage.t(), imageForDetection, 1);

        Bitmap detectionBitmap = Bitmap.createBitmap(imageForDetection.cols(), imageForDetection.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageForDetection, detectionBitmap);

        picHeight = detectionBitmap.getHeight();
        picWidth = detectionBitmap.getWidth();

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(detectionBitmap, imageSize, imageSize, false);

        ByteBuffer imageBuffer = ObjectDetectionUtil.convertBitmapToByteBuffer(resizedBitmap, imageSize);

        Object[] inputForModel = new Object[1];
        inputForModel[0] = imageBuffer;

        Map<Integer, Object> modelOutputs = new TreeMap<>();
        final int MAX_DETECTIONS = 10;

        float[][][] detectedBoxes = new float[1][MAX_DETECTIONS][4];
        float[][] detectedClasses = new float[1][MAX_DETECTIONS];
        float[][] detectionScores = new float[1][MAX_DETECTIONS];

        modelOutputs.put(0, detectedBoxes);
        modelOutputs.put(1, detectedClasses);
        modelOutputs.put(2, detectionScores);

        tfliteInterpreter.runForMultipleInputsOutputs(inputForModel, modelOutputs);

        Object boxes = modelOutputs.get(0);
        Object classes = modelOutputs.get(1);
        Object scores = modelOutputs.get(2);

        for (int i = 0; i < MAX_DETECTIONS; i++) {
            float classIndex = (float) Array.get(Array.get(classes, 0), i);
            float score = (float) Array.get(Array.get(scores, 0), i);
            if (score > 0.6) {
                Object box = Array.get(Array.get(boxes, 0), i);

                float top = (float) Array.get(box, 0) * picHeight;
                float bottom = (float) Array.get(box, 2) * picHeight;

                float left = (float) Array.get(box, 1) * picWidth;
                float right = (float) Array.get(box, 3) * picWidth;

                Imgproc.rectangle(imageForDetection, new Point(left, top), new Point(right, bottom), new Scalar(255, 0, 0), 2);

                String category = categories.get((int) classIndex);
                String label = String.format("%s: %.2f", category, score);

                Imgproc.putText(imageForDetection, label, new Point(left, top - 10), 3,1, new Scalar(0, 255, 0), 2);
            }
        }

        Core.flip(imageForDetection.t(), sourceImage, 0);
        return sourceImage;
    }

    public Mat detectTrafficLight(Mat sourceImage) {
        Mat imageForDetection = new Mat();
        Core.flip(sourceImage.t(), imageForDetection, 1);

        Bitmap detectionBitmap = Bitmap.createBitmap(imageForDetection.cols(), imageForDetection.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageForDetection, detectionBitmap);

        picHeight = detectionBitmap.getHeight();
        picWidth = detectionBitmap.getWidth();

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(detectionBitmap, imageSize, imageSize, false);

        ByteBuffer imageBuffer = ObjectDetectionUtil.convertBitmapToByteBuffer(resizedBitmap, imageSize);

        Object[] inputForModel = new Object[1];
        inputForModel[0] = imageBuffer;

        Map<Integer, Object> modelOutputs = new TreeMap<>();
        final int MAX_DETECTIONS = 10;

        float[][][] detectedBoxes = new float[1][MAX_DETECTIONS][4];
        float[][] detectedClasses = new float[1][MAX_DETECTIONS];
        float[][] detectionScores = new float[1][MAX_DETECTIONS];

        modelOutputs.put(0, detectedBoxes);
        modelOutputs.put(1, detectedClasses);
        modelOutputs.put(2, detectionScores);

        tfliteInterpreter.runForMultipleInputsOutputs(inputForModel, modelOutputs);

        Object boxes = modelOutputs.get(0);
        Object classes = modelOutputs.get(1);
        Object scores = modelOutputs.get(2);
        if(!tts.isSpeaking()) {
            for (int i = 0; i < MAX_DETECTIONS; i++) {
                float classIndex = (float) Array.get(Array.get(classes, 0), i);
                float score = (float) Array.get(Array.get(scores, 0), i);
                String category = categories.get((int) classIndex);

                if (score > 0.45 && category.equals("traffic light")) {

                    Object specificBox = Array.get(Array.get(detectedBoxes, 0), i);
                    float top = (float) Array.get(specificBox, 0) * picHeight;
                    float bottom = (float) Array.get(specificBox, 2) * picHeight;

                    float left = (float) Array.get(specificBox, 1) * picWidth;
                    float right = (float) Array.get(specificBox, 3) * picWidth;

                    Rect boundingBox = new Rect((int) left, (int) top, (int) right, (int) bottom);


                    TrafficLight.TrafficLightColor currentColor = TrafficLight.analyzeColor(detectionBitmap, boundingBox);
                    String colorStatus = "";

                    if(currentColor == TrafficLight.TrafficLightColor.GREEN) {
                        colorStatus = "GREEN";
                    } else if (currentColor == TrafficLight.TrafficLightColor.RED) {
                        colorStatus = "RED";
                    } else {
                        colorStatus = "Undef";
                    }

                    if (lastSpokenColor == null || lastSpokenColor != currentColor) {
                        String speechText = TrafficLight.trafficLightText;
                        if(!colorStatus.equals("Undef")) {
                            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
                            lastSpokenColor = currentColor;
                        }
                    }

                    Imgproc.rectangle(imageForDetection, new Point(left, top), new Point(right, bottom), new Scalar(255, 0, 0), 2);
                    String label = String.format("TL: %s", colorStatus);
                    Imgproc.putText(imageForDetection, label, new Point(left, top - 10), 3,1, new Scalar(0, 255, 0), 2);

                    break;
                }
            }
        }

        Core.flip(imageForDetection.t(), sourceImage, 0);
        return sourceImage;
    }

    public Mat detectObjectsInImageAndAnalyzeDepth(Mat inputImage, Bitmap depthBitmap) {
        Mat processedImage = new Mat();
        Core.flip(inputImage.t(), processedImage, 1);

        Bitmap bitmapForProcessing = Bitmap.createBitmap(processedImage.cols(), processedImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(processedImage, bitmapForProcessing);

        picHeight = bitmapForProcessing.getHeight();
        picWidth = bitmapForProcessing.getWidth();

        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmapForProcessing, imageSize, imageSize, false);
        ByteBuffer bufferForTFLite = ObjectDetectionUtil.convertBitmapToByteBuffer(bitmapResized, imageSize);

        Object[] tfInput = new Object[1];
        tfInput[0] = bufferForTFLite;

        Map<Integer, Object> tfOutputMap = new TreeMap<>();
        final int MAX_DETECTIONS = 10;

        float[][][] boxes = new float[1][MAX_DETECTIONS][4];
        float[][] classes = new float[1][MAX_DETECTIONS];
        float[][] scores = new float[1][MAX_DETECTIONS];

        tfOutputMap.put(0, boxes);
        tfOutputMap.put(1, classes);
        tfOutputMap.put(2, scores);

        tfliteInterpreter.runForMultipleInputsOutputs(tfInput, tfOutputMap);

        Object detectedBoxes = tfOutputMap.get(0);
        Object detectedClasses = tfOutputMap.get(1);
        Object detectionScores = tfOutputMap.get(2);

        List<DetectionObject> currentFrameObjects = new ArrayList<>();

        for (int i = 0; i < MAX_DETECTIONS; i++) {
            float classOfDetectedObject = (float) Array.get(Array.get(detectedClasses, 0), i);
            float scoreOfDetection = (float) Array.get(Array.get(detectionScores, 0), i);
            if ((scoreOfDetection > 0.6) || (scoreOfDetection > 0.4 && isVehicle(String.valueOf(classOfDetectedObject)))) {
                Object specificBox = Array.get(Array.get(detectedBoxes, 0), i);

                float top = (float) Array.get(specificBox, 0) * picHeight;
                float bottom = (float) Array.get(specificBox, 2) * picHeight;

                float left = (float) Array.get(specificBox, 1) * picWidth;
                float right = (float) Array.get(specificBox, 3) * picWidth;

                //Imgproc.rectangle(processedImage, new Point(left, top), new Point(right, bottom), new Scalar(255, 165, 0), 2);

                int depthMapWidth = depthBitmap.getWidth();
                int depthMapHeight = depthBitmap.getHeight();
                Rect boundingBox = new Rect((int) left, (int) top, (int) right, (int) bottom);

                String depthAnnotation = DepthRecognition.analyzeDepthAndAdjustDistance(boundingBox, bitmapForProcessing, depthBitmap, processedImage, categories, classOfDetectedObject, depthMapWidth, depthMapHeight);
                float adjustedDistance = getAdjustedDistanceValue(depthAnnotation);
                String objectLabel = categories.get((int) classOfDetectedObject);

                DetectionObject newObj = new DetectionObject(boundingBox, objectLabel, scoreOfDetection, adjustedDistance, nextObjectId++);
                currentFrameObjects.add(newObj);

                Imgproc.putText(processedImage, depthAnnotation, new Point(left, top - 10), 3,1, new Scalar(0, 255, 255), 2);

            }
        }
        //Log.d("Tracking Info", "Number of tracked objects before update: " + trackedObjects.size());
        // update and draw based on trackedObjects
        updateTrackedObjects(currentFrameObjects, trackedObjects);
        //Log.d("Tracking Info", "Number of tracked objects after update: " + trackedObjects.size());
        generateWarnings(currentFrameObjects, processedImage, trackedObjects, listener);
        drawBoundingBoxes(processedImage, trackedObjects);

        Core.flip(processedImage.t(), inputImage, 0);
        return inputImage;
    }

    public Mat detectObjectsDepthAndDescribe(Mat inputImage, Bitmap depthBitmap) {
        Mat processedImage = new Mat();
        Core.flip(inputImage.t(), processedImage, 1);

        Bitmap bitmapForProcessing = Bitmap.createBitmap(processedImage.cols(), processedImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(processedImage, bitmapForProcessing);

        picHeight = bitmapForProcessing.getHeight();
        picWidth = bitmapForProcessing.getWidth();

        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmapForProcessing, imageSize, imageSize, false);
        ByteBuffer bufferForTFLite = ObjectDetectionUtil.convertBitmapToByteBuffer(bitmapResized, imageSize);

        Object[] tfInput = new Object[1];
        tfInput[0] = bufferForTFLite;

        Map<Integer, Object> tfOutputMap = new TreeMap<>();
        final int MAX_DETECTIONS = 10;

        float[][][] boxes = new float[1][MAX_DETECTIONS][4];
        float[][] classes = new float[1][MAX_DETECTIONS];
        float[][] scores = new float[1][MAX_DETECTIONS];

        tfOutputMap.put(0, boxes);
        tfOutputMap.put(1, classes);
        tfOutputMap.put(2, scores);

        tfliteInterpreter.runForMultipleInputsOutputs(tfInput, tfOutputMap);

        Object detectedBoxes = tfOutputMap.get(0);
        Object detectedClasses = tfOutputMap.get(1);
        Object detectionScores = tfOutputMap.get(2);

        List<DetectionObject> currentFrameObjects = new ArrayList<>();

        for (int i = 0; i < MAX_DETECTIONS; i++) {
            float classOfDetectedObject = (float) Array.get(Array.get(detectedClasses, 0), i);
            float scoreOfDetection = (float) Array.get(Array.get(detectionScores, 0), i);
            if ((scoreOfDetection > 0.6) || (scoreOfDetection > 0.4 && isVehicle(String.valueOf(classOfDetectedObject)))) {
                Object specificBox = Array.get(Array.get(detectedBoxes, 0), i);

                float top = (float) Array.get(specificBox, 0) * picHeight;
                float bottom = (float) Array.get(specificBox, 2) * picHeight;

                float left = (float) Array.get(specificBox, 1) * picWidth;
                float right = (float) Array.get(specificBox, 3) * picWidth;

                //Imgproc.rectangle(processedImage, new Point(left, top), new Point(right, bottom), new Scalar(255, 165, 0), 2);

                int depthMapWidth = depthBitmap.getWidth();
                int depthMapHeight = depthBitmap.getHeight();
                Rect boundingBox = new Rect((int) left, (int) top, (int) right, (int) bottom);

                String depthAnnotation = DepthRecognition.analyzeDepthAndAdjustDistance(boundingBox, bitmapForProcessing, depthBitmap, processedImage, categories, classOfDetectedObject, depthMapWidth, depthMapHeight);
                float adjustedDistance = getAdjustedDistanceValue(depthAnnotation);
                String objectLabel = categories.get((int) classOfDetectedObject);

                DetectionObject newObj = new DetectionObject(boundingBox, objectLabel, scoreOfDetection, adjustedDistance, nextObjectId++);
                currentFrameObjects.add(newObj);

                //generateWarnings(newObj, objectLabel, adjustedDistance);
                Imgproc.putText(processedImage, depthAnnotation, new Point(left, top - 10), 3,1, new Scalar(0, 255, 255), 2);

            }
        }
        //Log.d("Tracking Info", "Number of tracked objects before update: " + trackedObjects.size());
        // update and draw based on trackedObjects
        updateTrackedObjects(currentFrameObjects, trackedObjects);
        //Log.d("Tracking Info", "Number of tracked objects after update: " + trackedObjects.size());

        environment.topThreeObjects(currentFrameObjects, picWidth);

        drawBoundingBoxes(processedImage, trackedObjects);

        Core.flip(processedImage.t(), inputImage, 0);
        return inputImage;
    }


}
