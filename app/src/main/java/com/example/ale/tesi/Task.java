package com.example.ale.tesi;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Task implements Runnable {

    /*
        Classe fondamentale che si occupa sia del posizionamento delle icone del riconoscimento,
        sia della gestione dei compiti automatici che vengono però definiti nella classe
        MainActivity.
     */

    //TEMPORANEO
    int propX = 1920;
    int propY = 1200;


    //TEMPORANEO
    private int count_foot = 0;
    private int index_foot = 0;
    private ArrayList<Drawable> footsteps = new ArrayList<>();

    private int count_voice = 0;
    private int index_voice = 0;
    private ArrayList<Drawable> voices = new ArrayList<>();


    //Atributi relativi alla grafica delle icone del riconoscimento.
    private boolean blur = false;
    private ArrayList<ImageView> imageViews = new ArrayList<>();
    private ArrayList<TextView> textViews = new ArrayList<>();
    private ArrayList<RelativeLayout> relativeLayouts = new ArrayList<>();

    private ArrayList<Drawable> drawablesRecognized = new ArrayList<>();
    private ArrayList<Drawable> drawablesNotRecognized = new ArrayList<>();
    private ArrayList<Drawable> drawablesNotRecognizedBlur = new ArrayList<>();
    private Drawable drawableRecognizedBlur;
    private boolean speaking = false;
    private boolean audioIsRestart = false;
    private int sizeX;
    private int sizeY;
    private RelativeLayout relativeLayout;

    private String idOperaAutoMode = "";

    //Attributi relativi alla gestione delle icone.
    private int frame_id = 1;
    private MainActivity activity;
    private int dimension;
    private RelativeLayout canvas;
    private ImageView imageView;
    private RelativeLayout relLabel;
    private TextView texLabel;
    private RelativeLayout.LayoutParams labelParams;
    private RelativeLayout.LayoutParams textParams;
    private android.os.Handler handler;
    private JSONObject jsonObject;
    private int not_reco_index = 0;
    private ArrayList<Integer> not_reco_array = new ArrayList<>();
    private int prec_size = 1;
    private long time = 0;
    private Mat frameMat;

    private double fps = 30;
    private double soglia_voice = 0.3;

    //Attributi relativi alla gestione dei compiti automatici.
    private int maxConfidence;
    private double interval;
    private int countPersonPerAudio = 0;

    public void setMaxConfidence(){
        maxConfidence = (int) (MainActivity.secondPerAuto -1)*(int) fps + 15;
    }

    public void setInterval(){
        interval = ((MainActivity.secondPerAuto - 1)/6)*fps;
    }

    public void setIdOperaAutoMode(String idOperaAutoMode) {
        this.idOperaAutoMode = idOperaAutoMode;
    }

    public int getMaxConfidence() {
        return maxConfidence;
    }

    public double getInterval() {
        return interval;
    }

    /*
        Fondamentalmente, ogni frame viene chiesto il JSON dalla telecamera. Viene fatto il parsing
        di questo JSON guardando se e quante opere sono inquadrate. Per ogni opera inquadrata ma non
        riconosciuta, viene creata una bitmap di opera non riconosciuta. Utilizzando il numero del
        frame e vari contatori, viene creata un'animazione mettendo in sequenza 3 diverse bitmap.
        Quando nel JSON viene trovata un opera con il campo recognize = true, viene creata un'icona
        di opera ricuonosciuta con la quale è possibile interagire: se verrà effettuato un tap su
        questa, verrò creato l'oggetto Opera relativo, viene aggiornato il footer di History e
        partirà l'audio. Quando l'utente desidererà continuare lo streaming video, basterà un tap
        sullo schermo, l'audio andrà in fade e sarà possibile effettuare un nuovo tap nell'icona
        scelta.
     */

    public Task(Activity activity, android.os.Handler handler){

        doubles[0] = 10.0;
        doubles[1] = 10.0;
        blurSize.set(doubles);

        //FOOTSTEP
        footsteps.add(activity.getResources().getDrawable(R.drawable.foot1));
        footsteps.add(activity.getResources().getDrawable(R.drawable.foot2));
        voices.add(activity.getResources().getDrawable(R.drawable.audio1));
        voices.add(activity.getResources().getDrawable(R.drawable.audio2));
        voices.add(activity.getResources().getDrawable(R.drawable.audio3));

        this.canvas = (RelativeLayout) activity.findViewById(R.id.canvas);

        imageViews.add(new ImageView(activity));
        textViews.add(new TextView(activity));
        relativeLayouts.add(new RelativeLayout(activity));
        texLabel = textViews.get(0);

        labelParams = new RelativeLayout.LayoutParams(canvas.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        texLabel.setTextAppearance(activity, R.style.TitleName);
        texLabel.setTypeface(Typeface.DEFAULT_BOLD);
        texLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        texLabel.setLayoutParams(textParams);
        texLabel.setPadding((int)activity.getResources().getDimension(R.dimen.label_margin_left), (int)activity.getResources().getDimension(R.dimen.label_margin_top), (int)activity.getResources().getDimension(R.dimen.label_margin_left), (int)activity.getResources().getDimension(R.dimen.label_margin_top));

        for (int i = 0; i < 7; i++){
            drawablesRecognized.add(i, activity.getResources().getDrawable(R.drawable.recognized_ff1 + i));
        }
        drawableRecognizedBlur = activity.getResources().getDrawable(R.drawable.recognized_blur_ff);
        for (int i = 0; i < 3; i++){

            drawablesNotRecognized.add(i, activity.getResources().getDrawable(R.drawable.not_recognized_ff1 + i));
            drawablesNotRecognizedBlur.add(i, activity.getResources().getDrawable(R.drawable.not_recognized_blur_ff1 + i));

        }
        this.activity = (MainActivity) activity;
        this.frame_id = 1;
        dimension = (int) activity.getResources().getDimension(R.dimen.icon_task);

        this.handler = handler;
    }



    public void run() {

        fps = ActivityTemporanea.fps;


        if(MainActivity.voiceCommand.equals("ON")){
            if(!activity.isNetworkAvailable()){
                MainActivity.voiceCommand = "OFF";
                activity.invalidateOptionsMenu();
            }
        }

        if(sizeX == 0 || sizeY == 0){
            sizeX = canvas.getWidth();
            sizeY = canvas.getHeight();
            labelParams = new RelativeLayout.LayoutParams(sizeX, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if(relativeLayout != null){
            relativeLayout.removeAllViews();
        }
        canvas.removeAllViews();
        speaking = false;
        audioIsRestart = false;
        if((activity.findViewById(R.id.blurCanvas)).getVisibility() == View.VISIBLE){
            (activity.findViewById(R.id.blurCanvas)).setVisibility(View.GONE);
        } else if(countBlur != 0){
            countBlur = 0;
            (activity.findViewById(R.id.blurCanvas)).setAlpha(0);
            doubles[0] = 10.0;
            doubles[1] = 10.0;
            blurSize.set(doubles);
            alpha = 0;
        }

        imageView = null;

        if(time == 0){
            time = System.currentTimeMillis();
        }

        //if((frame_id%20) == 0 || frame_id == 0) {

            //frame_id = (int) (activity.getVideoView().getCurrentPosition() * ((double)fps)/1000);

            //CAMERA
            long act_time = System.currentTimeMillis();
            frame_id = (int) ((act_time - time) * 0.03);
        //}

        activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    String msg = inputJson();
                    try {
                        JSONObject jsonObjectFather = new JSONObject(msg);
                        getListener(jsonObjectFather);

                    } catch (JSONException e) {
                        Toast.makeText(activity, "Errore di JSON", Toast.LENGTH_SHORT).show();
                    }
                }
            });


        //frame_id ++;
        //CAMERA
        handler.postDelayed(this, 33);

    }

    public String inputJson(){
        String msg = "";


        //TEMPORANEO

        InputStreamReader inputStreamReader = null;

        if(frame_id == 0){
            frame_id = 1;
        }

        try {
            int frame = activity.getResources().getIdentifier(ActivityTemporanea.frame_tmp + Integer.toString(frame_id), "raw", activity.getPackageName());
            inputStreamReader = new InputStreamReader(activity.getResources().openRawResource(frame));
        } catch (Exception e){
            System.exit(1);
        }

        //InputStreamReader inputStreamReader = new InputStreamReader(activity.getResources().openRawResource(R.raw.c_a_reduced_frame_1 + frame_id));
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                msg += line;
            }
        } catch (IOException e) {
            Toast.makeText(activity, "Errore di Lettura", Toast.LENGTH_SHORT).show();
        }
        return msg;
    }

    /*
        Metodo per il parsing dei JSON in ingresso. Viene scandito tutto l'array di opere, gestiti
        i vari contatori per le animazioni e per la gestione dei compiti automatici.
     */

    public void getListener(JSONObject jo){
        try{
            JSONArray jsonArray = new JSONArray(jo.get("bbs").toString());

            try {
                if (jo.getDouble("walking") == 1) {
                    count_foot++;
                    if (count_foot % 15 == 0) {
                        if (index_foot == 0) {
                            index_foot++;
                        } else {
                            index_foot--;
                        }
                    }

                    ((ImageView) activity.findViewById(R.id.footstep)).setImageDrawable(footsteps.get(index_foot));

                } else {
                    ((ImageView) activity.findViewById(R.id.footstep)).setImageDrawable(activity.getResources().getDrawable(R.drawable.foot0));
                }
            } catch (Exception e){
                /*if((12000 < activity.getVideoView().getCurrentPosition() && activity.getVideoView().getCurrentPosition() < 26000) ||
                        (33000 < activity.getVideoView().getCurrentPosition() && activity.getVideoView().getCurrentPosition() < 35000) ||
                        (37000 < activity.getVideoView().getCurrentPosition() && activity.getVideoView().getCurrentPosition() < 45000)){
                    count_foot++;
                    if (count_foot % 15 == 0) {
                        if (index_foot == 0) {
                            index_foot++;
                        } else {
                            index_foot--;
                        }
                    }

                    ((ImageView) activity.findViewById(R.id.footstep)).setImageDrawable(footsteps.get(index_foot));
                } else{
                    ((ImageView) activity.findViewById(R.id.footstep)).setImageDrawable(activity.getResources().getDrawable(R.drawable.foot0));
                }*/
            }

            try {
                if(jo.getDouble("p_voice") > soglia_voice){
                    speaking = true;

                    activity.stopAudioWithFade(1000);
                }
            } catch (JSONException e){

            }

            //TEMPORANEO


            try {
                if (speaking && jo.getDouble("walking") == 0) {
                    count_voice++;
                    if (count_voice % 15 == 0) {
                        if (index_voice < 2) {
                            index_voice++;
                        } else {
                            index_voice = 0;
                        }
                    }

                    ((ImageView) activity.findViewById(R.id.voice)).setImageDrawable(voices.get(index_voice));

                } else {
                    index_voice = 0;
                    ((ImageView) activity.findViewById(R.id.voice)).setImageDrawable(activity.getResources().getDrawable(R.drawable.audio0));
                }
            } catch (Exception e){

            }

            if(jsonArray.length() != 0) {

                int num_anim = 0;

                for (int i = 0; i < jsonArray.length(); i++){
                    if(((jsonArray.getJSONObject(i)).getString("class")).equals("artwork") && !(jsonArray.getJSONObject(i)).getBoolean("recognized")){
                        num_anim ++;
                    }
                }

                if(MainActivity.autoMode.equals("ON") &&
                        (activity.audioIsPlaying() || MainActivity.operaAudioAsk != null || !idOperaAutoMode.equals(""))){

                    Boolean stopAutoMode = true;
                    Boolean count = true;

                    for (int i = 0; i < jsonArray.length(); i++){

                        if(((jsonArray.getJSONObject(i)).getString("class")).equals("artwork") &&
                                (jsonArray.getJSONObject(i)).getBoolean("recognized")){

                            String id = jsonArray.getJSONObject(i).getString("artwork_id");

                            if((id.equals(idOperaAutoMode))
                                    && (jsonArray.getJSONObject(i).getBoolean("bb_big_enough"))){
                                stopAutoMode = false;
                            }
                            if(MainActivity.voiceCommand.equals("ON") && (jsonArray.getJSONObject(i).getString("artwork_id").equals(MainActivity.operaAudioAsk))){
                                count = false;
                            }
                        }
                    }

                    if(MainActivity.voiceCommand.equals("ON") && count){
                        countPersonPerAudio ++;
                    }

                    if(stopAutoMode){
                    //if(stopAutoMode && jo.getInt("static") == 0){
                        activity.stopAudioWithFade(MainActivity.fade_seconds * 100);
                    }
                }

                //MIGLIORIE

                if(MainActivity.autoMode.equals("ON")){

                    double dimension = 0;
                    Integer index = null;

                    for (int i = 0; i < jsonArray.length(); i++){

                        if(((jsonArray.getJSONObject(i)).getString("class")).equals("artwork") &&
                                (jsonArray.getJSONObject(i)).getBoolean("recognized")){

                            if(!speaking){

                                String id = (jsonArray.getJSONObject(i)).getString("artwork_id");

                                activity.setVolumeUp(id,
                                        (jsonArray.getJSONObject(i)).getBoolean("bb_big_enough"));
                            }

                            if(dimension < ((jsonArray.getJSONObject(i).getDouble("right") - jsonArray.getJSONObject(i).getDouble("left"))*
                                    (jsonArray.getJSONObject(i).getDouble("bottom") - jsonArray.getJSONObject(i).getDouble("top")))){
                                dimension = ((jsonArray.getJSONObject(i).getDouble("right") - jsonArray.getJSONObject(i).getDouble("left"))*
                                        (jsonArray.getJSONObject(i).getDouble("bottom") - jsonArray.getJSONObject(i).getDouble("top")));
                                index = i;
                            }

                        }
                    }

                    if(index != null && (jsonArray.getJSONObject(index)).getInt("confidence") >= maxConfidence) {

                        autoTask(jsonArray.getJSONObject(index));
                    }

                }

                if (countPersonPerAudio > fps) {
                    MainActivity.operaAudioAsk = null;
                }

                int index_art = 0;

                for (int t = 0; t < jsonArray.length(); t++) {

                    jsonObject = jsonArray.getJSONObject(t);


                    //TEMPORANEO
                    if(ActivityTemporanea.boxes.equals("ON")){
                        drawBox(jsonObject);
                    }

                    if(jsonObject.getString("class").equals("artwork")) {

                        /* if(t == 0){
                            if(jsonObject.getBoolean("bb_big_enough")){
                                blur = true;
                            }
                        } */

                        index_art ++;

                        if(!jsonObject.getBoolean("recognized")) {
                            createNotRecognizedIcon(jsonObject, num_anim, index_art - 1);
                        }else{
                            createRecognizedIcon(jsonObject, index_art - 1);
                        }
                    }
                }

                blur = false;
            }
        } catch (JSONException e){
            Toast.makeText(activity, "Errore di JSON", Toast.LENGTH_SHORT).show();
        }
    }

    public int getFrame_id() {
        return frame_id;
    }

    /*
        I metodi seguenti servono per gestire il posizionamento delle icone sullo schermo.
     */

    public int marginRight(JSONObject jsonObject, int index){
        try {

            //TEMPORANEO

            int right;


            right = (int) (jsonObject.getDouble("right") * sizeX);


            return Smooth.smooth_right(1080 - right, index);
        } catch (JSONException e){
            return 0;
        }
    }

    public int marginLeft(JSONObject jsonObject, int index){
        try {

            //TEMPORANEO
            int right;
            int left;


            right = (int) (jsonObject.getDouble("right") * sizeX);
            left = (int) (jsonObject.getDouble("left") * sizeX);


            if((right - left) > dimension) {
                return Smooth.smooth_left((right - left) / 2 + left - dimension / 2, index);
            } else {
                return Smooth.smooth_left(left, index);
            }
        } catch (JSONException e){
            return 0;
        }
    }

    public int marginTop(JSONObject jsonObject, int index){
        try {

            //TEMPORANEO
            int top;
            int bottom;

            top = (int) (jsonObject.getDouble("top") * sizeY);
            bottom = (int) (jsonObject.getDouble("bottom") * sizeY);

            return Smooth.smooth_top((bottom - top)/2 + top - dimension/2, index);
        } catch (JSONException e){
            return 0;
        }
    }

    public void createNotRecognizedIcon(JSONObject jsonObject, int size, int index){

        if(imageViews.size() >= index + 1){
            imageView = imageViews.get(index);
            relLabel = relativeLayouts.get(index);
            texLabel = textViews.get(index);
        } else {
            imageView = new ImageView(activity);
            imageViews.add(index, imageView);
            relLabel = new RelativeLayout(activity);
            relativeLayouts.add(index, relLabel);
            texLabel = new TextView(activity);
            texLabel.setTextAppearance(activity, R.style.TitleName);
            texLabel.setTypeface(Typeface.DEFAULT_BOLD);
            texLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            texLabel.setLayoutParams(textParams);
            texLabel.setPadding((int)activity.getResources().getDimension(R.dimen.label_margin_left), (int)activity.getResources().getDimension(R.dimen.label_margin_top), (int)activity.getResources().getDimension(R.dimen.label_margin_left), (int)activity.getResources().getDimension(R.dimen.label_margin_top));
            textViews.add(index, texLabel);
        }

        if(not_reco_array.size() <= index + 1){
            not_reco_array.add(index, 0);
        }
        int not_reco = not_reco_array.get(index);
        /*RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(dimension,
                dimension);
        layoutParams.setMargins(marginLeft(jsonObject, index), marginTop(jsonObject, index),
                marginRight(jsonObject, index), 0);*/

        if(not_reco <= 8)
        {
            not_reco_index = 0;
            not_reco ++;
        } else if(not_reco <= 16){
            not_reco_index = 1;
            not_reco ++;
        } else if (not_reco < 24) {
            not_reco_index = 2;
            not_reco ++;
        } else if (not_reco == 24) {
            not_reco_index = 2;
            not_reco = 1;
        }

        imageView.setImageDrawable(drawablesNotRecognized.get(not_reco_index));


        RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(dimension, dimension);
        int left = marginLeft(jsonObject, index);
        int right = marginRight(jsonObject, index);
        int top = marginTop(jsonObject, index);
        iconParams.setMargins(left, top, right, 0);
        imageView.setLayoutParams(iconParams);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        if(MainActivity.language.equals("IT")){
            texLabel.setText("Opera");
        } else{
            texLabel.setText("Artwork");
        }

        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(sizeX, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        texLabel.setBackground(activity.getDrawable(R.drawable.label_background_red));
        try{

            //TEMPORANEO

            if ((jsonObject.getDouble("right") - jsonObject.getDouble("left")) * sizeX > (5*dimension)/8) {

                labelParams.setMargins(left - sizeX / 2 + dimension / 2, top + dimension + (int) activity.getResources().getDimension(R.dimen.top_label), -sizeX, 0);
                texLabel.setLayoutParams(textParams);
                relLabel.setLayoutParams(labelParams);
                relLabel.removeAllViews();
                relLabel.addView(texLabel);
                canvas.addView(relLabel);

            }

        } catch(JSONException e){

        }

        not_reco_array.set(index, not_reco);
        canvas.addView(imageView);
    }

    public void createRecognizedIcon(final JSONObject jsonObject, int index){

        try {

            int index_bitmap = 6;

            String id = jsonObject.getString("artwork_id");

            int confidence = jsonObject.getInt("confidence");

            if (confidence < 15 + interval) {
                index_bitmap = 0;
            } else if (confidence < 15 + (interval*2)) {
                index_bitmap = 1;
            } else if (confidence < 15 + (interval*3)) {
                index_bitmap = 2;
            } else if (confidence < 15 + (interval*4)) {
                index_bitmap = 3;
                //CAMERABLUR
                //blur = true;
            } else if (confidence < 15 + (interval*5)) {
                index_bitmap = 4;
                //blur = true;
            } else if (confidence < 15 + (interval*6)) {
                index_bitmap = 5;
                //blur = true;
            } else if (confidence >= 15 + (interval*6)) {
                index_bitmap = 6;
                //blur = true;
            }

            if(imageViews.size() >= index + 1){
                imageView = imageViews.get(index);
                relLabel = relativeLayouts.get(index);
                texLabel = textViews.get(index);
            } else {
                imageView = new ImageView(activity);
                imageViews.add(index, imageView);
                relLabel = new RelativeLayout(activity);
                relativeLayouts.add(index, relLabel);
                texLabel = new TextView(activity);
                texLabel.setTextAppearance(activity, R.style.TitleName);
                texLabel.setTypeface(Typeface.DEFAULT_BOLD);
                texLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                texLabel.setLayoutParams(textParams);
                texLabel.setPadding((int)activity.getResources().getDimension(R.dimen.label_margin_left), (int)activity.getResources().getDimension(R.dimen.label_margin_top), (int)activity.getResources().getDimension(R.dimen.label_margin_left), (int)activity.getResources().getDimension(R.dimen.label_margin_top));
                textViews.add(index, texLabel);
            }


            if(confidence == 15 && countPersonPerAudio >= fps ){
                MainActivity.operaAudioAsk = null;
                countPersonPerAudio = 0;
            }
            //autoTask(jsonObject);
            imageView.setTag(id);

            imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if(MainActivity.voiceCommand.equals("ON")){
                        return false;
                    }

                    if(MainActivity.autoMode.equals("ON")){

                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            if (activity.getVideoIsRunning()) {
                                if(!activity.audioIsPlaying()){
                                    try {
                                        Opera opera = activity.createOpera(jsonObject.getString("artwork_id"));
                                        activity.startAudio(opera);
                                        idOperaAutoMode = jsonObject.getString("artwork_id");
                                    } catch(JSONException e){

                                    }
                                } else{
                                    if(activity.getOperaAudio()!= null && activity.getOperaAudio().equals(v.getTag())){
                                        activity.manageStreamVideo();
                                        if(activity.getOperaAudio() != null && canvas.findViewWithTag(activity.getOperaAudio())!=null){
                                            ((ImageView)canvas.findViewWithTag(activity.getOperaAudio())).setImageDrawable(activity.getDrawable(R.drawable.recognized_ff7));
                                        }
                                        activity.pauseAudio();
                                    } else {
                                        try {
                                            Opera opera = activity.createOpera(jsonObject.getString("artwork_id"));
                                            activity.stopAudio();
                                            idOperaAutoMode = opera.getId();
                                            activity.startAudio(opera);
                                        } catch(JSONException e){

                                        }
                                    }

                                }
                                /*activity.manageStreamVideo();
                                if(activity.getOperaAudio() != null && canvas.findViewWithTag(activity.getOperaAudio())!=null){
                                    ((ImageView)canvas.findViewWithTag(activity.getOperaAudio())).setImageDrawable(activity.getDrawable(R.drawable.recognized_ff7));
                                }
                                activity.pauseAudio();*/

                            } else {
                                if(activity.getOperaAudio() != null && !activity.getOperaAudio().equals(v.getTag())){
                                    try {
                                        Opera opera = activity.createOpera(jsonObject.getString("artwork_id"));
                                        activity.stopAudio();
                                        idOperaAutoMode = opera.getId();
                                        activity.startAudio(opera);
                                    } catch(JSONException e){

                                    }
                                }
                                activity.manageStreamVideo();
                            }

                            return true;
                        } else {
                            return false;
                            }

                    } else{
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                if (activity.getVideoIsRunning()) {
                                    activity.manageStreamVideo();
                                } else {

                                    if(activity.getOperaAudio() != null && activity.getOperaAudio().equals(v.getTag())){
                                        activity.manageStreamVideo();
                                        return true;
                                    } else {
                                        if(activity.audioIsPlaying()){
                                            ((ImageView)canvas.findViewWithTag(activity.getOperaAudio())).setImageDrawable(activity.getDrawable(R.drawable.recognized_ff7));
                                            activity.stopAudio();

                                        }
                                    }
                                }
                                try {
                                    ((ImageView)v).setImageDrawable(activity.getDrawable(R.drawable.recognized_pause));
                                    Opera opera = activity.createOpera(jsonObject.getString("artwork_id"));
                                    if(activity.audioIsPlaying()){
                                        if(activity.getOperaAudio() != null && !activity.getOperaAudio().equals(v.getTag())){
                                            activity.stopAudio();
                                        }else {
                                            activity.pauseAudio();
                                        }

                                    }
                                    activity.startAudio(opera);
                                } catch (JSONException e) {

                                }
                                return true;
                            } else {
                                return false;
                            }


                    }

                }
            });
            if (blur && MainActivity.blurMode.equals("ON")) {

                try {
                    if (jsonObject.getInt("confidence") >= 15 + (interval*4)) {

                        int top = jsonObject.getInt("top");
                        int right = jsonObject.getInt("right");
                        int left = jsonObject.getInt("left");
                        int bottom = jsonObject.getInt("bottom");

                        right = normalizeX(right);
                        left = normalizeX(left);

                        top = normalizeY(top);
                        bottom = normalizeY(bottom);

                        int width = right - left;
                        int height = bottom - top;

                        Log.d("BlurSize", top + " " + left + " " + right + " " + bottom + " " + width + " " + height);

                        blurBackground(top, left, width, height,frameMat);
                    }
                } catch (JSONException e) {

                }
            }

            if(id.equals(idOperaAutoMode) &&
                    (activity.audioIsPlaying() || (speaking && activity.isAudioIsPaused()))){
                imageView.setImageDrawable(drawablesRecognized.get(drawablesRecognized.size() - 1));
            }else {
                imageView.setImageDrawable(drawablesRecognized.get(index_bitmap));
            }

            RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(dimension, dimension);
            int left = marginLeft(jsonObject, index);
            int right = marginRight(jsonObject, index);
            int top = marginTop(jsonObject, index);
            iconParams.setMargins(left, top, right, 0);
            imageView.setLayoutParams(iconParams);

            Cursor cursor = activity.getDbManager().query(id);
            cursor.moveToPosition(0);
            JSONObject jsonObjectTitle = new JSONObject(cursor.getString(cursor.getColumnIndex(DBHelper.NAME)));

            if(MainActivity.language.equals("IT")){
                texLabel.setText(jsonObjectTitle.getString("it"));
            } else{
                texLabel.setText(jsonObjectTitle.getString("en"));
            }

            RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(sizeX, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            texLabel.setBackground(activity.getDrawable(R.drawable.label_background_green));
            //TEMPORANEO

            if ((jsonObject.getDouble("right") - jsonObject.getDouble("left")) * sizeX > (5*dimension)/8) {

                labelParams.setMargins(left - sizeX / 2 + dimension / 2, top + dimension + (int) activity.getResources().getDimension(R.dimen.top_label), -sizeX, 0);
                texLabel.setLayoutParams(textParams);
                relLabel.setLayoutParams(labelParams);
                relLabel.removeAllViews();
                relLabel.addView(texLabel);
                canvas.addView(relLabel);
            }

            if(idOperaAutoMode.equals(id) && activity.audioIsPlaying()){
                (imageView).setImageDrawable(activity.getDrawable(R.drawable.recognized_pause));
            }
            canvas.addView(imageView);
        } catch (JSONException e){

        }
    }

    /*
        Sezione relativa alla gestione della parte automatica. Fondamentalmente: se l'automode è ON,
        ogni opera che viene riconosciuta con un valore di confidence superiore a maxConfidence e
        con l'attributo bb_big_enough = true, viene fatto partire il task automatico. Se i
        voiceCommands sono attivi, viene chiesto se far partire l'audio, altrimenti viene avviato
        senza il contributo dell'utente. Inoltre, se l'opera di cui si sente l'audio non viene
        più inquadrata, viene avviato il fade dell'audio. Se il fade è attivo mentre si reinquadra
        l'opera di cui l'audio è attivo, il fade viene annullato e l'audio riparte. Se sono attivi
        i voiceCommands, una volta che viene chiesta l'autorizzazione a far partire l'audio e si
        risponde negativamente (o non si risponde), l'applicazione non richiederà niente fino a che
        l'opera è inquadrata. Per poter richiedere l'audio, è necessario non inquadrarla per un
        secondo.
     */

    public void autoTask(JSONObject jsonObject){

        try {

            String id = jsonObject.getString("artwork_id");

            if(speaking){
                return;
            }

            if(MainActivity.autoMode.equals("OFF") || activity.audioIsPlaying()){
                return;
            }

            if (jsonObject.getBoolean("bb_big_enough") &&
                    jsonObject.getInt("confidence") >= maxConfidence){

                if(MainActivity.voiceCommand.equals("ON")){
                    if(id.equals(idOperaAutoMode)){
                        if(activity.isAudioIsPaused()){
                            activity.restartAudio();
                            audioIsRestart = true;
                        }
                        return;
                    }
                    activity.stopAudio();
                    idOperaAutoMode = id;
                    activity.startAudioAsk(id);
                } else {
                    if(id.equals(idOperaAutoMode)){
                        if(activity.isAudioIsPaused()){
                            activity.restartAudio();
                            audioIsRestart = true;
                        }
                        return;
                    }
                    idOperaAutoMode = id;
                    Opera opera = activity.createOpera(id);
                    activity.startAudio(opera);

                }
            }
        } catch (JSONException e){

        }
    }

    private org.opencv.core.Size blurSize = new org.opencv.core.Size();
    private double[] doubles = new double[2];
    private float alpha = 0.0f;
    public Bitmap bitmapBlur;
    private int countBlur = 0;


    public void allocateBitmap(int width, int height){

            bitmapBlur = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);

    }

    public void blurBackground(int top, int left, int width, int height, Mat mat){

        //CAMERA2

        countBlur ++;


        (activity.findViewById(R.id.blurCanvas)).setVisibility(View.VISIBLE);
        Log.d("BlurCount", Integer.toString(countBlur));

        if(countBlur%fps == 0 && blurSize.width <= 35){
            doubles[0] = doubles[0] + 0.1;
            doubles[1] = doubles[1] + 0.1;
            blurSize.set(doubles);
        }
        Mat mfinalRGBA = new Mat();
        Imgproc.cvtColor(mat, mfinalRGBA, Imgproc.COLOR_RGB2RGBA, 4);
        Mat inner;
        inner = mfinalRGBA.submat(top, top+height, left, left+width);
        Scalar scalar = new Scalar(0,0,0,0);
        Core.multiply(inner, scalar, inner);
        Imgproc.blur(mfinalRGBA, mfinalRGBA, blurSize);

        Utils.matToBitmap(mfinalRGBA, bitmapBlur);
        ((ImageView)activity.findViewById(R.id.blurCanvas)).setImageBitmap(bitmapBlur);
        inner.release();
        mfinalRGBA.release();
        if(countBlur%10 == 0 && alpha <= 1){
            alpha += 0.1;
            (activity.findViewById(R.id.blurCanvas)).setAlpha(alpha);
            Log.d("Alpha", Float.toString((activity.findViewById(R.id.blurCanvas)).getAlpha()));
        }
    }


    public void drawBox(JSONObject jsonObject){

        //TEMPORANEO
        try{
            String jclass = jsonObject.getString("class");
            RelativeLayout relativeLayout = new RelativeLayout(activity);

            double right;
            double left;
            double top;
            double bottom;

            right = (jsonObject.getDouble("right") * sizeX);
            left = (jsonObject.getDouble("left") * sizeX);
            top = (jsonObject.getDouble("top") * sizeY);
            bottom = (jsonObject.getDouble("bottom") * sizeY);

            int width = (int) (right - left);
            int height = (int) (bottom - top);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
            layoutParams.setMargins((int) left, (int) top, 0, 0);
            relativeLayout.setLayoutParams(layoutParams);
            if(jclass.equals("person")){
                relativeLayout.setBackground(activity.getDrawable(R.drawable.box_border_blue));
            } else{
                relativeLayout.setBackground(activity.getDrawable(R.drawable.box_border_red));
            }

            canvas.addView(relativeLayout);
        } catch(JSONException e){

        }

    }



    //CANCELLARE
    public int normalizeX(int i){
        return (i*240)/1080;
    }

    public int normalizeY(int i){
        return (i*320)/1920;
    }
    //CANCELLARE

    public void setFrameMat(Mat frameMat) {
        this.frameMat = frameMat;
    }
}

