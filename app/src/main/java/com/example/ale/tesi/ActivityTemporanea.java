package com.example.ale.tesi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ActivityTemporanea extends AppCompatActivity {

    static public int video_tmp;
    static public String boxes;
    static public String frame_tmp;
    static public double fps = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temporanea);
        if(boxes == null){
            boxes = "OFF";
        }
        scegliVideo(this);
    }

    //TEMPORANEO

    private void scegliVideo(final Activity activity){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");
        final Intent intent = new Intent(activity, MainActivity.class);
        builder.setItems(new CharSequence[]
                        {"Video1", "Camera", "boxes " + boxes},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        switch (which) {
                            case 0:
                                video_tmp = R.raw.vid_20161007_131547_reduced_processed;
                                frame_tmp = "vid_20161007_131547_reduced_frame_";
                                fps = 27;
                                startActivity(intent);
                                break;
                            case 1:
                                scegliVideo(activity);
                            case 2:
                                if(boxes.equals("ON")){
                                    boxes = "OFF";
                                }else{
                                    boxes = "ON";
                                }
                                scegliVideo(activity);
                                break;
                        }

                    }
                });
        builder.create().show();
    }

    @Override
    protected void onResume() {
        scegliVideo(this);
        super.onResume();
    }
}
