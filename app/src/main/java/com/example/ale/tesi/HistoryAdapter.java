package com.example.ale.tesi;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Ale on 11/07/16.
 */

public class HistoryAdapter {

    /*
        Classe che serve da Adapter per la gestione del footer di history. Quando viene creato un
        oggetto Opera, viene aggiornata la history: viene creato un elemento grafico tappabile per
        poter creare la scheda dei dettagli. Questo oggetto è composto da un immagine che viene
        scaricata tramite un task nello stesso modo con cui viene fatto il download dell'immagine
        nella scheda dei dettagli.
     */

    private ArrayList<Opera> operas;
    private ArrayList<DownloadImageTask> downloadImageTasks;
    private LinearLayout linearLayout;
    private MainActivity activity;


    public HistoryAdapter(MainActivity activity, ArrayList<Opera> operas){
        this.activity = activity;
        linearLayout = (LinearLayout) activity.findViewById(R.id.list);
        this.operas = operas;
        downloadImageTasks = new ArrayList<DownloadImageTask>();
        createButtons();
    }

    public void createButtons(){

        if(operas.size() > 0){
            if(activity.findViewById(R.id.history) != null){
                ((RelativeLayout) activity.findViewById(R.id.list_container)).removeView(activity.findViewById(R.id.history));
            }
        }
            for(int i = 0; i < downloadImageTasks.size(); i++){
                downloadImageTasks.get(i).cancel(true);
            }
        downloadImageTasks.clear();

        for(int i = 0; i < operas.size(); i++) {

            final Opera opera = operas.get(i);

            if(opera.getThumbnail() == null){
                downloadImageTasks.add(new DownloadImageTask(opera));
                downloadImageTasks.get(downloadImageTasks.size() - 1).execute();
            }

            LayoutInflater inflater = activity.getLayoutInflater();
            inflater.inflate(R.layout.history_button, linearLayout);
            final RelativeLayout history_button = (RelativeLayout) linearLayout.findViewWithTag("history_button_tmp");
            history_button.setTag(opera.getId());
            history_button.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    if(opera.getThumbIsPressed()){
                        return;
                    }
                    updateAlpha();
                    if(activity.getVideoIsRunning()) {
                        activity.manageStreamVideo();
                    }
                    activity.openDetails(opera);
                    opera.setTuhmbIsPressed(true);
                    (history_button.findViewWithTag("history_image")).setAlpha((float) 0.5);
                }
            });
            ((TextView) history_button.findViewWithTag("history_text")).setText("OP " + Integer.toString(i + 1));

            if(opera.getThumbnail() != null){
                setThumbanil(opera, activity);
            }
        }
    }


    public void updateHistory(ArrayList<Opera> operas){
        this.operas = operas;
        linearLayout.removeAllViews();
        createButtons();
    }

    public void setThumbanil(final Opera opera, Activity activity){

        LinearLayout linearLayout = (LinearLayout) activity.findViewById(R.id.list);

        RelativeLayout history_button = (RelativeLayout) linearLayout.findViewWithTag(opera.getId());
        ((ImageView) history_button.findViewWithTag("history_image")).setImageBitmap(opera.getThumbnail());

    }

    public void updateAlpha(){

        for(int i = 0; i < operas.size(); i++){
            Opera opera = operas.get(i);
            RelativeLayout relativeLayout = (RelativeLayout) linearLayout.findViewWithTag(opera.getId());
            relativeLayout.findViewWithTag("history_image").setAlpha(1);
        }
    }

    private class DownloadImageTask extends AsyncTask<Void, Bitmap, Bitmap> {

        Opera opera;

        DownloadImageTask(Opera opera){
            this.opera = opera;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            String urldisplay = opera.getUrlThumb();
            Bitmap thumbnail = null;
            try {
                URL url = new URL(urldisplay);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                try {
                    InputStream input = connection.getInputStream();
                    thumbnail = BitmapFactory.decodeStream(input);
                } catch(InterruptedIOException e){
                    thumbnail = null;
                }

                if (null != thumbnail)
                    Log.e("BITMAP", "ISNOTNULL");
                else
                    Log.e("BITMAP", "ISNULL");

            } catch (Exception e) {
                Log.e("Error", "Errore di Connessione");
                thumbnail = null;
            }

            opera.setThumbnail(thumbnail);

            return thumbnail;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if(result != null) {
                setThumbanil(opera, activity);
            }
        }
    }


}
