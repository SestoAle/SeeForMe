package com.example.ale.tesi;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    static{
        System.loadLibrary("opencv_java3");
    }

    //attributi relativi alle variabili statiche dell'applicazione.
    static String language = "EN";
    static String autoMode = "ON";
    static String voiceCommand = "OFF";
    static String blurMode = "ON";
    static double secondPerAuto = 4;
    //TEMPORANEO
    static int fade_seconds = 20;

    private MediaPlayer audioPlayer;
    private boolean audioIsPaused = false;
    private String operaAudio;
    private DBManager dbManager;
    private Details details;
    private HistoryAdapter adapter;
    private ArrayList<Opera> operas = new ArrayList();
    private android.hardware.Camera camera;
    private boolean video_is_running = true;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private RequestQueue requestQueue;
    static final String REMOTE_ADRR = "http://localhost:8080";
    private Task task;
    private VideoView videoView;
    private Handler handler = new Handler();

    /*
        Classe principale dell'applicazione, che corrisponde alla sola activity presente.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {


        //CAMERA2!!!
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            voiceCommand = "OFF";
        }

        super.onCreate(savedInstanceState);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater inflater = getLayoutInflater();

        View customActionBar = inflater.inflate(R.layout.actionbar_layout, null);

        actionBar.setCustomView(customActionBar);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_main);

        dbManager = new DBManager(this);

        adapter = new HistoryAdapter(this, operas);

        //CAMERA
        /*videoView = (VideoView) findViewById(R.id.video);

        //NOMEVIDEO
        String path="android.resource://" + getPackageName() + "/" + ActivityTemporanea.video_tmp;

        Uri uri = Uri.parse(path);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.exit(1);
            }
        });
        videoView.setVideoURI(uri);

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getApplication(), uri);
        try{
            Log.d("FPS", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE));
        } catch (NullPointerException e){

        }*/
        //CAMERA2
        //surfaceView.setOnTouchListener(new View.OnTouchListener() {
        //videoView.setOnTouchListener(new View.OnTouchListener() {
        (findViewById(R.id.texture_video)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                manageStreamVideo();
                return false;
            }
        });

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){

            //CAMERA2!!!
            textureView = (TextureView)findViewById(R.id.texture_video);
            assert textureView != null;
            textureView.setSurfaceTextureListener(textureListener);
            //CAMERA
            //videoView.start();
            task = new Task(this, handler);
            Log.d("Dimensioni2", findViewById(R.id.activity_main).getWidth() + " " + findViewById(R.id.activity_main).getWidth());
            task.setInterval();
            task.setMaxConfidence();
            //CAMERA
            handler.postDelayed(task, 33);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    //Metodo per gestire i permessi.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    //CAMERA2!!!
                    textureView = (TextureView)findViewById(R.id.texture_video);
                    assert textureView != null;
                    textureView.setSurfaceTextureListener(textureListener);
                    openCamera(0,0);

                    //CAMERA
                    //videoView.start();
                    task = new Task(this, handler);
                    task.setInterval();
                    task.setMaxConfidence();
                    handler.postDelayed(task, 33);
                } else {
                    System.exit(0);
                }
                return;
        }
    }

    //Metodo per gestire il menù e i vari item in esso contenuto.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(autoMode.equals("OFF")){
            getMenuInflater().inflate(R.menu.menu_options, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_automode_options, menu);
            if(voiceCommand.equals("ON")){
                menu.findItem(R.id.voicecommandmode).setChecked(true);
            } else {
                menu.findItem(R.id.voicecommandmode).setChecked(false);
            }
        }

        if(language.equals("EN")){
            menu.findItem(R.id.select_language).setIcon(R.drawable.icon_language_eng);
            menu.findItem(R.id.select_language).setTitle("Select Language");
        } else {
            menu.findItem(R.id.select_language).setIcon(R.drawable.icon_language_ita);
            menu.findItem(R.id.select_language).setTitle("Seleziona Lingua");
        }

        if(blurMode.equals("ON")){
            menu.findItem(R.id.blurmode).setChecked(true);
        } else {
            menu.findItem(R.id.blurmode).setChecked(false);
        }

        return true;
    }

    private void alertVoiceCommand(final MenuItem item){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String yesbtn;
        String nobtn;
        if(language.equals("IT")){
            builder.setView(R.layout.alert_voicecommand_layout_it);
            yesbtn = "Si";
            nobtn = "No";
        } else {
            builder.setView(R.layout.alert_voicecommand_layout_en);
            yesbtn = "Yes";
            nobtn = "No";
        }
        builder.setPositiveButton(yesbtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                voiceCommand = "ON";
                item.setChecked(true);
                invalidateOptionsMenu();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(nobtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void alertRecogniziontUnavailable(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            if(language.equals("IT")){
                builder.setTitle("Speech Recognizer non disponibile");
            } else {
                builder.setTitle("Speech Recognizer unavailable");
            }
        }

        if(!isNetworkAvailable()){
            if(language.equals("IT")){
                builder.setTitle("Internet non disponibile");
            } else {
                builder.setTitle("Network unavailable");
            }
        }

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void alertBlurmode(final MenuItem item){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String yesbtn;
        String nobtn;
        if(language.equals("IT")){
            builder.setView(R.layout.alert_blurmode_layout_it);
            yesbtn = "Si";
            nobtn = "No";
        } else {
            builder.setView(R.layout.alert_blurmode_layout_en);
            yesbtn = "Yes";
            nobtn = "No";
        }
        builder.setPositiveButton(yesbtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                blurMode = "ON";
                item.setChecked(true);
                invalidateOptionsMenu();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(nobtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void alertAuotmode(final MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String yesbtn;
        String nobtn;
        if(language.equals("IT")){
            builder.setView(R.layout.alert_automode_layout_it);
            yesbtn = "Si";
            nobtn = "No";
        } else {
            builder.setView(R.layout.alert_automode_layout_en);
            yesbtn = "Yes";
            nobtn = "No";
        }
        builder.setPositiveButton(yesbtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                autoMode = "ON";
                item.setChecked(true);
                invalidateOptionsMenu();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(nobtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void alertSecondPerAuto(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = getLayoutInflater();
        View layout;
        if(language.equals("IT")){
            layout = layoutInflater.inflate(R.layout.alert_second_layout_it, null);
        } else {
            layout = layoutInflater.inflate(R.layout.alert_second_layout_en, null);
        }
        builder.setView(layout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seekbar);
        final TextView textView = (TextView) layout.findViewById(R.id.seekbarText);
        textView.setText(Integer.toString((int)secondPerAuto)+"/10");
        seekBar.setProgress((int)secondPerAuto);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(Integer.toString(progress)+"/10");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                secondPerAuto = seekBar.getProgress();
                task.setInterval();
                task.setMaxConfidence();
                Log.d("Leggere", Double.toString(task.getInterval()));
                Log.d("Leggere", Integer.toString(task.getMaxConfidence()));
            }
        });
    }

    //TEMPORANEO
    public void alertFade(){


        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Fade");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try{
                    fade_seconds = Integer.parseInt(input.getText().toString());
                    Log.d("Fade", Integer.toString(fade_seconds));
                } catch (Exception e){

                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Put actions for CANCEL button here, or leave in blank
            }
        });
        alert.show();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if(video_is_running){
            manageStreamVideo();
        }
        Log.d("Aperto", "Stop?");
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.automode:
                if(autoMode.equals("ON")){
                    autoMode = "OFF";
                    task.setIdOperaAutoMode("");
                    item.setChecked(false);
                    invalidateOptionsMenu();
                } else{
                    alertAuotmode(item);
                }
                return true;

            case R.id.select_language:
                if(language.equals("EN")){
                    language = "IT";
                    item.setIcon(R.drawable.icon_language_ita);
                    item.setTitle("Seleziona Lingua");
                    updateLanguageDetail();
                } else {
                    language = "EN";
                    item.setIcon(R.drawable.icon_language_eng);
                    item.setTitle("Select Language");
                    updateLanguageDetail();
                }
                audioChangeLanguage();
                return true;

            case R.id.voicecommandmode:
                if(!SpeechRecognizer.isRecognitionAvailable(this) || !isNetworkAvailable()){
                    alertRecogniziontUnavailable();
                    return true;
                }
                if(voiceCommand.equals("ON")){
                    voiceCommand = "OFF";
                    item.setChecked(false);
                } else {
                    alertVoiceCommand(item);
                }
                return true;

            case R.id.secondPerAuto:
                alertSecondPerAuto();
                return true;

            case R.id.blurmode:
                if(blurMode.equals("ON")){
                    blurMode = "OFF";
                    item.setChecked(false);
                    invalidateOptionsMenu();
                } else{
                    alertBlurmode(item);
                }
                return true;

            //FADE_SECONDS
            /*case  R.id.fade_seconds:
                alertFade();
                return true;*/
        }

        return true;
    }
    
    //Ogni 33 millisecondi (30 fps) viene "attivato" l'oggetto Task (viene chiamato il metodo
    // run()).
    public void startTask(){
        handler.postDelayed(task, 33);
    }

    public void stopTask(){
        handler.removeCallbacks(task);
    }


    //Metodo per gestire lo streaming video.
    public void manageStreamVideo(){

        if(!video_is_running){

            video_is_running = true;
            //CAMERA
            //videoView.start();
            openCamera(0,0);
            startTask();
            if(autoMode.equals("OFF")) {
                stopAudioWithFade(2000);
            } else {
                if(audioIsPaused && !audioPausedByFade){
                    audioPlayer.start();
                    audioIsPaused = false;
                }
            }
        }else if(video_is_running){
            if(autoMode.equals("ON") && audioPlayer!= null && audioIsPlaying() && audioHandler == null){
                if(getOperaAudio() != null && findViewById(R.id.canvas).findViewWithTag(getOperaAudio())!=null){
                    ((ImageView)findViewById(R.id.canvas).findViewWithTag(getOperaAudio())).setImageDrawable(getDrawable(R.drawable.recognized_ff7));
                }
                audioPlayer.pause();
                audioIsPaused = true;
            }
            
            video_is_running = false;
            //videoView.pause();
            //CAMERA
            cameraDevice.close();
            stopTask();
        }
    }

    public void restRequest(android.view.View v){
        requestQueue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(REMOTE_ADRR, getListener, errorListener);
        requestQueue.add(jsonArrayRequest);
    }


    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Toast.makeText(MainActivity.this, "Errore di Rete", Toast.LENGTH_SHORT).show();
        }
    };

    private Response.Listener<JSONArray> getListener = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
                try{
                    JSONObject j = response.getJSONObject(0);
                } catch (JSONException e){
                    Toast.makeText(MainActivity.this, "Errore di JSON", Toast.LENGTH_SHORT).show();
                }
            }

    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /*
        Metodo per gestire la creazione dell'oggetto Opera. Quando viene chiamato, se nell'array
        di Opera non è presente un oggetto con l'id passato in ingresso, allora viene creato una
        nuova Opera e viene aggiornata l'history tramite un oggetto HistoryAdapter.
     */
    public Opera createOpera(String id){

        int size = operas.size();
        boolean add = true;
        Opera opera = null;

        if(size == 0){
            opera = new Opera(id, dbManager);
            opera.setAudio_id(this);
            operas.add(opera);
            adapter.updateHistory(operas);
            return opera;
        } else{
            for (int i = 0; i < size; i ++) {

                if((operas.get(i)).getId().equals(id)) {
                    return operas.get(i);
                }
            }
        }

        if (add){
            opera = new Opera(id, dbManager);
            opera.setAudio_id(this);
            operas.add(opera);
            adapter.updateHistory(operas);
            return opera;
        }

        return opera;

    }

    @Override
    public void onBackPressed() {

        if(closeDetails()){
            return;
        }

        if(audioPlayer != null && (audioPlayer.isPlaying() || audioIsPaused)){
            audioPlayer.stop();
            audioPlayer = null;
        }


        //TEMPORANEO
        System.exit(1);
        super.onBackPressed();
    }

    /*
        I metodi seguenti servono per gestire il tapping del pulsante di back e per gestire
        l'apertura e la chiusura dell'eventuale scheda di dettagli.
     */

    public void openDetails(Opera opera) {
        for(int i = 0; i < operas.size(); i++)
        {
            operas.get(i).setTuhmbIsPressed(false);
        }
        RelativeLayout container = (RelativeLayout) findViewById(R.id.detailContainer);
        if(findViewById(R.id.details) != null) {
            details.stopDownload();
            details.hide((RelativeLayout) findViewById(R.id.detailContainer));
            details = null;
            details = new Details(this, opera);
            details.show(container);
        } else {
            container.removeAllViews();
            details = new Details(this, opera);
            details.showWithAnimation(container);
        }
    }

    public Boolean closeDetails(){
        if(findViewById(R.id.details) != null && details != null){
            adapter.updateAlpha();
            details.stopDownload();
            details.hideWithAnimation((RelativeLayout)findViewById(R.id.detailContainer));
            for(int i = 0; i < operas.size(); i++)
            {
                operas.get(i).setTuhmbIsPressed(false);
            }
            details = null;
            return true;
        }

        return false;
    }

    public boolean getVideoIsRunning(){
        return video_is_running;
    }

    public VideoView getVideoView() {
        return videoView;
    }

    public void updateLanguageDetail(){
        if (findViewById(R.id.details) != null){
            details.updateLanguageDetail();
        }
    }

    /*
        Sezione per la modalità automatica
     */

    private FadeAudio fadeAudio;
    private Handler audioHandler;
    private FadeAudioUp fadeAudioUp;
    private Handler audioHandlerUp;
    private boolean audioPausedByFade;


    //Per implementare il Fading dell'audio, viene creato un thread che agisce in background e in 2
    // secondi viene gradualmente diminuito l'audio fino a stoppare.
    private class FadeAudio implements Runnable{

        String operaPlaying;
        float volume = 1;
        float speed;
        int count = 0;

        public FadeAudio(String operaPlaying, float speed){
            this.operaPlaying = operaPlaying;
            this.speed = speed;
        }

        public String getOperaPlaying() {
            return operaPlaying;
        }

        public void run() {
            try {

                if(audioHandlerUp != null){
                    audioHandlerUp.removeCallbacks(fadeAudioUp);
                }

                if(audioPlayer.isPlaying()) {
                    audioPlayer.setVolume(volume, volume);
                }

                volume -= speed;

                count += 1;

                if (volume <= speed) {
                    volume = 0;
                    audioPlayer.pause();
                    audioIsPaused = true;
                    //audioPlayer.seekTo(audioPlayer.getDuration());
                    audioHandler.removeCallbacks(this);
                    audioHandler = null;
                    fadeAudio = null;
                    audioPausedByFade = true;

                    if(findViewById(R.id.headphones_icon) != null){
                        if (autoMode.equals("ON")) {
                            task.setIdOperaAutoMode(operaPlaying);
                        }
                        (findViewById(R.id.play_pause_details)).setTag("play");
                        ((ImageView) findViewById(R.id.play_pause_details)).
                                setImageDrawable(getResources().getDrawable(R.drawable.play_icon));
                    }
                    return;
                }
                if(audioHandler != null){
                    audioHandler.postDelayed(this, 50);
                }
            } catch (IllegalStateException e){
                return;
            }
        }

        public float getVolume() {
            return volume;
        }

    }

    private class FadeAudioUp implements Runnable{

        float volume;
        float speed = 0.0025f;

        public FadeAudioUp(float volume){
            this.volume = volume;
        }

        public void run(){
            try {

                volume += speed;
                if(volume < 1){
                    audioPlayer.setVolume(volume, volume);
                } else {
                    Log.d("Ferma", "Ferma");
                    audioHandlerUp.removeCallbacks(this);
                    fadeAudioUp = null;
                    audioHandlerUp = null;
                    return;
                }

                if(audioHandlerUp != null){
                    audioHandlerUp.postDelayed(this, 5);
                } else {
                    fadeAudioUp = null;
                }

            } catch (IllegalStateException e){
                return;
            }
        }
    }

    public void pauseAudio(){

        if(audioHandler != null){
            audioHandler.removeCallbacks(fadeAudio);
            audioHandler = null;
            fadeAudio = null;
        }
        if(autoMode.equals("ON") && audioPlayer!= null && audioIsPlaying() && audioHandler == null){
            audioPlayer.pause();
            audioIsPaused = true;
        }
    }

    public void startAudioWithoutFade(final Opera opera){

        audioIsPaused = false;

        if(audioPlayer == null || !audioPlayer.isPlaying()) {
            operaAudio = opera.getId();
            audioPlayer = MediaPlayer.create(this, opera.getAudio_id());
            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (findViewById(R.id.details) != null) {
                        if (autoMode.equals("ON")) {
                            task.setIdOperaAutoMode(opera.getId());
                            Log.d("IDOPERA", opera.getId());
                        }
                        (findViewById(R.id.play_pause_details)).setTag("play");
                        ((ImageView) findViewById(R.id.play_pause_details)).
                                setImageDrawable(getResources().getDrawable(R.drawable.play_icon));
                    }
                    audioPlayer.reset();
                    audioPlayer.release();
                    audioPlayer = null;
                    operaAudio = null;
                }
            });
            if (findViewById(R.id.details) != null) {
                if (autoMode.equals("ON")) {
                    task.setIdOperaAutoMode(opera.getId());
                    Log.d("IDOPERA", opera.getId());
                }
            }
            audioPlayer.start();
        }
    }

    //Metodo per la gestione dell'avvio dell'audio della relativa opera passata in ingresso.
    public void startAudio(final Opera opera){

        audioPausedByFade = false;

        if(audioIsPaused){
            Log.d("Toggle2", opera.getId() + " " + operaAudio);
            if(operaAudio.equals(opera.getId())){
                restartAudio();
                return;
            } else{
                stopAudio();
            }
        }

        audioIsPaused = false;

        if(audioPlayer == null || !audioPlayer.isPlaying()) {
            operaAudio = opera.getId();
            audioPlayer = MediaPlayer.create(this, opera.getAudio_id());
            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if(findViewById(R.id.details) != null){
                        if(autoMode.equals("ON")){
                            task.setIdOperaAutoMode(opera.getId());
                            Log.d("IDOPERA", opera.getId());
                        }
                        (findViewById(R.id.play_pause_details)).setTag("play");
                        ((ImageView)findViewById(R.id.play_pause_details)).
                                setImageDrawable(getResources().getDrawable(R.drawable.play_icon));
                    }
                    audioPlayer.reset();
                    audioPlayer.release();
                    audioPlayer = null;
                    if(findViewById(R.id.canvas).findViewWithTag(operaAudio) != null){
                        ((ImageView)findViewById(R.id.canvas).findViewWithTag(operaAudio)).setImageDrawable(getDrawable(R.drawable.recognized_ff7));
                    }
                    operaAudio = null;
                }
            });
            if(findViewById(R.id.details) != null) {
                if (autoMode.equals("ON")) {
                    task.setIdOperaAutoMode(opera.getId());
                    Log.d("IDOPERA", opera.getId());
                }
            }
            audioPlayer.setVolume(0.3f, 0.3f);
            fadeAudioUp = new FadeAudioUp(0.3f);
            audioHandlerUp = new Handler();
            audioHandlerUp.postDelayed(fadeAudioUp, 5);
            audioPlayer.start();
        }
    }

    public String getOperaAudio() {
        return operaAudio;
    }

    public void restartAudio(){

        audioPausedByFade = false;

        Log.d("Restart", "Restart");
        if(audioHandlerUp == null && fadeAudioUp == null){
            audioPlayer.setVolume(0.3f, 0.3f);
            fadeAudioUp = new FadeAudioUp(0.3f);
            audioHandlerUp = new Handler();
            audioHandlerUp.postDelayed(fadeAudioUp, 5);
        } else{
            audioPlayer.setVolume(1,1);
        }
        audioPlayer.start();
    }

    public void stopAudioWithFade(int x){
        if(audioPlayer == null){
            return;
        }
        if(audioPlayer.isPlaying() && fadeAudio == null && operaAudio != null){
            Log.d("Yeah", "No");
            fadeAudio = new FadeAudio(operaAudio, (float) 1/(((float)x)/50));
            Log.d("Volume", Float.toString((float) 1/(((float)x)/50)));
            audioHandler = new Handler();
            audioHandler.postDelayed(fadeAudio, 0);
            audioIsPaused = false;
        }
    }

    public void stopAudio(){
        if(audioPlayer == null){
            return;
        }
        if(audioIsPlaying() || audioIsPaused){
            audioIsPaused = false;
            if(audioHandler != null){
                audioHandler.removeCallbacks(fadeAudio);
                audioHandler = null;
                fadeAudio = null;
            }
            if(audioHandlerUp != null){
                audioHandlerUp.removeCallbacks(fadeAudioUp);
                fadeAudioUp = null;
                audioHandlerUp = null;
            }
            audioPlayer.stop();
            audioPlayer.reset();
            audioPlayer.release();
            audioPlayer = null;
            operaAudio = null;
            audioIsPaused = false;
            Log.d("Audio", "L'ho stoppato porca vacca");
        }
    }

    public Boolean audioIsPlaying(){
        if(audioPlayer == null){
            return false;
        }
        if(audioPlayer.isPlaying()){
            return true;
        }
        return false;
    }

    public boolean isAudioIsPaused() {
        return audioIsPaused;
    }

    //Metodo per gestire il cambiamento di lingua quando l'audio è già avviato. Verrà riavviato con
    // la lingua selezionata.
    public void audioChangeLanguage(){

        if(audioIsPaused){
            stopAudio();
            audioIsPaused = false;
            task.setIdOperaAutoMode("");
            return;
        }

        if(audioPlayer != null && audioIsPlaying() && operaAudio != null){
            Log.d("Cambio", "Cambio");
            audioPlayer.stop();
            audioPlayer.reset();
            audioPlayer.release();
            audioPlayer = null;
            if(audioPlayer != null){
                audioChangeLanguage();
            }
            int index = 0;
            for (int i = 0; i < operas.size(); i++){
                if(operas.get(i).getId().equals(operaAudio)){
                    index = i;
                }
            }
            audioPlayer = MediaPlayer.create(this, operas.get(index).getAudio_id());
            audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if(findViewById(R.id.details) != null){
                        (findViewById(R.id.play_pause_details)).setTag("play");
                        ((ImageView)findViewById(R.id.play_pause_details)).
                                setImageDrawable(getResources().getDrawable(R.drawable.play_icon));
                    }
                    audioPlayer.reset();
                    audioPlayer.release();
                    audioPlayer = null;
                    if(findViewById(R.id.canvas).findViewWithTag(operaAudio) != null){
                        ((ImageView)findViewById(R.id.canvas).findViewWithTag(operaAudio)).setImageDrawable(getDrawable(R.drawable.recognized_ff7));
                    }
                    operaAudio = null;
                }
            });
            Log.d("Cambio", "Cambio2 " + operas.get(index).getAudio_id());
            audioPlayer.setVolume(1,1);
            audioPlayer.start();
            if(audioPlayer.isPlaying()){
                Log.d("Eh si", "Eh si");
            }
        }
    }

    //Metodo per gestire il rialzo del volume: se si reinquadra la stessa opera dopo che il fading
    //è già avviato, verrà rialzato il volume.
    public void setVolumeUp(String id, boolean big_enough){

        if(fadeAudio != null && fadeAudio.getOperaPlaying().equals(id) && autoMode.equals("ON")
                && big_enough){

            Log.d("Id", id + " " + videoView.getCurrentPosition());

            audioHandler.removeCallbacks(fadeAudio);
            audioHandler = null;
            fadeAudioUp = new FadeAudioUp(fadeAudio.getVolume());
            fadeAudio = null;
            audioHandlerUp = new Handler();
            audioHandlerUp.postDelayed(fadeAudioUp, 5);
            task.setIdOperaAutoMode(id);
        }
    }

    /*
        Sezione per la gestione dei comandi vocali
     */

    private SpeechRecognizer speechRecognizer;
    private ArrayList<String> matches;
    private boolean turnSpeechOff = false;
    private Timer voiceTimer;
    static String operaAudioAsk;

    /*
        Sottoclasse per la gestione dei RecognitionListener. Sono presenti vari bug che qui sono
        evitati cercando di utilizzare i risultati parziali. All'avvio del riconoscimento, viene
        fatto partire un countdown che dopo 3 secondi ferma la possibilità di fornire comandi,
        andando quindi a valutare i risultati parziali. Se la risposta è "Yes" (in inglese)/ "Si"
        (in italiano), allora verrà fatto partire l'audio, altrimenti si aspetterà una nuova opera
        inquadrata.
     */

    public class MyRecognizerListener implements RecognitionListener {

        String id;

        MyRecognizerListener(String id){
            this.id = id;
        }

        @Override
        public void onEndOfSpeech() {

            if(turnSpeechOff){
                turnSpeechOff = false;
                speechRecognizer.destroy();
                speechRecognizer = null;
            }

        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("Speech", "Inizia ad Ascoltare");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d("Speech", "E' pronto ad Ascoltare");
        }

        @Override
        public void onPartialResults(Bundle results) {
                matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                Log.d("Speech", "Partial" + matches.get(0));


                    Log.d("Speech", "Risultati Parziali = " + Integer.toString(matches.size()));

                    for (int i = 0; i < matches.size(); i++) {
                        Log.d("Speech", matches.get(i));
                    }

                    if (matches.contains("si") || matches.contains("sì") || matches.contains("Sì")
                            || matches.contains("yes") || matches.contains("ES") ||
                            matches.contains("ok") || matches.contains("OK") || matches.contains("okay")) {
                        Opera opera = createOpera(id);
                        startAudio(opera);
                        matches = null;
                        turnSpeechOff = false;
                        speechRecognizer.destroy();
                        speechRecognizer = null;
                    }



        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onError(int error) {


            Log.d("Speech", "Dovrei Cancellarlo");
            if(turnSpeechOff) {
                Log.d("Speech", Integer.toString(error));
                turnSpeechOff = false;
                speechRecognizer.destroy();
                speechRecognizer = null;
            }

        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d("Speech", "Boh4");
        }

        @Override
        public void onResults(Bundle results) {
            matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (int i = 0; i < matches.size(); i++){
                Log.d("Parole",  matches.get(i));
            }

            if (matches.contains("si") || matches.contains("sì") || matches.contains("Sì")
                    || matches.contains("yes") || matches.contains("ES") ||
                    matches.contains("ok") || matches.contains("OK") || matches.contains("okay")){
                Opera opera = createOpera(id);
                startAudio(opera);
                matches = null;
                turnSpeechOff = false;
                speechRecognizer.destroy();
                speechRecognizer = null;
            } else if(matches.contains("no")){
                matches = null;
                turnSpeechOff = false;
                speechRecognizer.destroy();
                speechRecognizer = null;
            } else {
                matches = null;
                turnSpeechOff = false;
                speechRecognizer.destroy();
                speechRecognizer = null;
            }


        }

    }

    private MediaPlayer audioPlayerAsk;

    //Metodo per la gestione dell'avvio dell'audio di richiesta.
    public void startAudioAsk(final String art_id){

        if(speechRecognizer != null) {
            return;
        }

        if(audioPlayer == null || !audioPlayer.isPlaying()) {

            if(audioPlayerAsk != null){
                return;
            }

            if(operaAudioAsk != null && operaAudioAsk.equals(art_id)){
                return;
            }

            operaAudioAsk = art_id;
            if(language.equals("IT")) {
                audioPlayerAsk = MediaPlayer.create(this, R.raw.chiedi);
            } else {
                audioPlayerAsk = MediaPlayer.create(this, R.raw.ask);
            }
            audioPlayerAsk.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    audioPlayerAsk.reset();
                    audioPlayerAsk.release();
                    int audioTitle;
                    if(language.equals("IT")){
                        audioTitle = getResources().getIdentifier(art_id + "_title" + "_it", "raw", getPackageName());
                        audioPlayerAsk = MediaPlayer.create(MainActivity.this, audioTitle);
                    } else {
                        audioTitle = getResources().getIdentifier(art_id + "_title" + "_en", "raw", getPackageName());
                        audioPlayerAsk = MediaPlayer.create(MainActivity.this, audioTitle);
                    }
                    audioPlayerAsk.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            askVoice(art_id);
                            audioPlayerAsk.reset();
                            audioPlayerAsk.release();
                            audioPlayerAsk = null;
                        }
                    });
                    audioPlayerAsk.start();
                }
            });

            audioPlayerAsk.start();
        }
    }

    public void askVoice(String art_id){
        if(speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new MyRecognizerListener(art_id));
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, true);
            if(language.equals("IT")){
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it");
            } else {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US");
            }
            speechRecognizer.startListening(intent);
            voiceTimer = new Timer();
            voiceTimer.schedule(new StopVoiceManager(), 3000);
            Log.d("Speech", "Starta");
        }
    }

    public void stopVoice(){
        if(speechRecognizer != null){
            Log.d("Speech", "Cancello");
            turnSpeechOff = true;
            speechRecognizer.stopListening();
            voiceTimer = null;
        }
    }

    public class StopVoiceManager extends TimerTask{

        @Override
        public void run() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Speech", "Prova a Cancellare");
                    stopVoice();

                }
            });
        }
    }

    /*
        Metodo per controllare se la connessione è disponibile
     */

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    //CANCELLARE
    public class Callback implements Camera.PreviewCallback{
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            task.run();
        }
    }


    /* CAMERA2 !!!!! */


    int count_frame = 0;
    private TextureView textureView;
    private float aspectRatioTexture;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private Handler backgroundHandler;
    private HandlerThread backgroundHandlerThread;
    private ImageReader imageReader;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.d("FPS", "onImageAvailable: " + count_frame++);
            Image img = null;
            img = imageReader.acquireLatestImage();
            if(img != null){
                Mat rgb = convertYuv420888ToMat(img, false);
                Mat rgbFlip = new Mat();
                Mat mfinal = new Mat();
                Core.flip(rgb.t(), rgbFlip, 1);
                org.opencv.core.Size size = new org.opencv.core.Size(rgbFlip.width(), (rgbFlip.width() * aspectRatioTexture));
                Log.d("Resize", size.toString());
                Imgproc.resize(rgbFlip, mfinal, size);
                task.setFrameMat(mfinal);
                task.run();


                rgb.release();
                mfinal.release();
            }

            if (img != null)
                img.close();
        }
    };

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                private void process(CaptureResult result) {
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    process(partialResult);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    process(result);
                }

            };

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera(i, i1);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    public void openCamera(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            aspectRatioTexture = ((float)textureView.getHeight())/((float)textureView.getWidth());
            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert streamConfigurationMap != null;
            for(int i = 0; i < streamConfigurationMap.getOutputSizes(SurfaceTexture.class).length; i++){
                Size size = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[i];
                if(size.getHeight()/size.getWidth() == 16/9){
                    Log.d("AspectRatio", size.toString());
                }
            }
            imageDimension = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
            Size[] yuvSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            Size optimal = chooseSizeYUV(imageDimension, yuvSize);
            task.allocateBitmap((int) (optimal.getHeight() * aspectRatioTexture), optimal.getHeight());
            imageReader = ImageReader.newInstance(optimal.getWidth(), optimal.getHeight(), ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
            try{
                cameraManager.openCamera(cameraId, stateCallback, null);
            } catch (SecurityException e){
                Log.d("CameraError", "CameraError");
            }
        } catch (CameraAccessException e){

        }

    }

    public void createCameraPreview(){
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            Surface imgSurface = imageReader.getSurface();
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(imgSurface, surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSessions) {
                    if(cameraDevice == null){
                        return;
                    }
                    cameraCaptureSession = cameraCaptureSessions;
                    try{
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN);
                        captureRequest = captureRequestBuilder.build();
                        cameraCaptureSession.setRepeatingRequest(captureRequest, mCaptureCallback, backgroundHandler);
                    } catch (CameraAccessException e){

                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        }catch (CameraAccessException e){

        }
    }

    public Size chooseSizeYUV(Size imageDimension, Size[] yuvSize){
        int width = imageDimension.getWidth();
        int height = imageDimension.getHeight();

        Log.d("Ratio1", imageDimension.getWidth() + " " +imageDimension.getHeight());

        double ratio = ((double)width/(double)height);
        double subRatio;

        Log.d("Ratio", Double.toString(ratio));

        Size size = null;

        for(Size subYuv : yuvSize){
            subRatio = (double)subYuv.getWidth()/(double)subYuv.getHeight();
            Log.d("SubRatio", Double.toString(subRatio));
            if(subRatio == ratio){
                size = subYuv;
            }
        }

        Log.d("Optimal", size.getWidth() + " " + size.getHeight());

        return size;
    }

    public Mat convertYuv420888ToMat(Image image, boolean isGreyOnly) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        int ySize = yPlane.getBuffer().remaining();

        if (isGreyOnly) {
            byte[] data = new byte[ySize];
            yPlane.getBuffer().get(data, 0, ySize);

            Mat greyMat = new Mat(height, width, CvType.CV_8UC1);
            greyMat.put(0, 0, data);

            return greyMat;
        }

        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        int uSize = uPlane.getBuffer().remaining();
        int vSize = vPlane.getBuffer().remaining();

        byte[] data = new byte[ySize + (ySize/2)];

        yPlane.getBuffer().get(data, 0, ySize);

        ByteBuffer ub = uPlane.getBuffer();
        ByteBuffer vb = vPlane.getBuffer();

        int uvPixelStride = uPlane.getPixelStride();
        if (uvPixelStride == 1) {
            uPlane.getBuffer().get(data, ySize, uSize);
            vPlane.getBuffer().get(data, ySize + uSize, vSize);

            Mat yuvMat = new Mat(height + (height / 2), width, CvType.CV_8UC1);
            yuvMat.put(0, 0, data);
            Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
            Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_I420, 3);
            yuvMat.release();
            return rgbMat;
        }

        vb.get(data, ySize, vSize);
        for (int i = 0; i < uSize; i += 2) {
            data[ySize + i + 1] = ub.get(i);
        }

        Mat yuvMat = new Mat(height + (height / 2), width, CvType.CV_8UC1);
        yuvMat.put(0, 0, data);
        Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3);
        yuvMat.release();
        return rgbMat;
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
        }
        if (null != imageReader) {
            imageReader.close();
        }
    }

    //CAMERA2!!!


    @Override
    protected void onResume() {
        super.onResume();
        video_is_running = true;
        Log.d("Resume", "Resume");
        if(cameraDevice != null){
            if(textureView.isAvailable()){
                Log.d("Resume", "Resume1");
                closeCamera();
                openCamera(0,0);
            } else{
                Log.d("Resume", "Resume2");
                closeCamera();
                textureView.setSurfaceTextureListener(textureListener);
            }
        }
    }

    @Override
    public void onPause() {
        Log.d("Pausa", "pausa");
        stopAudio();
        closeCamera();
        super.onPause();
    }

    public DBManager getDbManager() {
        return dbManager;
    }
}


