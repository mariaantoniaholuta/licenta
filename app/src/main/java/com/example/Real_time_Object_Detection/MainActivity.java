package com.example.Real_time_Object_Detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import org.opencv.android.OpenCVLoader;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    static {
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity: ","Opencv is loaded");
        }
        else {
            Log.d("MainActivity: ","Opencv failed to load");
        }
    }

    private Button camera_button;
    private ObectDetectionClass obectDetectionClass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void onClickCamera(View v) {
        Intent i = new Intent(MainActivity.this, com.example.Real_time_Object_Detection.CameraActivity.class);
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
}