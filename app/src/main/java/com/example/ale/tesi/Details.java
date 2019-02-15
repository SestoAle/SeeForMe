package com.example.ale.tesi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Created by Ale on 13/07/16.
 */

public class Details {

    /*
        Classe che serve per creare graficamente la sezione dei dettagli riguardante la singola
        opera.
     */

    private Bitmap image;
    private MainActivity activity;
    private Opera opera;
    private DownloadImageTask downloadImageTask;

    Details(MainActivity activity, Opera opera){
        this.activity = activity;
        this.opera = opera;
    }

    /*
        Il metodo show(ViewGroup) si occupa di mostrare la scheda dei dettagli dell'opera,
        inizializzando inoltre il download dell'immagine, se quest'ultima non è già presente
        nell'oggetto Opera relativo.
        Il metodo sottostante showWithAnimation(ViewGroup) si occupa della stessa cosa, aggiungend
        però l'animazione dal basso verso l'alto.

        I metodi hide e hideWithAnimation si occupano di nascondere la scheda dall'UI
        dell'applicazione.
     */

    public void show(ViewGroup root){

        activity.stopAudioWithFade(0);
        LayoutInflater inflater = activity.getLayoutInflater();
        inflater.inflate(R.layout.details_layout, root);

        if(opera.getImage_big() == null){
            downloadImageTask = (new DownloadImageTask(opera));
            downloadImageTask.execute(opera.getUrlImage());
        } else {
            updateImageDetails();
        }
        updateLanguageDetail();

    }

    public void showWithAnimation(ViewGroup root){
        show(root);
        Animation slide = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_details_animation_up);
        root.startAnimation(slide);
    }

    public void hide(ViewGroup root){
        root.removeAllViews();
    }

    public void hideWithAnimation(ViewGroup root){
        Animation slide = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_details_animation_down);
        slide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                hide((RelativeLayout)activity.findViewById(R.id.detailContainer));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        root.startAnimation(slide);
    }


    /*
        Per scaricare l'immagine, viene creato un task che inizia a scaricare il png contentuno
        all'indirizzo passato (contenuto nel campo data del database). Una volta che l'immagine
        viene scaricata, viene convertita in bitmap, salvata nell'oggetto Opera relativo e viene
        resa graficamente nella scheda dei dettagli. Se l'oggetto Opera ha già salvato la sua
        immagine, questo task non viene creato.
     */

    private class DownloadImageTask extends AsyncTask<String, Bitmap, Bitmap> {

        Opera opera;
        Boolean isDownloaded = false;

        DownloadImageTask(Opera opera){
            this.opera = opera;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            try {
                URL url = new URL(urldisplay);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                try{
                    InputStream input = connection.getInputStream();
                    image = BitmapFactory.decodeStream(input);
                } catch (InterruptedIOException e ){
                    image = null;
                }
                if (null != image)
                    Log.e("IMAGE", "ISNOTNULL");
                else
                    Log.e("IMAGE", "ISNULL");

            } catch (Exception e) {
                Log.e("Error", "Errore di Connessione");
                image = null;
            }
            isDownloaded = true;
            return image;
        }

        @Override
        protected void onCancelled() {
            opera.setImage_big(null);
            downloadImageTask = null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if(result != null && activity.findViewById(R.id.details) != null &&
                    opera.getImage_big() == null) {
                opera.setImage_big(new BitmapDrawable(result));
                image = null;
                updateImageDetails();
            } else {
                imageNotFound();
            }

            downloadImageTask = null;
        }
    }

    public void updateImageDetails(){
        (activity.findViewById(R.id.details_progress_bar)).setVisibility(View.GONE);
        (activity.findViewById(R.id.details_image_not_found)).setVisibility(View.GONE);
        ((ImageView)activity.findViewById(R.id.details_image)).setImageDrawable(opera.getImage_big());
    }

    public void imageNotFound(){
        (activity.findViewById(R.id.details_progress_bar)).setVisibility(View.GONE);
        ((TextView)activity.findViewById(R.id.details_image_not_found)).setText(activity.getResources().getString(R.string.image_not_found));
    }

    public void stopDownload(){
        if(opera.getImage_big() == null && downloadImageTask != null) {
            downloadImageTask.cancel(true);
        }
    }

    /*
        Il metodo updateLanguageDetail() gestisce il cambio di lingua causato dall'eventuale tap
        dell'utente nell'Item del menu relativo alla lingua.
     */

    public void updateLanguageDetail(){
        String author;
        String location;
        String year;
        String type;

        if(MainActivity.language.equals("EN")){
            author = activity.getResources().getString(R.string.author_en);
            location = activity.getResources().getString(R.string.location_en);
            year = activity.getResources().getString(R.string.year_en);
            type = activity.getResources().getString(R.string.type_en);
        } else {
            author = activity.getResources().getString(R.string.author_it);
            location = activity.getResources().getString(R.string.location_it);
            year = activity.getResources().getString(R.string.year_it);
            type = activity.getResources().getString(R.string.type_it);
        }
        ((ProgressBar)activity.findViewById(R.id.details_progress_bar)).getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        ((TextView)activity.findViewById(R.id.details_author)).setText(author + opera.getAuthor());
        ((TextView)activity.findViewById(R.id.details_title)).setText(opera.getTitle());
        (activity.findViewById(R.id.details_back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.closeDetails();
            }
        });
        ((TextView)activity.findViewById(R.id.details_details)).setText(opera.getDescription());
        ((TextView)activity.findViewById(R.id.details_dimensions)).setText(location + opera.getLocation());
        ((TextView)activity.findViewById(R.id.details_year)).setText(year + opera.getYear());
        ((TextView)activity.findViewById(R.id.details_type)).setText(type + opera.getType());
        (activity.findViewById(R.id.headphones_icon)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ImageView imageView = (ImageView) activity.findViewById(R.id.play_pause_details);

                if(imageView.getTag().equals("play")){
                    imageView.setTag("pause");
                    imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.pause_icon));
                    activity.startAudio(opera);
                } else {
                    imageView.setTag("play");
                    imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.play_icon));
                    activity.stopAudio();
                }
            }
        });
        (activity.findViewById(R.id.play_pause_details)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ImageView imageView = (ImageView) activity.findViewById(R.id.play_pause_details);

                if(imageView.getTag().equals("play")){
                    imageView.setTag("pause");
                    imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.pause_icon));
                    activity.startAudio(opera);
                } else {
                    imageView.setTag("play");
                    imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.play_icon));
                    activity.stopAudio();
                }
            }
        });
        if(activity.audioIsPlaying()){
            ((ImageView)activity.findViewById(R.id.play_pause_details)).setImageDrawable(activity.getResources().getDrawable(R.drawable.pause_icon));
            (activity.findViewById(R.id.play_pause_details)).setTag("pause");
        }
    }
}
