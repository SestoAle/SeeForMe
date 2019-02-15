package com.example.ale.tesi;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.RawRes;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Ale on 10/07/16.
 */

public class Opera {

    /*
        Classe relativa alla singola Opera. Ha come attributi tutti gli elementi necessari che sono
        salvati nel json dati_opere.
     */

    private String id_name;
    private String urlThumb;
    private Bitmap thumbnail;
    private String urlImage;
    private BitmapDrawable image_big;
    private int audio_id_it;
    private int audio_id_en;
    private boolean tuhmbIsPressed = false;
    private String author_it;
    private String auhtor_en;
    private String title_it;
    private String title_en;
    private String description_it;
    private String description_en;
    private String location_it;
    private String location_en;
    private String year;
    private String type_it;
    private String type_en;

    public Opera(String id_name, DBManager dbManager){

        this.id_name = id_name;

        Cursor cursor = dbManager.query(id_name);
        cursor.moveToPosition(0);

        try{
            JSONObject jsonObject = new JSONObject(cursor.getString(cursor.getColumnIndex(DBHelper.FIELD_DATA)));

            JSONObject jsonObjectData = jsonObject.getJSONObject("title");
            title_it = jsonObjectData.getString("it");
            title_en = jsonObjectData.getString("en");

            urlThumb =  jsonObject.getString("thumbnail");

            jsonObjectData = jsonObject.getJSONObject("author");
            author_it = jsonObjectData.getString("it");
            auhtor_en = jsonObjectData.getString("en");

            jsonObjectData = jsonObject.getJSONObject("location");
            location_it = jsonObjectData.getString("it");
            location_en = jsonObjectData.getString("en");

            jsonObjectData = jsonObject.getJSONObject("description");
            description_it = jsonObjectData.getString("it");
            description_en = jsonObjectData.getString("en");

            year =  jsonObject.getString("year");

            jsonObjectData = jsonObject.getJSONObject("type");
            type_it = jsonObjectData.getString("it");
            type_en = jsonObjectData.getString("en");

            urlImage =  jsonObject.getString("mainImage");

        } catch (JSONException e){
            Log.e("Errore", "Errore");
        }

    }

    public String getId() {
        return id_name;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setTuhmbIsPressed(boolean tuhmbIsPressed) {
        this.tuhmbIsPressed = tuhmbIsPressed;
    }

    public boolean getThumbIsPressed(){
        return this.tuhmbIsPressed;
    }

    public String getAuthor() {

        if(MainActivity.language.equals("EN"))
        {
            return auhtor_en;
        }
        else{
            return author_it;
        }
    }

    public String getDescription() {
        if(MainActivity.language.equals("EN"))
        {
            return description_en;
        }
        else{
            return description_it;
        }
    }

    public String getLocation() {
        if(MainActivity.language.equals("EN"))
        {
            return location_en;
        }
        else{
            return location_it;
        }
    }

    public String getTitle() {
        if(MainActivity.language.equals("EN"))
        {
            return title_en;
        }
        else{
            return title_it;
        }
    }

    public String getType() {
        if(MainActivity.language.equals("EN"))
        {
            return type_en;
        }
        else{
            return type_it;
        }
    }

    public String getUrlThumb() {
        return urlThumb;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public String getYear() {
        return year;
    }

    public void setImage_big(BitmapDrawable image_big) {
        this.image_big = image_big;
    }

    public BitmapDrawable getImage_big() {
        return image_big;
    }

    public int getAudio_id() {
        if(MainActivity.language.equals("EN"))
        {
            return audio_id_en;
        }
        else{
            return audio_id_it;
        }
    }

    public void setAudio_id(Context context) {
        audio_id_en = context.getResources().getIdentifier(id_name + "_en", "raw", context.getPackageName());
        audio_id_it = context.getResources().getIdentifier(id_name + "_it", "raw", context.getPackageName());
    }
}
