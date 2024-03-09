package com.example.Real_time_Object_Detection;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

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

public class ObectDetectionClass {
    // used to load model and predicts
    private final Interpreter interpreter;
    //used to store label in arrayList
    private  final List<String> labelList;
    //Input Size
    private final int inputSize;

    // use to initlize gpu in app
    private final GpuDelegate gpuDelegate;

    private int height;
    private int width;

    ObectDetectionClass(int input_Size, AssetManager assetManger, String modelPath, String labelPath) throws IOException {
        this.inputSize = input_Size;
        // use to define gpu or cpu && number of threads
        Interpreter.Options options = new Interpreter.Options();
        this.gpuDelegate = new GpuDelegate();
        //to use gpu not mobile cpu
        options.addDelegate(gpuDelegate);
        //set threads according to your phone cabability
        options.setNumThreads(2);

        // load model deceleared down in function loadModdelFile
        interpreter = new Interpreter(loadModelFile(assetManger, modelPath), options);

        // load labelmap deceleared down in function loadLabelList
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

    public Mat recongizeImage(Mat mat_image) {
        // rotate original image by 90 degree to get potrait mode
        Mat rotated_image = new Mat();
        Core.flip(mat_image.t(), rotated_image, 1);

        // now convert it to bitmap
        Bitmap bitmap = Bitmap.createBitmap(rotated_image.cols(), rotated_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_image,bitmap);

        //define height and width
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        //Scale the bitmap image to the inputSize of the model
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);

        //now define a method that convert the Scaled bitmap to bufferByte as the model input should be in it
        ByteBuffer byteBuffer = ConvertBitmapToByteBuffer(scaledBitmap);

        //store input ByteBuffer in arrayObject of size 1;
        Object[] input = new Object[1];
        input[0] = byteBuffer;

        // defineing the output by treemap of three arrays (boxes,classes,score)
        Map<Integer,Object> output_map = new TreeMap<>();

        // 10: top 10 objects detected
        // 4: there coordrinate
        float [][][] boxes = new float[1][10][4];

        // store classes of 10 objects
        float[][] classes = new float [1][10];

        // store score of 10 objects
        float[][] score = new float [1][10];



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
        for (int i=0 ;i<10;i++){
            //Object value_box = (float) Array.get(Array.get(object_boxes,0),i);
            float value_classes = (float) Array.get(Array.get(object_classes,0),i);
            float value_score = (float) Array.get(Array.get(object_score,0),i);
            if(value_score>0.5){
                Object value_box1= (Object) Array.get(Array.get(object_boxes,0),i);

                //we are mulltiplying by Original Frame
                float top =(float) Array.get(value_box1,0)*height;
                float left =(float) Array.get(value_box1,1)*width;
                float bottom =(float) Array.get(value_box1,2)*height;
                float right =(float) Array.get(value_box1,3)*width;

                //draw rectangle in Orignal frame //startingPoint of box   // EndingPoint of box  // Color Box     // thickness
                Imgproc.rectangle(rotated_image,new Point(left,top),new Point(right,bottom),new Scalar(255,155,155),2);

                //write text on the frame
                Imgproc.putText(rotated_image,labelList.get((int)value_classes),new Point(left,top),3,1,new Scalar(100,100,100) ,2);
                //Imgproc.putText(rotated_image, labelList.get((int)value_classes) + ": " + String.format("%.2f", value_score), new Point(left,top), 3, 1, new Scalar(100,100,100), 2);


            }
        }

        // before returning rotate it back by -90 degree
        Core.flip(rotated_image.t(), mat_image, 0);
        return mat_image;
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
