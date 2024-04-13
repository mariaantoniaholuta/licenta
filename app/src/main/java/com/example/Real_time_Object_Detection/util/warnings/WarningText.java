package com.example.Real_time_Object_Detection.util.warnings;

import android.widget.TextView;


public class WarningText {

    private TextView  warningTextView;

    public WarningText(TextView  warningTextView) {
        this. warningTextView =  warningTextView;
    }

    private void setWarning(String objectName) {
        String text;
        text = String.format("Careful! " + objectName + " is too Close");
        updatePositionStatus(text);
    }

    private void updatePositionStatus(String text) {
        warningTextView.post(new Runnable() {
            @Override
            public void run() {
                warningTextView.setText(text);
            }
        });
    }
}
