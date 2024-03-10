package com.example.Real_time_Object_Detection;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.IOException;
public class MiDASModel {
    private final Interpreter interpreter;
    private static final String MODEL_FILE_NAME = "depth_model.tflite";
    private static final int NUM_THREADS = 5;
    private static final int INPUT_IMAGE_DIM = 256;
    private static final float[] MEAN = new float[]{123.675f, 116.28f, 103.53f};
    private static final float[] STD = new float[]{58.395f, 57.12f, 57.375f};
    private final ImageProcessor inputTensorProcessor;
    private final TensorProcessor outputTensorProcessor;
    private final GpuDelegate gpuDelegate;

    public MiDASModel(Context context) throws IOException {

        Interpreter.Options options = new Interpreter.Options();
        this.gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(NUM_THREADS);

        interpreter = new Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE_NAME), options);

        inputTensorProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(MEAN, STD))
                .build();

        outputTensorProcessor = new TensorProcessor.Builder()
                .add(new MinMaxScalingOp())
                .build();
    }

    public Bitmap getDepthMap(Bitmap inputImage) {
        TensorImage inputTensor = new TensorImage(DataType.FLOAT32);
        inputTensor.load(inputImage);
        inputTensor = inputTensorProcessor.process(inputTensor);

        TensorBuffer outputTensor = TensorBufferFloat.createFixedSize(new int[]{INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, 1}, DataType.FLOAT32);

        interpreter.run(inputTensor.getBuffer(), outputTensor.getBuffer());

        outputTensor = outputTensorProcessor.process(outputTensor);

        return BitmapUtils.byteBufferToBitmap(outputTensor.getFloatArray(), INPUT_IMAGE_DIM);
    }

    private static class MinMaxScalingOp implements TensorOperator {
        @Override
        public TensorBuffer apply(TensorBuffer input) {
            float[] values = input.getFloatArray();
            float max = getMax(values);
            float min = getMin(values);
            for (int i = 0; i < values.length; i++) {
                values[i] = (((values[i] - min) / (max - min)) * 255);
                if (values[i] < 0) {
                    values[i] += 255;
                }
            }
            TensorBuffer output = TensorBufferFloat.createFixedSize(input.getShape(), DataType.FLOAT32);
            output.loadArray(values);
            return output;
        }

        private float getMax(float[] values) {
            float max = values[0];
            for (float v : values) {
                if (v > max) {
                    max = v;
                }
            }
            return max;
        }

        private float getMin(float[] values) {
            float min = values[0];
            for (float v : values) {
                if (v < min) {
                    min = v;
                }
            }
            return min;
        }
    }
}